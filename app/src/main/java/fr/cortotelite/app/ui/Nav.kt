package fr.cortotelite.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
    val tabs = listOf("clients", "docs", "travels", "settings")
    Scaffold(
        bottomBar = {
            if (tabs.any { route == it }) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == "clients",
                        onClick = { nav.tab("clients") },
                        icon = { Icon(Icons.Outlined.People, null) },
                        label = { Text("Clients") }
                    )
                    NavigationBarItem(
                        selected = route == "docs",
                        onClick = { nav.tab("docs") },
                        icon = { Icon(Icons.Outlined.Description, null) },
                        label = { Text("Devis / Factures") }
                    )
                    NavigationBarItem(
                        selected = route == "travels",
                        onClick = { nav.tab("travels") },
                        icon = { Icon(Icons.Outlined.DirectionsCar, null) },
                        label = { Text("Déplacements") }
                    )
                    NavigationBarItem(
                        selected = route == "settings",
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
        }
    }
}

private fun androidx.navigation.NavController.tab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
