package kz.chaykin.potracheno.di

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kz.chaykin.potracheno.data.backup.BackupManager
import kz.chaykin.potracheno.data.db.ALL_MIGRATIONS
import kz.chaykin.potracheno.data.db.PotrachenoDatabase
import kz.chaykin.potracheno.data.demo.DemoTrip
import kz.chaykin.potracheno.data.photo.PhotoCleaner
import kz.chaykin.potracheno.data.photo.PhotoStore
import kz.chaykin.potracheno.data.prefs.SettingsStore
import kz.chaykin.potracheno.data.repo.ChecklistRepository
import kz.chaykin.potracheno.data.repo.DebtRepository
import kz.chaykin.potracheno.data.repo.OperationRepository
import kz.chaykin.potracheno.data.repo.PersonRepository
import kz.chaykin.potracheno.data.repo.TripRepository
import kz.chaykin.potracheno.data.sync.DriveApi
import kz.chaykin.potracheno.data.sync.DriveAuth
import kz.chaykin.potracheno.data.sync.DriveSync
import kz.chaykin.potracheno.util.AppClock

/**
 * Зависимости собираются руками, как у брата: для приложения такого размера Hilt даёт
 * больше сборочной возни, чем пользы — здесь один экземпляр каждого объекта и всё видно глазами.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Для работы, которая должна пережить любой экран: уборка мусора, восстановление из копии. */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val clock: AppClock = AppClock()

    val database: PotrachenoDatabase by lazy {
        Room.databaseBuilder(appContext, PotrachenoDatabase::class.java, PotrachenoDatabase.NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
    }

    val photoStore: PhotoStore by lazy { PhotoStore(appContext) }

    private val photoCleaner: PhotoCleaner by lazy { PhotoCleaner(database.personDao(), photoStore) }

    val settingsStore: SettingsStore by lazy { SettingsStore(appContext) }

    val tripRepository: TripRepository by lazy { TripRepository(database.tripDao(), settingsStore) }

    val operationRepository: OperationRepository by lazy { OperationRepository(database) }

    val personRepository: PersonRepository by lazy { PersonRepository(database, photoStore, photoCleaner) }

    val debtRepository: DebtRepository by lazy { DebtRepository(database.debtDao()) }

    val checklistRepository: ChecklistRepository by lazy { ChecklistRepository(database.checklistDao()) }

    val demoTrip: DemoTrip by lazy { DemoTrip(database) }

    val backupManager: BackupManager by lazy { BackupManager(appContext, database, photoStore) }

    val driveAuth: DriveAuth by lazy { DriveAuth(appContext) }

    val driveSync: DriveSync by lazy { DriveSync(appContext, backupManager, DriveApi()) }
}
