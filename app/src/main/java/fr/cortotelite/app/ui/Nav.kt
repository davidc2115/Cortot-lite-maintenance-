package fr.cortotelite.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.cortotelite.app.data.Repo

@Composable
fun CortotRoot(repo: Repo) {
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val route = back?.destination?.route.orEmpty()

    val selectedTab = when {
        route == "clients" || route.startsWith("client") -> "clients"
        route == "docs" || route.startsWith("doc") -> "docs"
        route == "travels" -> "travels"
        route == "employees" || route.startsWith("employee") || route.startsWith("payslips") -> "employees"
        route == "settings" -> "settings"
        else -> ""
    }
    val showBottomBar = selectedTab.isNotEmpty()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == "clients",
                        onClick = { nav.tab("clients") },
                        icon = { Icon(Icons.Outlined.People, null) },
                        label = { Text("Clients") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == "docs",
                        onClick = { nav.tab("docs") },
                        icon = { Icon(Icons.Outlined.Description, null) },
                        label = { Text("Devis / Factures") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == "employees",
                        onClick = { nav.tab("employees") },
                        icon = { Icon(Icons.Outlined.Badge, null) },
                        label = { Text("Employés") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == "travels",
                        onClick = { nav.tab("travels") },
                        icon = { Icon(Icons.Outlined.DirectionsCar, null) },
                        label = { Text("Dépl.") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == "settings",
                        onClick = { nav.tab("settings") },
                        icon = { Icon(Icons.Outlined.Settings, null) },
                        label = { Text("Société") }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "clients", modifier = Modifier.padding(pad)) {
            composable("clients") { ClientsScreen(repo, nav) }
            composable(
                "client/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { ClientEditScreen(repo, it.arguments!!.getLong("id"), nav) }
            composable("docs") { DocumentsScreen(repo, nav) }
            composable(
                "doc/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { DocumentScreen(repo, it.arguments!!.getLong("id"), nav) }
            composable("travels") { TravelsScreen(repo, nav) }
            composable("settings") { SettingsScreen(repo) }
            composable("employees") { EmployeesScreen(repo, nav) }
            composable(
                "employee/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { EmployeeEditScreen(repo, it.arguments!!.getLong("id"), nav) }
            composable(
                "payslips/{employeeId}",
                arguments = listOf(navArgument("employeeId") { type = NavType.LongType })
            ) { PayslipsScreen(repo, it.arguments!!.getLong("employeeId"), nav) }
        }
    }
}

private fun androidx.navigation.NavController.tab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
            inclusive = false
        }
        launchSingleTop = true
        restoreState = true
    }
}
