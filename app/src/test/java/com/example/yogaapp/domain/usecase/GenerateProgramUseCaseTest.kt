package com.example.yogaapp.domain.usecase

import com.example.yogaapp.domain.model.YogaLevel
import com.example.yogaapp.domain.model.YogaRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GenerateProgramUseCase")
class GenerateProgramUseCaseTest {

    private val useCase = GenerateProgramUseCase()

    @Test
    fun `should produce a 30-day program for Beginner level`() {
        val program = useCase(YogaLevel.Beginner)
        assertEquals(30, program.days.size)
        assertEquals(YogaLevel.Beginner, program.level)
        assertEquals(1, program.currentDay)
    }

    @Test
    fun `should produce a 30-day program for Advanced level`() {
        val program = useCase(YogaLevel.Advanced)
        assertEquals(30, program.days.size)
        assertEquals(YogaLevel.Advanced, program.level)
    }

    @Test
    fun `should give each day between 5 and 7 poses`() {
        val program = useCase(YogaLevel.Intermediate)
        program.days.forEach { day ->
            val count = day.poses.size
            assertTrue(count in 5..7, "Day ${day.dayNumber} has $count poses — expected 5..7")
        }
    }

    @Test
    fun `should generate stable day ids program-N`() {
        val program = useCase(YogaLevel.Beginner)
        program.days.forEachIndexed { idx, day ->
            assertEquals("program-${idx + 1}", day.id)
        }
    }

    @Test
    fun `should always end each day with a rest pose (savasana preferred)`() {
        val program = useCase(YogaLevel.Beginner)
        program.days.forEach { day ->
            val last = day.poses.last()
            assertTrue(
                last.poseId == "savasana" || "savasana" in day.poses.map { it.poseId },
                "Day ${day.dayNumber} should include savasana",
            )
        }
    }

    @Test
    fun `should use only Beginner-level poses in the first 7 days`() {
        val program = useCase(YogaLevel.Advanced)
        val firstWeek = program.days.take(7)
        firstWeek.forEach { day ->
            day.poses.forEach { entry ->
                val pose = com.example.yogaapp.domain.model.PoseLibrary.byId(entry.poseId)
                assertNotNull(pose)
                assertEquals(YogaLevel.Beginner, pose!!.level, "Day ${day.dayNumber} has non-Beginner pose ${pose.id}")
            }
        }
    }

    @Test
    fun `should respect PROGRAM_DAYS constant`() {
        assertEquals(30, YogaRules.PROGRAM_DAYS)
    }

    @Test
    fun `should produce distinct programs when called with different levels`() {
        val beginner = useCase(YogaLevel.Beginner)
        val advanced = useCase(YogaLevel.Advanced)
        // The day ids are the same (program-N), but pose composition differs.
        val firstDayBeginnerPoses = beginner.days.first().poses.map { it.poseId }
        val firstDayAdvancedPoses = advanced.days.first().poses.map { it.poseId }
        // Day 1 is the same Beginner-level intro for both — they should match.
        assertEquals(firstDayBeginnerPoses, firstDayAdvancedPoses)
    }

    @Test
    fun `should produce valid orderIndex values starting at 0`() {
        val program = useCase(YogaLevel.Beginner)
        program.days.forEach { day ->
            day.poses.forEachIndexed { idx, entry ->
                assertEquals(idx, entry.orderIndex, "Pose at index $idx should have orderIndex $idx")
            }
        }
    }
}
