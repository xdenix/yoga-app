package com.example.yogaapp.domain.usecase

import com.example.yogaapp.domain.model.DayPoseEntry
import com.example.yogaapp.domain.model.PoseLibrary
import com.example.yogaapp.domain.model.YogaPose
import com.example.yogaapp.domain.util.TestAppDispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SessionTimerUseCase")
class SessionTimerUseCaseTest {

    private lateinit var timer: SessionTimerUseCase

    @BeforeEach
    fun setUp() {
        timer = SessionTimerUseCase(TestAppDispatchers())
    }

    @Test
    fun `start should populate state with the first pose and its duration`() = runTest {
        val poses = sampleSession()
        timer.start(poses, globalDurationSec = null, pauseBetweenSec = 0)
        val state = timer.state.value
        assertNotNull(state.current)
        assertEquals(0, state.currentIndex)
        assertEquals(poses.first().first.defaultDurationSec, state.remaining)
        assertTrue(state.isRunning)
        assertFalse(state.isFinished)
    }

    @Test
    fun `globalDurationSec overrides pose defaults when set`() = runTest {
        val poses = sampleSession()
        timer.start(poses, globalDurationSec = 60, pauseBetweenSec = 0)
        val state = timer.state.value
        assertEquals(60, state.remaining)
        assertEquals(60, state.poses.first().durationSec)
    }

    @Test
    fun `totalDurationSec should sum pose durations`() = runTest {
        val poses = sampleSession()
        timer.start(poses, globalDurationSec = 30, pauseBetweenSec = 0)
        val state = timer.state.value
        // 2 poses × 30s each = 60s total
        assertEquals(60, state.totalDurationSec)
    }

    @Test
    fun `setDurationForCurrentPose should override current pose duration`() = runTest {
        val poses = sampleSession()
        timer.start(poses, globalDurationSec = 30, pauseBetweenSec = 0)
        timer.setDurationForCurrentPose(90)
        val state = timer.state.value
        assertEquals(90, state.remaining)
        assertEquals(90, state.poses.first().durationSec)
    }

    @Test
    fun `clamps pose duration to MIN_DURATION_SEC and MAX_DURATION_SEC`() = runTest {
        val poses = sampleSession()
        timer.start(poses, globalDurationSec = 30, pauseBetweenSec = 0)
        timer.setDurationForCurrentPose(5)
        assertEquals(com.example.yogaapp.domain.model.YogaRules.MIN_DURATION_SEC, timer.state.value.remaining)
        timer.setDurationForCurrentPose(1000)
        assertEquals(com.example.yogaapp.domain.model.YogaRules.MAX_DURATION_SEC, timer.state.value.remaining)
    }

    @Test
    fun `previous should decrement currentIndex without going below 0`() = runTest {
        val poses = sampleSession()
        timer.start(poses, globalDurationSec = 30, pauseBetweenSec = 0)
        timer.previous() // at index 0 — no-op
        assertEquals(0, timer.state.value.currentIndex)
    }

    @Test
    fun `pause and resume should toggle isRunning flag`() = runTest {
        val poses = sampleSession()
        timer.start(poses, globalDurationSec = 30, pauseBetweenSec = 0)
        assertTrue(timer.state.value.isRunning)
        timer.pause()
        assertFalse(timer.state.value.isRunning)
        timer.resume()
        assertTrue(timer.state.value.isRunning)
    }

    @Test
    fun `cancel should reset state to empty`() = runTest {
        val poses = sampleSession()
        timer.start(poses, globalDurationSec = 30, pauseBetweenSec = 0)
        timer.cancel()
        val state = timer.state.value
        assertEquals(0, state.poses.size)
        assertEquals(0, state.currentIndex)
        assertEquals(0, state.remaining)
        assertFalse(state.isRunning)
    }

    private fun sampleSession(): List<Pair<YogaPose, DayPoseEntry>> {
        val tadasana = PoseLibrary.byId("tadasana")!!
        val vrksasana = PoseLibrary.byId("vrksasana")!!
        return listOf(
            tadasana to DayPoseEntry(poseId = tadasana.id, orderIndex = 0, durationOverrideSec = null),
            vrksasana to DayPoseEntry(poseId = vrksasana.id, orderIndex = 1, durationOverrideSec = null),
        )
    }
}
