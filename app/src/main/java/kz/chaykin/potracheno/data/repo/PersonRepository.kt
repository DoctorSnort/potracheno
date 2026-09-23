package kz.chaykin.potracheno.data.repo

import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kz.chaykin.potracheno.data.db.PotrachenoDatabase
import kz.chaykin.potracheno.data.db.entity.PersonEntity
import kz.chaykin.potracheno.data.photo.PhotoCleaner
import kz.chaykin.potracheno.data.photo.PhotoStore
import kz.chaykin.potracheno.model.Person
import kz.chaykin.potracheno.util.NameOrder
import java.io.File

class PersonRepository(
    private val database: PotrachenoDatabase,
    private val photoStore: PhotoStore,
    private val photoCleaner: PhotoCleaner,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val personDao = database.personDao()
    private val debtDao = database.debtDao()
    private val operationDao = database.operationDao()

    fun observeAll(): Flow<List<Person>> = personDao.observeAll().map { list ->
        list.map { it.toDomain() }.sortedWith(compareBy(NameOrder.comparator) { it.name })
    }

    fun observe(id: Long): Flow<Person?> = personDao.observe(id).map { it?.toDomain() }

    suspend fun save(person: Person): Long {
        val timestamp = now()
        val entity = PersonEntity(
            id = person.id,
            name = person.name,
            photoFileName = person.photoFileName,
            createdAt = if (person.id == 0L) timestamp else person.createdAt,
            updatedAt = timestamp,
        )
        val id = if (person.id == 0L) {
            personDao.insert(entity)
        } else {
            personDao.update(entity)
            person.id
        }
        // Заменённая или убранная фотография больше никому не нужна.
        photoCleaner.removeOrphans()
        return id
    }

    /**
     * Человек уходит вместе со своими долгами во всех поездках. Траты, где за него платили,
     * остаются со своей долей как была — баланс задним числом не меняется, —
     * а полный чек пересчитывается из оставшихся долгов.
     */
    suspend fun delete(id: Long) {
        database.withTransaction {
            val expenseIds = debtDao.expenseIdsByPerson(id)
            personDao.delete(id)
            expenseIds.forEach { expenseId ->
                val operation = operationDao.get(expenseId) ?: return@forEach
                val stillShared = debtDao.sumByExpense(expenseId)
                operationDao.setPaid(
                    expenseId,
                    if (stillShared == 0L) null else operation.amountMinor + stillShared,
                )
            }
        }
        photoCleaner.removeOrphans()
    }

    fun newCameraTarget(): File = photoStore.newCameraTempFile()

    suspend fun importPhoto(uri: Uri): String = photoStore.importFromUri(uri)

    suspend fun importPhoto(file: File): String = photoStore.importFromFile(file)

    /** Снимки, сделанные в редакторе и брошенные без сохранения. */
    suspend fun discardUnsavedPhotos() = photoCleaner.removeOrphans()
}
