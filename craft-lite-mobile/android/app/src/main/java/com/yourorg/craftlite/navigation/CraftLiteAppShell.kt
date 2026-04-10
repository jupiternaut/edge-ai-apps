package com.yourorg.craftlite.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yourorg.craftlite.ui.diff.DiffScreen
import com.yourorg.craftlite.ui.home.HomeScreen
import com.yourorg.craftlite.ui.session.SessionScreen
import com.yourorg.craftlite.ui.settings.SettingsScreen
import com.yourorg.craftlite.ui.workspace.WorkspaceScreen

@Composable
fun CraftLiteAppShell() {
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route
  val navItems = listOf(Route.Home, Route.Session, Route.Workspace, Route.Settings)

  Scaffold(
    bottomBar = {
      NavigationBar {
        navItems.forEach { item ->
          NavigationBarItem(
            selected = currentRoute == item.value,
            onClick = {
              if (currentRoute != item.value) {
                navController.navigate(item.value)
              }
            },
            icon = { Text(item.value.take(1).uppercase()) },
            label = { Text(item.value.replaceFirstChar { it.uppercase() }) },
          )
        }
      }
    },
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = Route.Home.value,
      modifier = Modifier.padding(innerPadding),
    ) {
      composable(Route.Home.value) { HomeScreen() }
      composable(Route.Session.value) { SessionScreen() }
      composable(Route.Workspace.value) { WorkspaceScreen() }
      composable(Route.Diff.value) { DiffScreen() }
      composable(Route.Settings.value) { SettingsScreen() }
    }
  }
}
