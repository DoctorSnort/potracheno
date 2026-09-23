package kz.chaykin.potracheno.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kz.chaykin.potracheno.R
import kz.chaykin.potracheno.model.OperationType
import kz.chaykin.potracheno.ui.checklist.ChecklistScreen
import kz.chaykin.potracheno.ui.converter.ConverterScreen
import kz.chaykin.potracheno.ui.debts.DebtsScreen
import kz.chaykin.potracheno.ui.home.HomeScreen
import kz.chaykin.potracheno.ui.operationeditor.OperationEditorScreen
import kz.chaykin.potracheno.ui.operations.OperationsScreen
import kz.chaykin.potracheno.ui.persondebt.PersonDebtScreen
import kz.chaykin.potracheno.ui.personeditor.PersonEditorScreen
import kz.chaykin.potracheno.ui.report.ReportScreen
import kz.chaykin.potracheno.ui.settings.SettingsScreen
import kz.chaykin.potracheno.ui.tripeditor.TripEditorScreen
import kotlin.reflect.KClass

private data class Tab(val route: Any, val routeClass: KClass<*>, @StringRes val label: Int, val icon: ImageVector)

private val Tabs = listOf(
    Tab(HomeRoute, HomeRoute::class, R.string.tab_home, Icons.Outlined.Home),
    Tab(ReportRoute, ReportRoute::class, R.string.tab_report, Icons.Outlined.Insights),
    Tab(DebtsRoute, DebtsRoute::class, R.string.tab_debts, Icons.Outlined.Handshake),
    Tab(ChecklistRoute, ChecklistRoute::class, R.string.tab_checklist, Icons.Outlined.Checklist),
    Tab(ConverterRoute, ConverterRoute::class, R.string.tab_converter, Icons.Outlined.CurrencyExchange),
)

@Composable
fun PotrachenoNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = Tabs.any { tab -> destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Tabs.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { inner ->
        // Отступ снизу — под нижнюю панель (или под системную, когда панели нет).
        // Поглощаем его, чтобы Scaffold экранов не добавил тот же отступ второй раз.
        val bottom = PaddingValues(bottom = inner.calculateBottomPadding())
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier
                .padding(bottom)
                .consumeWindowInsets(bottom),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onCreateTrip = { navController.navigate(TripEditorRoute()) },
                    onEditTrip = { navController.navigate(TripEditorRoute(it)) },
                    onAddOperation = { type -> navController.navigate(OperationEditorRoute(type = type)) },
                    onOpenOperation = { navController.navigate(OperationEditorRoute(operationId = it)) },
                    onOpenAllOperations = { navController.navigate(OperationsRoute) },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                )
            }

            composable<ReportRoute> {
                ReportScreen(
                    onOpenOperation = { navController.navigate(OperationEditorRoute(operationId = it)) },
                )
            }

            composable<DebtsRoute> {
                DebtsScreen(
                    onOpenPerson = { navController.navigate(PersonDebtRoute(it)) },
                    onAddPerson = { navController.navigate(PersonEditorRoute()) },
                )
            }

            composable<ChecklistRoute> { ChecklistScreen() }

            composable<ConverterRoute> { ConverterScreen() }

            composable<TripEditorRoute> {
                TripEditorScreen(onDone = { navController.popBackStack() })
            }

            composable<OperationEditorRoute> {
                OperationEditorScreen(onDone = { navController.popBackStack() })
            }

            composable<OperationsRoute> {
                OperationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenOperation = { navController.navigate(OperationEditorRoute(operationId = it)) },
                    onAdd = { navController.navigate(OperationEditorRoute(type = OperationType.EXPENSE)) },
                )
            }

            composable<PersonEditorRoute> {
                PersonEditorScreen(
                    onDone = { deleted ->
                        // После удаления возвращаться на экран удалённого человека бессмысленно.
                        if (deleted) {
                            navController.popBackStack(DebtsRoute, inclusive = false)
                        } else {
                            navController.popBackStack()
                        }
                    },
                )
            }

            composable<PersonDebtRoute> {
                PersonDebtScreen(
                    onBack = { navController.popBackStack() },
                    onEditPerson = { navController.navigate(PersonEditorRoute(it)) },
                    onOpenOperation = { navController.navigate(OperationEditorRoute(operationId = it)) },
                )
            }

            composable<SettingsRoute> {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
