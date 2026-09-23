package kz.chaykin.potracheno.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import kz.chaykin.potracheno.data.db.dao.ChecklistDao
import kz.chaykin.potracheno.data.db.dao.DebtDao
import kz.chaykin.potracheno.data.db.dao.OperationDao
import kz.chaykin.potracheno.data.db.dao.PersonDao
import kz.chaykin.potracheno.data.db.dao.TripDao
import kz.chaykin.potracheno.data.db.entity.ChecklistItemEntity
import kz.chaykin.potracheno.data.db.entity.DebtEntryEntity
import kz.chaykin.potracheno.data.db.entity.OperationEntity
import kz.chaykin.potracheno.data.db.entity.PersonEntity
import kz.chaykin.potracheno.data.db.entity.TripEntity

@Database(
    entities = [
        TripEntity::class,
        OperationEntity::class,
        PersonEntity::class,
        DebtEntryEntity::class,
        ChecklistItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PotrachenoDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun operationDao(): OperationDao
    abstract fun personDao(): PersonDao
    abstract fun debtDao(): DebtDao
    abstract fun checklistDao(): ChecklistDao

    companion object {
        const val NAME = "potracheno.db"
    }
}

/**
 * Все миграции по порядку. Первая версия — мигрировать не из чего; когда появится вторая,
 * её миграция встаёт сюда и обязательно получает тест на схемах из app/schemas.
 */
val ALL_MIGRATIONS: Array<Migration> = emptyArray()
