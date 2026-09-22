package com.example.bnki.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bnki.ui.card.CardEditScreen
import com.example.bnki.ui.deck.DeckDetailScreen
import com.example.bnki.ui.deck.DeckListScreen
import com.example.bnki.ui.deck.DeckSettingsScreen
import com.example.bnki.ui.stats.StatsScreen
import com.example.bnki.ui.study.StudyScreen

object Routes {
    const val DECKS = "decks"
    const val STATS = "stats"
    fun deck(id: Long) = "deck/$id"
    fun study(id: Long) = "study/$id"
    fun card(deckId: Long, cardId: Long) = "card/$deckId/$cardId"
    fun settings(id: Long) = "deckSettings/$id"
}

@Composable
fun BnkiNavHost() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.DECKS) {
        composable(Routes.DECKS) {
            DeckListScreen(
                onOpenDeck = { nav.navigate(Routes.deck(it)) },
                onOpenStats = { nav.navigate(Routes.STATS) },
                onStudyAll = { nav.navigate(Routes.study(0L)) },
            )
        }

        composable(
            "deck/{deckId}",
            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
        ) { entry ->
            val deckId = entry.arguments!!.getLong("deckId")
            DeckDetailScreen(
                deckId = deckId,
                onBack = { nav.popBackStack() },
                onStudy = { nav.navigate(Routes.study(it)) },
                onAddCard = { nav.navigate(Routes.card(it, 0L)) },
                onEditCard = { d, c -> nav.navigate(Routes.card(d, c)) },
                onSettings = { nav.navigate(Routes.settings(it)) },
            )
        }

        composable(
            "study/{deckId}",
            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
        ) { entry ->
            StudyScreen(
                deckId = entry.arguments!!.getLong("deckId"),
                onBack = { nav.popBackStack() },
            )
        }

        composable(
            "card/{deckId}/{cardId}",
            arguments = listOf(
                navArgument("deckId") { type = NavType.LongType },
                navArgument("cardId") { type = NavType.LongType },
            ),
        ) { entry ->
            CardEditScreen(
                deckId = entry.arguments!!.getLong("deckId"),
                cardId = entry.arguments!!.getLong("cardId"),
                onBack = { nav.popBackStack() },
            )
        }

        composable(
            "deckSettings/{deckId}",
            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
        ) { entry ->
            val deckId = entry.arguments!!.getLong("deckId")
            DeckSettingsScreen(
                deckId = deckId,
                onBack = { nav.popBackStack() },
                onDeleted = { nav.popBackStack(Routes.DECKS, inclusive = false) },
            )
        }

        composable(Routes.STATS) {
            StatsScreen(onBack = { nav.popBackStack() })
        }
    }
}
