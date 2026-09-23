package kz.chaykin.potracheno.data.demo

import androidx.room.withTransaction
import kz.chaykin.potracheno.data.db.PotrachenoDatabase
import kz.chaykin.potracheno.data.db.entity.ChecklistItemEntity
import kz.chaykin.potracheno.data.db.entity.DebtEntryEntity
import kz.chaykin.potracheno.data.db.entity.OperationEntity
import kz.chaykin.potracheno.data.db.entity.PersonEntity
import kz.chaykin.potracheno.data.db.entity.TripEntity
import kz.chaykin.potracheno.model.Category
import kz.chaykin.potracheno.model.CoverColor
import kz.chaykin.potracheno.model.OperationType
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Демо-поездка — чтобы показать приложение во всей красе, не дожидаясь отпуска.
 * Даты строятся вокруг сегодняшнего дня: поездка всегда «в разгаре», с историей
 * за прошлые дни и тратами за сегодня. Существующие данные не трогает — только добавляет.
 */
class DemoTrip(
    private val database: PotrachenoDatabase,
    private val now: () -> LocalDateTime = LocalDateTime::now,
) {

    private data class Spend(
        val day: Int,
        val time: String,
        val category: Category,
        val amount: Long,
        val comment: String,
        val excluded: Boolean = false,
        /** Полный чек, если часть заплачена за попутчика. */
        val paid: Long? = null,
    )

    /** Возвращает id новой поездки. */
    suspend fun create(): Long = database.withTransaction {
        val nowAt = now().withNano(0)
        val today = nowAt.toLocalDate()
        val start = today.minusDays(DAYS_BEFORE_TODAY)
        val stamp = System.currentTimeMillis()

        val tripId = database.tripDao().insert(
            TripEntity(
                name = "Китай · демо",
                emoji = "🐉",
                cover = CoverColor.GRAPE,
                currencyCode = "CNY",
                currencySymbol = "¥",
                rate = BigDecimal("12.5"),
                startDate = start,
                endDate = start.plusDays(TRIP_DAYS - 1),
                createdAt = stamp,
                updatedAt = stamp,
            ),
        )

        fun at(day: Int, time: String): LocalDateTime =
            LocalDateTime.of(start.plusDays(day.toLong()), LocalTime.parse(time))

        suspend fun operation(
            type: OperationType,
            amount: Long,
            at: LocalDateTime,
            category: Category? = null,
            comment: String? = null,
            excluded: Boolean = false,
            paid: Long? = null,
        ): Long = database.operationDao().insert(
            OperationEntity(
                tripId = tripId,
                type = type,
                amountMinor = amount * 100,
                paidMinor = paid?.let { it * 100 },
                category = category,
                comment = comment,
                occurredAt = at,
                excludeFromDaily = excluded,
                createdAt = stamp,
                updatedAt = stamp,
            ),
        )

        operation(OperationType.TOP_UP, 15_000, at(0, "08:40"), comment = "Обменник в аэропорту")
        operation(OperationType.TOP_UP, 5_000, at(3, "11:15"), comment = "Банкомат у отеля")

        val spends = listOf(
            Spend(0, "10:05", Category.TRANSPORT, 120, "Такси из аэропорта"),
            Spend(0, "13:30", Category.FOOD, 85, "Лапша с говядиной"),
            Spend(0, "16:10", Category.CONNECTION, 99, "Местная симка"),
            Spend(0, "20:45", Category.FOOD, 168, "Хого с острым бульоном"),
            Spend(1, "09:20", Category.CAFE, 42, "Кофе и баоцзы"),
            Spend(1, "12:00", Category.FUN, 60, "Запретный город"),
            Spend(1, "15:40", Category.TRANSPORT, 18, "Метро"),
            Spend(1, "19:30", Category.FOOD, 210, "Димсамы"),
            Spend(2, "07:30", Category.FUN, 880, "Великая стена, экскурсия на весь день", excluded = true),
            Spend(2, "18:20", Category.FOOD, 96, "Уличная еда на Ванфуцзине"),
            Spend(2, "21:10", Category.SOUVENIRS, 145, "Магнитики и открытки"),
            Spend(3, "10:10", Category.CAFE, 38, "Бабл-ти"),
            Spend(3, "14:25", Category.SHOPPING, 430, "Кроссовки"),
            Spend(3, "20:05", Category.FOOD, 300, "Утка по-пекински на двоих", paid = 600),
            Spend(4, "09:00", Category.HEALTH, 56, "Аптека: пластыри и чай от горла"),
            Spend(4, "11:40", Category.TRANSPORT, 553, "Скоростной поезд в Шанхай", excluded = true),
            Spend(4, "17:50", Category.FOOD, 124, "Сяолунбао"),
            Spend(5, "10:30", Category.FUN, 180, "Шанхайская башня"),
            Spend(5, "13:15", Category.CAFE, 64, "Кофейня на Бунде"),
            Spend(5, "19:40", Category.FOOD, 238, "Ужин с видом на реку"),
            Spend(DAYS_BEFORE_TODAY.toInt(), "09:10", Category.CAFE, 45, "Завтрак"),
            Spend(DAYS_BEFORE_TODAY.toInt(), "13:05", Category.FOOD, 118, "Обед в чайном доме"),
            Spend(DAYS_BEFORE_TODAY.toInt(), "15:30", Category.SOUVENIRS, 260, "Чай для мамы"),
        )

        val vasya = person("Вася", stamp)
        val masha = person("Маша", stamp)
        val petya = person("Петя", stamp)

        spends.forEach { spend ->
            // Сегодняшние траты — только те, что уже «случились» к этому часу, иначе утром
            // на экране окажется обед, до которого ещё жить и жить.
            val moment = at(spend.day, spend.time).let { if (it.isAfter(nowAt)) nowAt.minusMinutes(5) else it }
            val id = operation(
                type = OperationType.EXPENSE,
                amount = spend.amount,
                at = moment,
                category = spend.category,
                comment = spend.comment,
                excluded = spend.excluded,
                paid = spend.paid,
            )
            spend.paid?.let { paid ->
                database.debtDao().insert(debt(tripId, vasya, paid - spend.amount, moment, stamp, expenseId = id))
            }
        }

        database.debtDao().insert(debt(tripId, masha, -120, at(1, "18:30"), stamp, comment = "Такси до храма Неба"))
        database.debtDao().insert(debt(tripId, petya, 45, at(4, "09:05"), stamp, comment = "Одолжил на кофе"))
        database.debtDao().insert(debt(tripId, petya, 150, at(5, "13:20"), stamp, comment = "Билет на башню"))

        // Все пункты — только этой поездки: общие («всегда») пережили бы удаление демо
        // и остались бы в настоящем чек-листе.
        listOf(
            "Паспорт и виза" to true,
            "Зарядка и адаптер для розетки" to true,
            "Купить чай для мамы" to true,
            "Попробовать утку по-пекински" to true,
            "Сходить в парк Бэйхай" to false,
            "Покататься на пароме по Хуанпу" to false,
        ).forEach { (text, done) ->
            database.checklistDao().insert(
                ChecklistItemEntity(tripId = tripId, text = text, done = done, createdAt = stamp, updatedAt = stamp),
            )
        }

        tripId
    }

    /** Второе нажатие не должно заводить второго Васю: берём уже знакомого, если он есть. */
    private suspend fun person(name: String, stamp: Long): Long =
        database.personDao().getAll().firstOrNull { it.name == name }?.id
            ?: database.personDao().insert(PersonEntity(name = name, photoFileName = null, createdAt = stamp, updatedAt = stamp))

    private fun debt(
        tripId: Long,
        personId: Long,
        amount: Long,
        at: LocalDateTime,
        stamp: Long,
        comment: String? = null,
        expenseId: Long? = null,
    ) = DebtEntryEntity(
        tripId = tripId,
        personId = personId,
        amountMinor = amount * 100,
        comment = comment,
        occurredAt = at,
        expenseId = expenseId,
        createdAt = stamp,
    )

    private companion object {
        const val TRIP_DAYS = 12L

        /** Сегодня — седьмой день поездки: есть история и есть что ещё тратить. */
        const val DAYS_BEFORE_TODAY = 6L
    }
}
