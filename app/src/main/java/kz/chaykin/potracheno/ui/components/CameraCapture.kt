package kz.chaykin.potracheno.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * Снимок системной камерой. Вынесено в одно место, потому что ошибиться здесь можно дважды,
 * и оба раза молча: файл нужно отдать через FileProvider, а проверка «есть ли камера»
 * с Android 11 работает только при объявленном <queries> в манифесте — иначе
 * resolveActivity возвращает null на телефоне, где камера прекрасно себе есть.
 *
 * Возвращает действие «снять фото»: оно создаёт файл, зовёт камеру и отдаёт результат.
 */
@Composable
fun rememberCameraCapture(
    createTarget: () -> File,
    onCaptured: (File) -> Unit,
    onUnavailable: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<File?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success: Boolean ->
        val file = pending
        pending = null
        if (success && file != null) onCaptured(file)
    }

    return {
        val hasCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .resolveActivity(context.packageManager) != null
        if (!hasCamera) {
            onUnavailable()
        } else {
            val file = createTarget()
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            pending = file
            // Страховка на случай, если камера пропала между проверкой и запуском
            // (или её спрятал какой-нибудь корпоративный профиль).
            try {
                launcher.launch(uri)
            } catch (_: ActivityNotFoundException) {
                pending = null
                onUnavailable()
            }
        }
    }
}
