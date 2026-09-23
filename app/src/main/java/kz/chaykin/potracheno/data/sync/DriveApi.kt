package kz.chaykin.potracheno.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Токен протух — снаружи это единственная ошибка, на которую стоит реагировать особо. */
class DriveAuthExpired(message: String) : IOException(message)

data class DriveFileInfo(val id: String, val modifiedTime: String?, val size: Long?)

/**
 * Работа с Google Диском напрямую по HTTPS. Официальный клиент Google API тянет за собой
 * десятки мегабайт и собственный слой HTTP ради трёх запросов — здесь их ровно три:
 * найти копию, залить копию, скачать копию.
 */
class DriveApi {

    private val json = Json { ignoreUnknownKeys = true }

    /** Ищем копию именно среди своих файлов: с областью `drive.file` чужих мы и не видим. */
    suspend fun findBackup(token: String): DriveFileInfo? = withContext(Dispatchers.IO) {
        val query = encode("name = '$BACKUP_NAME' and trashed = false")
        val fields = encode("files(id,name,modifiedTime,size)")
        val order = encode("modifiedTime desc")
        val url = "$API/files?q=$query&fields=$fields&orderBy=$order&spaces=drive&pageSize=10"

        val body = request(url, "GET", token) { it.readBytes().decodeToString() }
        json.decodeFromString<FileListDto>(body).files.firstOrNull()?.let {
            DriveFileInfo(it.id, it.modifiedTime, it.size?.toLongOrNull())
        }
    }

    /** Если копия уже есть — обновляем её, чтобы в Диске не плодились одинаковые файлы. */
    suspend fun upload(token: String, fileId: String?, source: File): DriveFileInfo =
        withContext(Dispatchers.IO) {
            val body = if (fileId == null) createFile(token, source) else updateFile(token, fileId, source)
            val dto = json.decodeFromString<FileDto>(body)
            DriveFileInfo(dto.id, dto.modifiedTime, dto.size?.toLongOrNull())
        }

    suspend fun download(token: String, fileId: String, target: File): Unit = withContext(Dispatchers.IO) {
        request("$API/files/$fileId?alt=media", "GET", token) { input ->
            target.outputStream().use { input.copyTo(it) }
        }
    }

    /** Только чтобы показать в настройках, куда именно уходят копии. */
    suspend fun accountEmail(token: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val body = request("$API/about?fields=${encode("user(emailAddress)")}", "GET", token) {
                it.readBytes().decodeToString()
            }
            json.decodeFromString<AboutDto>(body).user?.emailAddress
        }.getOrNull()
    }

    private fun createFile(token: String, source: File): String {
        val boundary = "potracheno" + System.nanoTime()
        val metadata = """{"name":"$BACKUP_NAME"}"""
        val head = "--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n" +
            metadata + "\r\n--$boundary\r\nContent-Type: $ZIP_TYPE\r\n\r\n"
        val tail = "\r\n--$boundary--\r\n"

        val connection = open("$UPLOAD/files?uploadType=multipart&fields=$FILE_FIELDS", "POST", token)
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        connection.setFixedLengthStreamingMode(
            head.toByteArray().size.toLong() + source.length() + tail.toByteArray().size,
        )
        connection.outputStream.use { out ->
            out.write(head.toByteArray())
            source.inputStream().use { it.copyTo(out) }
            out.write(tail.toByteArray())
        }
        return connection.readOrThrow { it.readBytes().decodeToString() }
    }

    private fun updateFile(token: String, fileId: String, source: File): String {
        val connection = open(
            "$UPLOAD/files/$fileId?uploadType=media&fields=$FILE_FIELDS",
            "PATCH",
            token,
        )
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", ZIP_TYPE)
        connection.setFixedLengthStreamingMode(source.length())
        connection.outputStream.use { out -> source.inputStream().use { it.copyTo(out) } }
        return connection.readOrThrow { it.readBytes().decodeToString() }
    }

    private fun <T> request(url: String, method: String, token: String, read: (InputStream) -> T): T =
        open(url, method, token).readOrThrow(read)

    private fun open(url: String, method: String, token: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }

    private fun <T> HttpURLConnection.readOrThrow(read: (InputStream) -> T): T = try {
        val code = responseCode
        if (code in 200..299) {
            inputStream.use(read)
        } else {
            val detail = errorStream?.use { it.readBytes().decodeToString() }.orEmpty()
            throw toError(code, detail)
        }
    } finally {
        disconnect()
    }

    private fun toError(code: Int, body: String): IOException {
        val message = runCatching {
            json.decodeFromString<ErrorDto>(body).error?.message
        }.getOrNull()?.takeIf { it.isNotBlank() }

        return when (code) {
            HttpURLConnection.HTTP_UNAUTHORIZED ->
                DriveAuthExpired("Google Диск больше не пускает — подключите его заново")
            HttpURLConnection.HTTP_FORBIDDEN ->
                IOException(message ?: "Google Диск отказал в доступе")
            else -> IOException(message ?: "Google Диск ответил ошибкой $code")
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    @Serializable
    private data class FileListDto(val files: List<FileDto> = emptyList())

    @Serializable
    private data class FileDto(
        val id: String,
        val modifiedTime: String? = null,
        val size: String? = null,
    )

    @Serializable
    private data class AboutDto(val user: AboutUserDto? = null)

    @Serializable
    private data class AboutUserDto(val emailAddress: String? = null)

    @Serializable
    private data class ErrorDto(val error: ErrorBodyDto? = null)

    @Serializable
    private data class ErrorBodyDto(val message: String? = null)

    companion object {
        /** Имя одно и то же: копия перезаписывается, а не копится в Диске десятками. */
        const val BACKUP_NAME = "potracheno-backup.zip"

        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private const val FILE_FIELDS = "id,modifiedTime,size"
        private const val ZIP_TYPE = "application/zip"
        private const val TIMEOUT_MS = 30_000
    }
}
