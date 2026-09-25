package com.safemode.safekeepingforffx

import com.safemode.safekeepingforffx.ui.screens.home.CategoryProgress
import com.safemode.safekeepingforffx.ui.screens.home.HomeViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The Home cards are shown in the order the player dragged them into, but that saved order and the
 * set of cards drift apart over time: an update adds a list, a game version hides one, an old order
 * still names a list that's gone. [HomeViewModel.orderCards] has to reconcile the two without ever
 * dropping a real card or crashing on a stale id.
 */
class HomeCardOrderTest {

    private fun card(route: String) = CategoryProgress(route, route, 0, 1)

    private fun routesOf(cards: List<CategoryProgress>) = cards.map { it.route }

    @Test
    fun emptyOrderKeepsNaturalOrder() {
        val cards = listOf(card("a"), card("b"), card("c"))
        assertEquals(listOf("a", "b", "c"), routesOf(HomeViewModel.orderCards(cards, emptyList())))
    }

    @Test
    fun savedOrderIsApplied() {
        val cards = listOf(card("a"), card("b"), card("c"))
        assertEquals(
            listOf("c", "a", "b"),
            routesOf(HomeViewModel.orderCards(cards, listOf("c", "a", "b")))
        )
    }

    @Test
    fun cardsMissingFromTheOrderKeepTheirNaturalPositionAtTheEnd() {
        // "d" was added in an update after the order was saved. It shouldn't jump to the front or
        // vanish - it trails the cards the order does name, where the natural order put it.
        val cards = listOf(card("a"), card("b"), card("c"), card("d"))
        assertEquals(
            listOf("c", "a", "d"),
            // Ordered set covers a, c; b is filtered out upstream (unavailable); d is new.
            routesOf(HomeViewModel.orderCards(listOf(card("c"), card("a"), card("d")), listOf("c", "a", "b")))
        )
    }

    @Test
    fun severalNewCardsStayInTheirNaturalRelativeOrder() {
        val cards = listOf(card("new1"), card("known"), card("new2"))
        assertEquals(
            listOf("known", "new1", "new2"),
            routesOf(HomeViewModel.orderCards(cards, listOf("known")))
        )
    }

    @Test
    fun anOrderNamingCardsThatAreGoneIsHarmless() {
        val cards = listOf(card("a"), card("b"))
        assertEquals(
            listOf("b", "a"),
            routesOf(HomeViewModel.orderCards(cards, listOf("gone", "b", "also_gone", "a")))
        )
    }
}
