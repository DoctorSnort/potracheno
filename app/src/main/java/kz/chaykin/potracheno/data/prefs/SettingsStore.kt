package kz.chaykin.potracheno.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kz.chaykin.potracheno.model.LocalRate
import kz.chaykin.potracheno.model.LocalRateDirection

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Показывать суммы в рублях, а не в валюте поездки. Общий для главной и отчёта. */
    val showInRub: Boolean = false,
)

/**
 * Всё, что приложение помнит про Google Диск. Токен доступа здесь не хранится:
 * он живёт около часа и запрашивается заново перед каждой выгрузкой.
 */
data class SyncState(
    val connected: Boolean = false,
    val accountEmail: String? = null,
    /** Автоматическая выгрузка раз в сутки. */
    val autoDaily: Boolean = false,
    val lastSyncAt: Long = 0L,
    val lastError: String? = null,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            themeMode = prefs[KeyThemeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            showInRub = prefs[KeyShowInRub] ?: false,
        )
    }.distinctUntilChanged()

    val syncState: Flow<SyncState> = context.dataStore.data.map { prefs ->
        SyncState(
            connected = prefs[KeyDriveConnected] ?: false,
            accountEmail = prefs[KeyDriveEmail],
            autoDaily = prefs[KeyDriveAuto] ?: false,
            lastSyncAt = prefs[KeyDriveLastSync] ?: 0L,
            lastError = prefs[KeyDriveLastError],
        )
    }.distinctUntilChanged()

    /** Последняя выбранная поездка: с неё приложение и открывается. 0 — ещё не выбирали. */
    val selectedTripId: Flow<Long> = context.dataStore.data.map { it[KeySelectedTrip] ?: 0L }
        .distinctUntilChanged()

    suspend fun setSelectedTrip(id: Long) {
        context.dataStore.edit { it[KeySelectedTrip] = id }
    }

    suspend fun setShowInRub(enabled: Boolean) {
        context.dataStore.edit { it[KeyShowInRub] = enabled }
    }

    /** Курс, который назвал меняла, — свой у каждой поездки: у юаня и бата он разный. */
    fun localRate(tripId: Long): Flow<LocalRate?> = context.dataStore.data.map { prefs ->
        val text = prefs[localRateKey(tripId)] ?: return@map null
        val direction = prefs[localRateDirKey(tripId)]
            ?.let { runCatching { LocalRateDirection.valueOf(it) }.getOrNull() }
            ?: LocalRateDirection.CUR_TO_RUB
        LocalRate(text, direction)
    }.distinctUntilChanged()

    suspend fun setLocalRate(tripId: Long, rate: LocalRate) {
        context.dataStore.edit { prefs ->
            prefs[localRateKey(tripId)] = rate.text
            prefs[localRateDirKey(tripId)] = rate.direction.name
        }
    }

    suspend fun setDriveConnected(email: String?) {
        context.dataStore.edit { prefs ->
            prefs[KeyDriveConnected] = true
            if (email != null) prefs[KeyDriveEmail] = email
            prefs.remove(KeyDriveLastError)
        }
    }

    suspend fun clearDrive() {
        context.dataStore.edit { prefs ->
            prefs.remove(KeyDriveConnected)
            prefs.remove(KeyDriveEmail)
            prefs.remove(KeyDriveAuto)
            prefs.remove(KeyDriveLastSync)
            prefs.remove(KeyDriveLastError)
        }
    }

    suspend fun setAutoDaily(enabled: Boolean) {
        context.dataStore.edit { it[KeyDriveAuto] = enabled }
    }

    suspend fun setSyncSucceeded(at: Long) {
        context.dataStore.edit { prefs ->
            prefs[KeyDriveLastSync] = at
            prefs.remove(KeyDriveLastError)
        }
    }

    suspend fun setSyncFailed(reason: String) {
        context.dataStore.edit { it[KeyDriveLastError] = reason }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KeyThemeMode] = mode.name }
    }

    private companion object {
        val KeyThemeMode = stringPreferencesKey("theme_mode")
        val KeyShowInRub = booleanPreferencesKey("show_in_rub")
        val KeySelectedTrip = longPreferencesKey("selected_trip_id")
        val KeyDriveConnected = booleanPreferencesKey("drive_connected")
        val KeyDriveEmail = stringPreferencesKey("drive_email")
        val KeyDriveAuto = booleanPreferencesKey("drive_auto_daily")
        val KeyDriveLastSync = longPreferencesKey("drive_last_sync")
        val KeyDriveLastError = stringPreferencesKey("drive_last_error")

        fun localRateKey(tripId: Long) = stringPreferencesKey("local_rate_$tripId")
        fun localRateDirKey(tripId: Long) = stringPreferencesKey("local_rate_dir_$tripId")
    }
}
