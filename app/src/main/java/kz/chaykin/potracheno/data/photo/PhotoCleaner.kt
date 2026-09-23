package kz.chaykin.potracheno.data.photo

import kz.chaykin.potracheno.data.db.dao.PersonDao

/**
 * Удаляет файлы, на которые больше никто не ссылается: фото удалённых людей
 * и снимки, сделанные в редакторе и брошенные без сохранения.
 * Фотографии здесь есть только у людей — больше ссылок искать негде.
 */
class PhotoCleaner(
    private val personDao: PersonDao,
    private val photoStore: PhotoStore,
) {
    suspend fun removeOrphans() {
        val referenced = personDao.allPhotoFileNames().toSet()
        photoStore.listFileNames()
            .filterNot { it in referenced }
            .forEach { photoStore.delete(it) }
    }
}
