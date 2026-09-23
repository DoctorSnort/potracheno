package kz.chaykin.potracheno.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Фотографии лежат в приватной папке приложения: их не видно в галерее и они уезжают
 * вместе с приложением при удалении. В базе хранится только имя файла — полный путь
 * меняется при переустановке, и записи бы протухли.
 */
class PhotoStore(private val context: Context) {

    private val photosDir: File
        get() = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }

    private val cameraDir: File
        get() = File(context.cacheDir, CAMERA_DIR).apply { mkdirs() }

    fun file(fileName: String): File = File(photosDir, fileName)

    fun listFileNames(): List<String> =
        photosDir.listFiles()?.map { it.name }.orEmpty()

    /** Временный файл под снимок системной камеры. Живёт в кэше и после импорта удаляется. */
    fun newCameraTempFile(): File = File(cameraDir, "camera-${UUID.randomUUID()}.jpg")

    suspend fun importFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        store(
            openStream = { requireNotNull(context.contentResolver.openInputStream(uri)) { "Не удалось открыть $uri" } },
        )
    }

    suspend fun importFromFile(source: File): String = withContext(Dispatchers.IO) {
        val name = store(openStream = { source.inputStream() })
        source.delete()
        name
    }

    /** Кладёт готовый файл под конкретным именем — нужно при восстановлении из бэкапа. */
    suspend fun writeRaw(fileName: String, bytes: ByteArray): Unit = withContext(Dispatchers.IO) {
        File(photosDir, fileName).writeBytes(bytes)
    }

    suspend fun delete(fileName: String): Unit = withContext(Dispatchers.IO) {
        File(photosDir, fileName).delete()
    }

    suspend fun deleteAll(): Unit = withContext(Dispatchers.IO) {
        photosDir.listFiles()?.forEach { it.delete() }
    }

    private fun store(openStream: () -> InputStream): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream().use { BitmapFactory.decodeStream(it, null, bounds) }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val decoded = openStream().use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Не удалось прочитать изображение")

        val rotation = openStream().use { readRotation(it) }
        val oriented = if (rotation == 0f) decoded else decoded.rotated(rotation)
        val scaled = oriented.scaledToFit(MAX_DIMENSION)

        val fileName = "${UUID.randomUUID()}.jpg"
        FileOutputStream(File(photosDir, fileName)).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }

        if (scaled !== oriented) scaled.recycle()
        if (oriented !== decoded) oriented.recycle()
        decoded.recycle()
        return fileName
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        while (width / (sample * 2) >= MAX_DIMENSION || height / (sample * 2) >= MAX_DIMENSION) {
            sample *= 2
        }
        return sample
    }

    private fun readRotation(stream: InputStream): Float =
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

    private fun Bitmap.rotated(degrees: Float): Bitmap =
        Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees) }, true)

    private fun Bitmap.scaledToFit(maxSide: Int): Bitmap {
        val longest = maxOf(width, height)
        if (longest <= maxSide) return this
        val ratio = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(this, (width * ratio).toInt(), (height * ratio).toInt(), true)
    }

    companion object {
        private const val PHOTOS_DIR = "photos"
        private const val CAMERA_DIR = "camera"
        private const val MAX_DIMENSION = 1600
        private const val JPEG_QUALITY = 85

        /**
         * Путь к фотографии для экранов. Держим его здесь, чтобы раскладка папок
         * знала о себе в одном месте, а не повторялась в каждом Composable.
         */
        fun fileIn(context: Context, fileName: String): File =
            File(File(context.filesDir, PHOTOS_DIR), fileName)
    }
}
