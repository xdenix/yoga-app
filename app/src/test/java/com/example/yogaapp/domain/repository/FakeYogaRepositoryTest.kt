package com.example.yogaapp.domain.repository

import com.example.yogaapp.domain.model.YogaLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("FakeYogaRepository (used by VM tests)")
class FakeYogaRepositoryTest {

    private lateinit var repository: FakeYogaRepository

    @BeforeEach
    fun setUp() {
        repository = FakeYogaRepository()
    }

    @Test
    fun `should expose all 42 poses on init`() = runTest {
        val poses = repository.observePoses().first()
        assertEquals(42, poses.size)
    }

    @Test
    fun `getPose should return matching pose by id`() = runTest {
        val pose = repository.getPose("tadasana")
        assertNotNull(pose)
        assertEquals("Tadasana", pose!!.sanskritName)
    }

    @Test
    fun `getPose should return null for unknown id`() = runTest {
        val pose = repository.getPose("unknown-pose")
        assertNull(pose)
    }

    @Test
    fun `createProgram should populate the program flow`() = runTest {
        val program = repository.createProgram(YogaLevel.Intermediate)
        assertEquals(30, program.days.size)
        val observed = repository.observeProgram().first()
        assertNotNull(observed)
        assertEquals(YogaLevel.Intermediate, observed!!.level)
    }

    @Test
    fun `updateProgramDay should replace matching day`() = runTest {
        val program = repository.createProgram(YogaLevel.Beginner)
        val originalDay = program.days.first()
        val edited = originalDay.copy(title = "Edited title")
        repository.updateProgramDay(edited)
        val updated = repository.observeProgram().first()!!
        assertEquals("Edited title", updated.days.first().title)
    }

    @Test
    fun `skipDay with autoShift should advance currentDay`() = runTest {
        repository.createProgram(YogaLevel.Beginner)
        repository.skipDay(1, autoShift = true)
        val program = repository.observeProgram().first()!!
        assertEquals(2, program.currentDay)
        assertTrue(program.days.first().isSkipped)
    }

    @Test
    fun `recordCompletion should write a progress entry`() = runTest {
        repository.createProgram(YogaLevel.Beginner)
        repository.recordCompletion("program-1", durationSec = 600)
        val progress = repository.observeProgress().first()
        assertEquals(1, progress.size)
        assertEquals("program-1", progress.first().dayId)
        assertEquals(600, progress.first().durationSec)
    }

    @Test
    fun `resetProgress should clear all entries`() = runTest {
        repository.createProgram(YogaLevel.Beginner)
        repository.recordCompletion("program-1", 600)
        repository.resetProgress()
        val progress = repository.observeProgress().first()
        assertTrue(progress.isEmpty())
    }

    @Test
    fun `createCustomDay should append to custom days`() = runTest {
        val day = repository.createCustomDay("My morning flow", listOf("tadasana", "vrksasana"))
        assertEquals("My morning flow", day.title)
        assertEquals(2, day.poses.size)
        val customDays = repository.observeCustomDays().first()
        assertEquals(1, customDays.size)
    }

    @Test
    fun `deleteCustomDay should remove matching id`() = runTest {
        val day = repository.createCustomDay("Temp", listOf("tadasana"))
        val removed = repository.deleteCustomDay(day.id)
        assertTrue(removed)
        assertTrue(repository.observeCustomDays().first().isEmpty())
    }
}
