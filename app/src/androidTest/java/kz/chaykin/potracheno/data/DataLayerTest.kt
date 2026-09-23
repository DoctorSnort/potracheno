package kz.chaykin.potracheno.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kz.chaykin.potracheno.data.backup.BackupManager
import kz.chaykin.potracheno.data.db.PotrachenoDatabase
import kz.chaykin.potracheno.data.photo.PhotoCleaner
import kz.chaykin.potracheno.data.photo.PhotoStore
import kz.chaykin.potracheno.data.repo.ChecklistRepository
import kz.chaykin.potracheno.data.repo.DebtRepository
import kz.chaykin.potracheno.data.repo.OperationRepository
import kz.chaykin.potracheno.data.repo.PersonRepository
import kz.chaykin.potracheno.data.repo.TripRepository
import kz.chaykin.potracheno.data.prefs.SettingsStore
import kz.chaykin.potracheno.model.Category
import kz.chaykin.potracheno.model.DebtShare
import kz.chaykin.potracheno.model.Operation
import kz.chaykin.potracheno.model.OperationType
import kz.chaykin.potracheno.model.Person
import kz.chaykin.potracheno.model.Trip
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Данные — то место, где ошибка стоит денег: делёжка трат, каскады и круг резервной копии.
 * База в памяти, репозитории настоящие.
 */
@RunWith(AndroidJUnit4::class)
class DataLayerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: PotrachenoDatabase
    private lateinit var photoStore: PhotoStore
    private lateinit var trips: TripRepository
    private lateinit var operations: OperationRepository
    private lateinit var persons: PersonRepository
    private lateinit var debts: DebtRepository
    private lateinit var checklist: ChecklistRepository
    private lateinit var archive: File

    private val at = LocalDateTime.of(2026, 9, 23, 14, 5)

    // JUnit4 требует от @Before и @After возврата void, поэтому runBlocking, а не runTest:
    // с runTest весь класс молча не запускается (грабли брата «Заказано»).
    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(context, PotrachenoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        photoStore = PhotoStore(context)
        photoStore.deleteAll()
        trips = TripRepository(database.tripDao(), SettingsStore(context))
        operations = OperationRepository(database)
        persons = PersonRepository(database, photoStore, PhotoCleaner(database.personDao(), photoStore))
        debts = DebtRepository(database.debtDao())
        checklist = ChecklistRepository(database.checklistDao())
        archive = File(context.cacheDir, "backup-test.zip")
    }

    @After
    fun tearDown() = runBlocking {
        database.close()
        archive.delete()
        photoStore.deleteAll()
    }

    private suspend fun newTrip(name: String = "Китай") = trips.save(
        Trip(
            name = name,
            currencyCode = "CNY",
            currencySymbol = "¥",
            rate = BigDecimal("12.5"),
            startDate = LocalDate.of(2026, 9, 20),
            endDate = LocalDate.of(2026, 9, 30),
        ),
    )

    private fun expense(tripId: Long, id: Long = 0) = Operation(
        id = id,
        tripId = tripId,
        type = OperationType.EXPENSE,
        amountMinor = 0,
        category = Category.CAFE,
        comment = "Утка по-пекински",
        occurredAt = at,
    )

    @Test
    fun трата_за_двоих_в_кошелёк_моя_доля_на_Васю_долг() = runTest {
        val tripId = newTrip()
        val vasya = persons.save(Person(name = "Вася"))

        val id = operations.saveExpense(expense(tripId), paidMinor = 300_000, shares = listOf(DebtShare(vasya, 150_000)))

        val saved = operations.get(id)!!
        assertEquals(150_000L, saved.amountMinor)
        assertEquals(300_000L, saved.paidMinor)
        assertEquals(150_000L, debts.observeBalances(tripId).first().single().balanceMinor)
    }

    @Test
    fun правка_траты_пересобирает_долги_удаление_траты_уносит_их() = runTest {
        val tripId = newTrip()
        val vasya = persons.save(Person(name = "Вася"))
        val petya = persons.save(Person(name = "Петя"))
        val id = operations.saveExpense(expense(tripId), 300_000, listOf(DebtShare(vasya, 150_000)))

        operations.saveExpense(expense(tripId, id), 300_000, listOf(DebtShare(petya, 100_000)))
        val balances = debts.observeBalances(tripId).first().associate { it.person.name to it.balanceMinor }
        assertEquals(0L, balances["Вася"])
        assertEquals(100_000L, balances["Петя"])
        assertEquals(200_000L, operations.get(id)!!.amountMinor)

        operations.delete(id)
        assertTrue(debts.observeBalances(tripId).first().all { it.balanceMinor == 0L })
    }

    @Test
    fun удаление_человека_не_меняет_мою_долю_а_полный_чек_пересчитывается() = runTest {
        val tripId = newTrip()
        val vasya = persons.save(Person(name = "Вася"))
        val petya = persons.save(Person(name = "Петя"))
        val id = operations.saveExpense(
            expense(tripId),
            300_000,
            listOf(DebtShare(vasya, 100_000), DebtShare(petya, 100_000)),
        )

        persons.delete(vasya)
        val afterOne = operations.get(id)!!
        assertEquals(100_000L, afterOne.amountMinor)
        assertEquals(200_000L, afterOne.paidMinor)

        persons.delete(petya)
        assertNull(operations.get(id)!!.paidMinor)
    }

    @Test
    fun погасить_полностью_сводит_долг_в_ноль() = runTest {
        val tripId = newTrip()
        val vasya = persons.save(Person(name = "Вася"))
        debts.addEntry(tripId, vasya, -50_000, "за такси", at)
        debts.addEntry(tripId, vasya, 20_000, null, at)

        debts.settle(tripId, vasya, "в расчёте", at)

        assertEquals(0L, debts.observeBalances(tripId).first().single().balanceMinor)
        assertEquals(3, debts.observeEntries(tripId, vasya).first().size)
    }

    @Test
    fun удаление_поездки_уносит_её_операции_долги_и_пункты_общие_пункты_остаются() = runTest {
        val china = newTrip()
        val turkey = newTrip("Турция")
        val vasya = persons.save(Person(name = "Вася"))
        operations.saveExpense(expense(china), 300_000, listOf(DebtShare(vasya, 150_000)))
        checklist.add("Паспорт", null)
        checklist.add("Адаптер для розетки", china)
        checklist.add("Крем от солнца", turkey)

        trips.delete(china)

        assertTrue(database.operationDao().getAll().isEmpty())
        assertTrue(database.debtDao().getAll().isEmpty())
        assertEquals(listOf("Паспорт", "Крем от солнца"), checklist.observeVisible(turkey).first().map { it.text })
    }

    @Test
    fun убрать_выполненные_пункты_поездки_удаляются_общие_только_снимают_отметку() = runTest {
        val tripId = newTrip()
        val passport = checklist.add("Паспорт", null)
        val adapter = checklist.add("Адаптер", tripId)
        checklist.setDone(passport, true)
        checklist.setDone(adapter, true)

        checklist.clearDone(tripId)

        val left = checklist.observeVisible(tripId).first()
        assertEquals(listOf("Паспорт"), left.map { it.text })
        assertEquals(false, left.single().done)
    }

    @Test
    fun копия_выгрузили_стёрли_восстановили_тем_же_вместе_с_фото() = runTest {
        val tripId = newTrip()
        photoStore.writeRaw("vasya.jpg", byteArrayOf(1, 2, 3))
        val vasya = persons.save(Person(name = "Вася", photoFileName = "vasya.jpg"))
        val expenseId = operations.saveExpense(expense(tripId), 300_000, listOf(DebtShare(vasya, 150_000)))
        checklist.add("Паспорт", null)

        val backup = BackupManager(context, database, photoStore)
        backup.exportTo(archive)
        trips.delete(tripId)
        persons.delete(vasya)
        photoStore.deleteAll()

        val result = backup.importFrom(archive)

        assertEquals(1, result.tripCount)
        val trip = trips.get(tripId)!!
        assertEquals(0, BigDecimal("12.5").compareTo(trip.rate))
        val restored = operations.get(expenseId)!!
        assertEquals(150_000L, restored.amountMinor)
        assertEquals(300_000L, restored.paidMinor)
        assertEquals(at, restored.occurredAt)
        assertEquals(150_000L, debts.observeBalances(tripId).first().single().balanceMinor)
        assertEquals(listOf("Паспорт"), checklist.observeVisible(null).first().map { it.text })
        assertTrue(photoStore.file("vasya.jpg").exists())
    }
}
