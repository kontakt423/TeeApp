package com.kraeutertee.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val titleDE: String,
    val icon: ImageVector
) {
    object Kraeuter : Screen("kraeuter", "Kräuter",   Icons.Default.LocalFlorist)
    object Karte    : Screen("karte",    "Karte",      Icons.Default.Map)
    object Kalender : Screen("kalender", "Kalender",   Icons.Default.CalendarMonth)
    object Rezepte  : Screen("rezepte",  "Rezepte",    Icons.Default.MenuBook)
    object KI       : Screen("ki",       "KI-Assistent", Icons.Default.SmartToy)

    companion object {
        val bottomNavItems = listOf(Kraeuter, Karte, Kalender, Rezepte, KI)
    }
}

// Sub-routes
object Routes {
    const val HERB_DETAIL   = "herb_detail/{herbId}"
    const val SETTINGS      = "settings"
    const val RECIPE_EDIT   = "recipe_edit/{recipeId}"
    const val RECIPE_NEW    = "recipe_new"

    fun herbDetail(herbId: String) = "herb_detail/$herbId"
    fun recipeEdit(recipeId: Int)  = "recipe_edit/$recipeId"
}
