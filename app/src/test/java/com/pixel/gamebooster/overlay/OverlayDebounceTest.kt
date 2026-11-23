package com.pixel.gamebooster.overlay

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class OverlayDebounceTest {

    @Test
    fun sliderRapidUpdatesAreDebouncedToSingleWriteAfter300ms() = runTest {
        val sliderFlow = MutableStateFlow(0.1f)
        val debouncedList = mutableListOf<Float>()

        val job = launch {
            sliderFlow
                .debounce(300L)
                .toList(debouncedList)
        }

        sliderFlow.value = 0.2f
        advanceTimeBy(50L)
        sliderFlow.value = 0.3f
        advanceTimeBy(50L)
        sliderFlow.value = 0.4f
        advanceTimeBy(350L)

        assertEquals(listOf(0.4f), debouncedList)
        job.cancel()
    }

    @Test
    fun separateBurstsYieldMultipleEmissionsWhenIdleExceeds300ms() = runTest {
        val volumeFlow = MutableStateFlow(0.5f)
        val debouncedList = mutableListOf<Float>()

        val job = launch {
            volumeFlow
                .debounce(300L)
                .toList(debouncedList)
        }

        // Burst 1
        volumeFlow.value = 0.6f
        advanceTimeBy(50L)
        volumeFlow.value = 0.7f
        advanceTimeBy(350L) // Settle burst 1

        // Burst 2
        volumeFlow.value = 0.8f
        advanceTimeBy(50L)
        volumeFlow.value = 0.9f
        advanceTimeBy(350L) // Settle burst 2

        assertEquals(listOf(0.7f, 0.9f), debouncedList)
        job.cancel()
    }
}
