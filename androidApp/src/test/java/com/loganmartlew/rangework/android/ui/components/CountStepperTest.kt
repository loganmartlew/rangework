package com.loganmartlew.rangework.android.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountStepperTest {

    // --- canDecrement / canIncrement ---

    @Test
    fun canDecrementIsFalseAtMin() {
        val state = CountStepperState(value = 0, min = 0, max = 10)
        assertFalse(state.canDecrement)
    }

    @Test
    fun canDecrementIsTrueAboveMin() {
        val state = CountStepperState(value = 1, min = 0, max = 10)
        assertTrue(state.canDecrement)
    }

    @Test
    fun canIncrementIsFalseAtMax() {
        val state = CountStepperState(value = 10, min = 0, max = 10)
        assertFalse(state.canIncrement)
    }

    @Test
    fun canIncrementIsTrueBelowMax() {
        val state = CountStepperState(value = 9, min = 0, max = 10)
        assertTrue(state.canIncrement)
    }

    // --- increment ---

    @Test
    fun incrementIncreasesValueByOne() {
        val state = CountStepperState(value = 5, min = 0, max = 10)
        assertEquals(6, state.increment().value)
    }

    @Test
    fun incrementClampsAtMax() {
        val state = CountStepperState(value = 10, min = 0, max = 10)
        assertEquals(10, state.increment().value)
    }

    // --- decrement ---

    @Test
    fun decrementDecreasesValueByOne() {
        val state = CountStepperState(value = 5, min = 0, max = 10)
        assertEquals(4, state.decrement().value)
    }

    @Test
    fun decrementClampsAtMin() {
        val state = CountStepperState(value = 0, min = 0, max = 10)
        assertEquals(0, state.decrement().value)
    }

    // --- withValue (invalid-input clamping) ---

    @Test
    fun withValueClampsBelowMinToMin() {
        val state = CountStepperState(value = 5, min = 1, max = 50)
        assertEquals(1, state.withValue(-10).value)
    }

    @Test
    fun withValueClampsAboveMaxToMax() {
        val state = CountStepperState(value = 5, min = 1, max = 50)
        assertEquals(50, state.withValue(999).value)
    }

    @Test
    fun withValueAcceptsValueInsideRange() {
        val state = CountStepperState(value = 5, min = 1, max = 50)
        assertEquals(20, state.withValue(20).value)
    }

    @Test
    fun withValueAcceptsExactMin() {
        val state = CountStepperState(value = 5, min = 1, max = 50)
        assertEquals(1, state.withValue(1).value)
    }

    @Test
    fun withValueAcceptsExactMax() {
        val state = CountStepperState(value = 5, min = 1, max = 50)
        assertEquals(50, state.withValue(50).value)
    }

    // --- boundary: min == max ---

    @Test
    fun whenMinEqualsMaxNeitherDirectionEnabled() {
        val state = CountStepperState(value = 5, min = 5, max = 5)
        assertFalse(state.canDecrement)
        assertFalse(state.canIncrement)
    }
}
