package kz.chaykin.potracheno.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kz.chaykin.potracheno.data.db.dao.TripDao
import kz.chaykin.potracheno.data.db.entity.TripEntity
import kz.chaykin.potracheno.data.prefs.SettingsStore
import kz.chaykin.potracheno.model.Trip
import java.time.LocalDate

class TripRepository(
    private val tripDao: TripDao,
    private val settingsStore: SettingsStore,
    private val now: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now,
) {
    fun observeAll(): Flow<List<Trip>> = tripDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observe(id: Long): Flow<Trip?> = tripDao.observe(id).map { it?.toDomain() }

    suspend fun get(id: Long): Trip? = tripDao.get(id)?.toDomain()

    /**
     * Текущая поездка: последняя выбранная. Если её удалили или ещё не выбирали — та,
     * что идёт сегодня, а за ней самая свежая по датам: после восстановления из копии
     * логичнее открыть Китай, в котором сейчас находишься, чем Стамбул через месяц.
     * Без поездок вовсе — null, и главная зовёт создать первую.
     */
    fun observeCurrent(): Flow<Trip?> =
        combine(settingsStore.selectedTripId, observeAll()) { selectedId, trips ->
            val today = today()
            trips.firstOrNull { it.id == selectedId }
                ?: trips.firstOrNull { !today.isBefore(it.startDate) && !today.isAfter(it.endDate) }
                ?: trips.firstOrNull()
        }.distinctUntilChanged()

    suspend fun select(id: Long) = settingsStore.setSelectedTrip(id)

    suspend fun save(trip: Trip): Long {
        val timestamp = now()
        val entity = TripEntity(
            id = trip.id,
            name = trip.name,
            emoji = trip.emoji,
            cover = trip.cover,
            currencyCode = trip.currencyCode,
            currencySymbol = trip.currencySymbol,
            rate = trip.rate,
            startDate = trip.startDate,
            endDate = trip.endDate,
            createdAt = if (trip.id == 0L) timestamp else trip.createdAt,
            updatedAt = timestamp,
        )
        return if (trip.id == 0L) {
            tripDao.insert(entity)
        } else {
            tripDao.update(entity)
            trip.id
        }
    }

    /** Каскад уносит операции, долги и пункты чек-листа этой поездки. */
    suspend fun delete(id: Long) = tripDao.delete(id)
}
