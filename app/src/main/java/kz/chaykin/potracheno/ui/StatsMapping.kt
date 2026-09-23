package kz.chaykin.potracheno.ui

import kz.chaykin.potracheno.domain.stats.ExpenseFact
import kz.chaykin.potracheno.domain.stats.ReportBuilder
import kz.chaykin.potracheno.domain.stats.ReportData
import kz.chaykin.potracheno.domain.stats.ReportExpense
import kz.chaykin.potracheno.domain.stats.TopUpFact
import kz.chaykin.potracheno.domain.stats.TripStats
import kz.chaykin.potracheno.domain.stats.TripStatsCalculator
import kz.chaykin.potracheno.domain.stats.TripStatsInput
import kz.chaykin.potracheno.model.Category
import kz.chaykin.potracheno.model.Operation
import kz.chaykin.potracheno.model.OperationType
import kz.chaykin.potracheno.model.Trip
import java.time.LocalDate

private val calculator = TripStatsCalculator()
private val reportBuilder = ReportBuilder()

private fun List<Operation>.topUps() = filter { it.type == OperationType.TOP_UP }
    .map { TopUpFact(it.amountMinor, it.occurredAt.toLocalDate()) }

/** Операции поездки → вход калькулятора. Долги сюда не попадают: в операциях только своя доля. */
fun statsOf(trip: Trip, operations: List<Operation>, today: LocalDate): TripStats =
    calculator.calculate(
        TripStatsInput(
            start = trip.startDate,
            end = trip.endDate,
            today = today,
            topUps = operations.topUps(),
            expenses = operations.filter { it.isExpense }
                .map { ExpenseFact(it.amountMinor, it.occurredAt.toLocalDate(), it.excludeFromDaily) },
        ),
    )

fun reportOf(trip: Trip, operations: List<Operation>, today: LocalDate): ReportData =
    reportBuilder.build(
        start = trip.startDate,
        end = trip.endDate,
        today = today,
        topUps = operations.topUps(),
        expenses = operations.filter { it.isExpense }.map {
            ReportExpense(it.amountMinor, it.occurredAt.toLocalDate(), it.excludeFromDaily, it.category ?: Category.OTHER)
        },
    )
