package com.yourorg.craftlite.navigation

sealed class Route(val value: String) {
  data object Home : Route("home")
  data object Session : Route("session")
  data object Workspace : Route("workspace")
  data object Diff : Route("diff")
  data object Settings : Route("settings")
}
