package com.pixel.gamebooster.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PullTitleHeaderStateTest {

    private val density = Density(density = 2f, fontScale = 1f)

    @Test
    fun fractionCalculationProduces0AtMinHeightAnd1AtMaxHeight() {
        val state = PullTitleHeaderState(
            minHeight = 64.dp,
            maxHeight = 120.dp,
            density = density,
            view = null,
        )
        // Initially at minHeight
        assertEquals(0f, state.fraction, 0.001f)

        // Expand to maxHeight by posting downward scroll delta
        // 56 dp * 2 px/dp = 112 px
        val consumed = state.nestedScrollConnection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 112f),
            source = NestedScrollSource.UserInput,
        )
        assertEquals(112f, consumed.y, 0.001f)
        assertEquals(120.dp, state.height)
        assertEquals(1f, state.fraction, 0.001f)
    }

    @Test
    fun fractionMathIsClampedStrictlyWithin0And1EvenWhenScrollDeltaExceedsBounds() {
        val state = PullTitleHeaderState(
            minHeight = 64.dp,
            maxHeight = 120.dp,
            density = density,
            view = null,
        )

        // Over-expand with excessive delta (e.g. 1000px)
        state.nestedScrollConnection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 1000f),
            source = NestedScrollSource.UserInput,
        )
        assertTrue(state.fraction <= 1f)
        assertEquals(1f, state.fraction, 0.001f)
        assertEquals(120.dp, state.height)

        // Over-collapse with excessive upward delta (e.g. -1000px)
        state.nestedScrollConnection.onPreScroll(
            available = Offset(0f, -1000f),
            source = NestedScrollSource.UserInput,
        )
        assertTrue(state.fraction >= 0f)
        assertEquals(0f, state.fraction, 0.001f)
        assertEquals(64.dp, state.height)
    }

    @Test
    fun onPreScrollConsumesUpwardDeltaWhenHeightGreaterThanMinHeightAndClampsToMinHeight() {
        val state = PullTitleHeaderState(
            minHeight = 64.dp,
            maxHeight = 120.dp,
            density = density,
            view = null,
        )
        // First expand partially: 20 dp * 2 px/dp = 40 px -> height = 84 dp
        state.nestedScrollConnection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 40f),
            source = NestedScrollSource.UserInput,
        )
        assertEquals(84.dp, state.height)

        // PreScroll with upward delta (available.y < 0)
        // delta of -20px: should consume 20px (10 dp) reducing height to 74 dp
        val consumedOffset = state.nestedScrollConnection.onPreScroll(
            available = Offset(0f, -20f),
            source = NestedScrollSource.UserInput,
        )
        assertEquals(-20f, consumedOffset.y, 0.001f)
        assertEquals(74.dp, state.height)

        // PreScroll with delta of -100px: should clamp to minHeight (64 dp)
        // Remaining delta to minHeight is 10 dp = 20 px
        val consumedClamp = state.nestedScrollConnection.onPreScroll(
            available = Offset(0f, -100f),
            source = NestedScrollSource.UserInput,
        )
        assertEquals(-20f, consumedClamp.y, 0.001f)
        assertEquals(64.dp, state.height)
        assertEquals(0f, state.fraction, 0.001f)

        // When already at minHeight, onPreScroll consumes nothing
        val consumedAtMin = state.nestedScrollConnection.onPreScroll(
            available = Offset(0f, -50f),
            source = NestedScrollSource.UserInput,
        )
        assertEquals(Offset.Zero, consumedAtMin)
    }

    @Test
    fun onPostScrollConsumesDownwardDeltaWhenHeightLessThanMaxHeightAndClampsToMaxHeight() {
        val state = PullTitleHeaderState(
            minHeight = 64.dp,
            maxHeight = 120.dp,
            density = density,
            view = null,
        )
        // PostScroll with downward delta from UserInput
        // delta of 30 px = 15 dp -> height = 79 dp
        val consumed1 = state.nestedScrollConnection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 30f),
            source = NestedScrollSource.UserInput,
        )
        assertEquals(30f, consumed1.y, 0.001f)
        assertEquals(79.dp, state.height)

        // PostScroll with non-user input should not consume
        val nonUserConsumed = state.nestedScrollConnection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 30f),
            source = NestedScrollSource.SideEffect,
        )
        assertEquals(Offset.Zero, nonUserConsumed)
        assertEquals(79.dp, state.height)

        // PostScroll with large delta that exceeds maxHeight
        // Available headroom: 120 dp - 79 dp = 41 dp = 82 px
        val consumedClamp = state.nestedScrollConnection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 200f),
            source = NestedScrollSource.UserInput,
        )
        assertEquals(82f, consumedClamp.y, 0.001f)
        assertEquals(120.dp, state.height)
        assertEquals(1f, state.fraction, 0.001f)

        // When already at maxHeight, onPostScroll consumes nothing
        val consumedAtMax = state.nestedScrollConnection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 50f),
            source = NestedScrollSource.UserInput,
        )
        assertEquals(Offset.Zero, consumedAtMax)
    }
}
