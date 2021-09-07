package www.info_pro.ru.camera;

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View


class FocusView : View {

    companion object {
        const val FOCUS_VIEW_HEIGHT = 50
        const val FOCUS_VIEW_WIDTH = 280
    }

    private val paint = Paint()

    private val rectH = FOCUS_VIEW_HEIGHT.toPx
    private val rectW = FOCUS_VIEW_WIDTH.toPx

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        setLayerType(LAYER_TYPE_HARDWARE, null);
        paint.color = Color.TRANSPARENT
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT);
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        //     paint.blendMode = BlendMode.SRC_OUT
        // }
        // }
        canvas.drawColor(Color.parseColor("#80000000"))
        canvas.drawRect(
            width / 2 - (rectW/2F),
            height / 2 - (rectH/2),
            width / 2 + (rectW/2F),
            height / 2 + (rectH/2),
            paint
        )
    }
}
