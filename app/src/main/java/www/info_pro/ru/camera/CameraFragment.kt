package www.info_pro.ru.camera

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.AssetManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.ImageButton
import android.widget.ImageView
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import androidx.window.WindowManager
import org.tensorflow.lite.Interpreter
import www.info_pro.ru.KEY_EVENT_ACTION
import www.info_pro.ru.KEY_EVENT_EXTRA
import www.info_pro.ru.infrastructure.Constants
import www.info_pro.ru.infrastructure.Minio
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Main fragment for this app. Implements all camera operations including:
 * - Viewfinder
 * - Photo taking
 * - Image analysis
 */
class CameraFragment : Fragment() {

    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: PreviewView
    private lateinit var photoFilePath: String
    private lateinit var broadcastManager: LocalBroadcastManager

    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var windowManager: WindowManager

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    /** Volume down button receiver used to trigger shutter */
    private val volumeDownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {
                // When the volume down button is pressed, simulate a shutter button click
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val shutter = container.findViewById<ImageButton>(www.info_pro.ru.R.id.camera_capture_button)
                    shutter.simulateClick()
                }
            }
        }
    }

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation
                imageAnalyzer?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    override fun onResume() {
        super.onResume()

        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), www.info_pro.ru.R.id.fragment_container)
                    .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()

        // Unregister the broadcast receivers and listeners
        broadcastManager.unregisterReceiver(volumeDownReceiver)
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = inflater.inflate(www.info_pro.ru.R.layout.fragment_camera, container, false)

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout
        viewFinder = container.findViewById(www.info_pro.ru.R.id.view_finder)

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        broadcastManager = LocalBroadcastManager.getInstance(view.context)

        // Set up the intent filter that will receive events from our main activity
        val filter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }
        broadcastManager.registerReceiver(volumeDownReceiver, filter)

        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)

        //Initialize WindowManager to retrieve display metrics
        windowManager = WindowManager(view.context)
        photoFilePath = activity?.intent?.getStringExtra("CameraPhotoPath") ?: ""

        // Wait for the views to be properly laid out
        viewFinder.post {

            // Keep track of the display in which this view is attached
            displayId = viewFinder.display.displayId

            // Build UI controls
            updateCameraUi()

            // Set up the camera and its use cases
            setUpCamera()
        }
    }

    /**
     * Inflate camera controls and update the UI manually upon config changes to avoid removing
     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
     * transition on devices that support it.
     *
     * NOTE: The flag is supported starting in Android 8 but there still is a small flash on the
     * screen for devices that run Android 9 or below.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Rebind the camera with the updated display metrics
        bindCameraUseCases()
    }

    /** Initialize CameraX, and prepare to bind the camera use cases  */
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({

            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Select lensFacing depending on the available cameras
            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = windowManager.getCurrentWindowMetrics().bounds
        Log.d(TAG, "Screen metrics: ${metrics.width()} x ${metrics.height()}")

        val screenAspectRatio = aspectRatio(metrics.width(), metrics.height())
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = viewFinder.display.rotation

        // CameraProvider
        val cameraProvider = cameraProvider
                ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Preview
        preview = Preview.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation
                .setTargetRotation(rotation)
                .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()

        // ImageAnalysis
        imageAnalyzer = ImageAnalysis.Builder()
                // We request aspect ratio but no resolution
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(viewFinder.surfaceProvider)

            viewFinder.setOnTouchListener { _, event ->
                return@setOnTouchListener when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                                metrics.width().toFloat(), metrics.height().toFloat()
                        )
                        val autoFocusPoint = factory.createPoint(event.x, event.y)
                        try {
                            camera?.cameraControl?.startFocusAndMetering(
                                    FocusMeteringAction.Builder(
                                            autoFocusPoint,
                                            FocusMeteringAction.FLAG_AF
                                    ).apply {
                                        //focus only when the user tap the preview
                                        disableAutoCancel()
                                    }.build()
                            )
                        } catch (e: CameraInfoUnavailableException) {
                            Log.d("ERROR", "cannot access camera", e)
                        }
                        viewFinder.performClick()
                    }
                    else -> false // Unhandled event.
                }
            }

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    private fun updateCameraUi() {
        // Remove previous UI if any
        container.findViewById<ConstraintLayout>(www.info_pro.ru.R.id.camera_ui_container)?.let {
            container.removeView(it)
        }

        // Inflate a new view containing all UI for controlling the camera
        val controls = View.inflate(requireContext(), www.info_pro.ru.R.layout.camera_ui_container, container)
        val progress = controls.findViewById<View>(www.info_pro.ru.R.id.camera_progress)
        val cameraBtn = controls.findViewById<ImageButton>(www.info_pro.ru.R.id.camera_capture_button)

        // Listener for button used to capture photo
        cameraBtn.setOnClickListener {
            progress.isVisible = true
            cameraBtn.isVisible = false;
            cameraProvider?.unbind(preview);

            // Get a stable reference of the modifiable image capture use case
            imageCapture?.let { imageCapture ->
                // Create output file to hold the image
                val photoFile = File(photoFilePath)

                // Setup image capture metadata
                val metadata = Metadata().apply {
                    // Mirror image when using the front camera
                    isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                }

                // Create output options object which contains file + metadata
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                        .setMetadata(metadata)
                        .build()

                // Setup image capture listener which is triggered after photo has been taken
                imageCapture.takePicture(outputOptions, cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onError(exc: ImageCaptureException) {
                                Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                                activity?.finish()
                            }

                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                                val croppedBitmap = performCrop(savedUri)

                                Log.d(TAG, "Photo capture succeeded: $savedUri")

                                // If the folder selected is an external media directory, this is
                                // unnecessary but otherwise other apps will not be able to access our
                                // images unless we scan them using [MediaScannerConnection]
                                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(savedUri.toFile().extension)
                                MediaScannerConnection.scanFile(
                                        context,
                                        arrayOf(savedUri.toFile().absolutePath),
                                        arrayOf(mimeType)
                                ) { _, uri -> Log.d(TAG, "Image capture scanned into media store: $uri") }

                                croppedBitmap.let {
                                    val recognizeValue = recognize(it)
                                    Log.d(TAG, "Recognize: $recognizeValue")
                                    val data = Intent()
                                    data.putExtra(Constants.VALUE_WITH_PHOTO, recognizeValue)
                                    activity?.setResult(-1, data)
                                    activity?.finish()
                                }

                            }
                        })

            }

            // We can only change the foreground Drawable using API level 23+ API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                // Display flash animation to indicate that photo was captured
                container.postDelayed({
                    container.foreground = ColorDrawable(Color.WHITE)
                    container.postDelayed({ container.foreground = null }, ANIMATION_FAST_MILLIS)
                }, ANIMATION_SLOW_MILLIS)
            }
        }
    }


    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    private fun performCrop(uri: Uri): Bitmap {
        val ei = ExifInterface(uri.path!!)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

        val bitmap = BitmapFactory.decodeFile(uri.path)
        val bounds = windowManager.getCurrentWindowMetrics().bounds

        val bitmapMetric = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270 -> bitmap.width
            else -> bitmap.height
        }

        val screenAspectRatioValue = bitmapMetric / bounds.height().toFloat()
        val h: Float = FocusView.FOCUS_VIEW_HEIGHT * screenAspectRatioValue
        val w: Float = FocusView.FOCUS_VIEW_WIDTH * screenAspectRatioValue

        var frameH = h.toPx
        var frameW = w.toPx

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.preRotate(90F)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.preRotate(180F)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.preRotate(270F)
            else -> matrix.preRotate(0F)
        }

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                val old = frameW
                frameW = frameH
                frameH = old
            }
            else -> {
            }
        }

        val x = bitmap.width - frameW
        val y = bitmap.height - frameH

        //MODEL_WIDTH, MODEL_HEIGHT - размеры которые ожидает модель
        val bitmapFinal = Bitmap.createBitmap(
                bitmap,
                (x / 2).toInt(),
                (y / 2).toInt(),
                frameW.toInt(),
                frameH.toInt(),
                matrix,
                true
        ).scale(MODEL_WIDTH, MODEL_HEIGHT, true)

        val file = File(uri.path!!)
        val fileOutputStream = FileOutputStream(file, false)

        bitmapFinal.compress(
            Bitmap.CompressFormat.JPEG,
            100,
            fileOutputStream
        )

        fileOutputStream.close()

        Minio.uploadFile(file.name, file.path)

        return bitmapFinal
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd("EC_models/model_892.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun recognize(imageData: Bitmap): String {
        val model = loadModelFile(context?.assets!!)

        val options = Interpreter.Options()
        options.setUseNNAPI(true)
        val interpreter = Interpreter(model, options)

        val inputShape = interpreter.getInputTensor(0).shape()
        val bitmapBuffer = convertBitmapToByteBuffer(imageData, inputShape[1], inputShape[0])

        val outputClassesCount = 48
        val longTypeSize = 8
        val modelOutputSize = outputClassesCount * longTypeSize
        val result = ByteBuffer.allocate(modelOutputSize)
        interpreter.run(bitmapBuffer, result)
        return getOutputString(result)
    }

    private fun getOutputString(output: ByteBuffer): String {
        output.rewind()
        output.order(ByteOrder.LITTLE_ENDIAN)
        var result = ""

        do {
            val symbol = output.long
            if (symbol < 0) {
                break
            }
            result += symbol.toString()
        } while (output.position() < output.limit())

        return result
    }

    private fun convertBitmapToByteBuffer(
            bitmap: Bitmap,
            inputImageWidth: Int,
            inputImageHeight: Int
    ): ByteBuffer {
        val FLOAT_TYPE_SIZE = 4
        val PIXEL_SIZE = 1
        val modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth * inputImageHeight * PIXEL_SIZE
        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in pixels) {
            val r = (pixelValue shr 16 and 0xFF)
            val g = (pixelValue shr 8 and 0xFF)
            val b = (pixelValue and 0xFF)

            val normalizedPixelValue = (r + g + b) / 3.0f
            byteBuffer.putFloat(normalizedPixelValue)
        }

        return byteBuffer
    }

    companion object {
        private const val TAG = "INFNET_Camera"
        private const val RATIO_4_3_VALUE = 4.0F / 3.0F
        private const val RATIO_16_9_VALUE = 16.0F / 9.0F

        private const val MODEL_HEIGHT = 161
        private const val MODEL_WIDTH = 892
    }
}
