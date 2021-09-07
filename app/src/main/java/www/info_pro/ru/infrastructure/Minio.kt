package www.info_pro.ru.infrastructure

import android.content.ContentResolver
import android.util.Log
import io.minio.MinioClient
import io.minio.UploadObjectArgs


object Minio {

    var minioClient: MinioClient = MinioClient.builder()
        .endpoint("http://45.93.4.146:9000")
        .credentials("admin", "weLPAzcb2A8Xd54U")
        .build()

    fun uploadFile(fileName: String, path: String) {
        minioClient.uploadObject(
            UploadObjectArgs.builder()
                .bucket("counters")
                .`object`(fileName)
                .contentType("image/jpeg")
                .filename(path)
                .build()
        )
    }
}