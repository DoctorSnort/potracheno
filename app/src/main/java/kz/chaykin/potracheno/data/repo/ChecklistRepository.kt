package kz.chaykin.potracheno.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kz.chaykin.potracheno.data.db.dao.ChecklistDao
import kz.chaykin.potracheno.data.db.entity.ChecklistItemEntity
import kz.chaykin.potracheno.model.ChecklistItem

class ChecklistRepository(
    private val checklistDao: ChecklistDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** Пункты на все поездки плюс пункты [tripId]. Без поездки — только общие. */
    fun observeVisible(tripId: Long?): Flow<List<ChecklistItem>> =
        checklistDao.observeVisible(tripId).map { list -> list.map { it.toDomain() } }

    suspend fun add(text: String, tripId: Long?): Long {
        val timestamp = now()
        return checklistDao.insert(
            ChecklistItemEntity(tripId = tripId, text = text, done = false, createdAt = timestamp, updatedAt = timestamp),
        )
    }

    suspend fun setDone(id: Long, done: Boolean) = modify(id) { it.copy(done = done) }

    /** Перенос между «на все поездки» (null) и конкретной поездкой. */
    suspend fun setScope(id: Long, tripId: Long?) = modify(id) { it.copy(tripId = tripId) }

    suspend fun rename(id: Long, text: String) = modify(id) { it.copy(text = text) }

    /** Вернуть удалённое свайпом — тем же id и на то же место. */
    suspend fun restore(item: ChecklistItem) {
        checklistDao.insert(
            ChecklistItemEntity(
                id = item.id,
                tripId = item.tripId,
                text = item.text,
                done = item.done,
                createdAt = item.createdAt,
                updatedAt = now(),
            ),
        )
    }

    suspend fun delete(id: Long) = checklistDao.delete(id)

    suspend fun clearDone(tripId: Long?) = checklistDao.clearDone(tripId)

    private suspend fun modify(id: Long, change: (ChecklistItemEntity) -> ChecklistItemEntity) {
        val item = checklistDao.get(id) ?: return
        checklistDao.update(change(item).copy(updatedAt = now()))
    }
}
