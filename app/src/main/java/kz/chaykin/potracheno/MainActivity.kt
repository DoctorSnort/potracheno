package kz.chaykin.potracheno

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kz.chaykin.potracheno.ui.PotrachenoRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as PotrachenoApp).container
        // Пришли из уведомления об ошибке выгрузки — сразу на настройки, а не на главную.
        // Только при первом создании и не из «Недавних»: интент с меткой остаётся в задаче,
        // и без этих проверок каждый повторный запуск снова уводил бы в настройки.
        val fromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        val openSettings = savedInstanceState == null && !fromHistory &&
            intent.getBooleanExtra(EXTRA_OPEN_SETTINGS, false)
        setContent {
            PotrachenoRoot(settingsStore = container.settingsStore, openSettings = openSettings)
        }
    }

    companion object {
        const val EXTRA_OPEN_SETTINGS = "open_settings"

        /** Новый чистый стек: иначе тап по уведомлению вернёт в тот экран, где приложение бросили. */
        fun openSettingsIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_SETTINGS, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
