package kz.chaykin.potracheno.ui.navigation

import kotlinx.serialization.Serializable
import kz.chaykin.potracheno.model.OperationType

/**
 * Маршруты типобезопасной навигации. Идентификаторы всегда Long, где 0 значит «ещё не сохранено»:
 * nullable-параметры в маршрутах требуют своего NavType и ничего не дают взамен.
 */

// Вкладки нижней панели.
@Serializable
data object HomeRoute

@Serializable
data object ReportRoute

@Serializable
data object DebtsRoute

@Serializable
data object ChecklistRoute

@Serializable
data object ConverterRoute

// Экраны поверх вкладок — без нижней панели.
@Serializable
data class TripEditorRoute(val tripId: Long = 0L)

@Serializable
data class OperationEditorRoute(
    val operationId: Long = 0L,
    val type: OperationType = OperationType.EXPENSE,
)

@Serializable
data object OperationsRoute

@Serializable
data class PersonEditorRoute(val personId: Long = 0L)

@Serializable
data class PersonDebtRoute(val personId: Long)

@Serializable
data object SettingsRoute
