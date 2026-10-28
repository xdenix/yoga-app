package com.example.yogaapp.domain.usecase

import com.example.yogaapp.domain.model.PoseLibrary
import com.example.yogaapp.domain.model.PoseSide
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
    fun `should give each day about the requested slot count by default 10`() {
        val program = useCase(YogaLevel.Intermediate)
        program.days.forEach { day ->
            assertTrue(
                day.poses.size in (YogaRules.DEFAULT_POSES_PER_DAY - 1)..YogaRules.DEFAULT_POSES_PER_DAY,
                "Day ${day.dayNumber} has ${day.poses.size} slots",
            )
        }
    }

    @Test
    fun `should respect custom posesPerDay`() {
        val program = useCase(YogaLevel.Beginner, posesPerDay = 5)
        program.days.forEach { day ->
            assertTrue(
                day.poses.size in 4..5,
                "Day ${day.dayNumber} has ${day.poses.size} slots",
            )
        }
    }

    @Test
    fun `should expand bilateral poses into right then left`() {
        val program = useCase(YogaLevel.Beginner, posesPerDay = 10)
        val bilateralIds = PoseLibrary.all.filter { it.bilateral }.map { it.id }.toSet()
        program.days.forEach { day ->
            day.poses.filter { it.poseId in bilateralIds }.forEach { entry ->
                assertTrue(
                    entry.side == PoseSide.Right || entry.side == PoseSide.Left,
                    "Bilateral ${entry.poseId} must have a side",
                )
            }
            // Consecutive same bilateral id should be Right then Left
            day.poses.zipWithNext().forEach { (a, b) ->
                if (a.poseId == b.poseId && a.poseId in bilateralIds) {
                    assertEquals(PoseSide.Right, a.side)
                    assertEquals(PoseSide.Left, b.side)
                }
            }
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
                val pose = PoseLibrary.byId(entry.poseId)
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
        assertTrue(beginner.days != advanced.days || beginner.level != advanced.level)
    }

    @Test
    fun `toDayEntries expands tree into two sides`() {
        val tree = PoseLibrary.byId("vrksasana")!!
        val mountain = PoseLibrary.byId("tadasana")!!
        val entries = YogaRules.toDayEntries(listOf(tree, mountain))
        assertEquals(3, entries.size)
        assertEquals(PoseSide.Right, entries[0].side)
        assertEquals(PoseSide.Left, entries[1].side)
        assertEquals(PoseSide.None, entries[2].side)
        assertEquals("vrksasana", entries[0].poseId)
        assertEquals("vrksasana", entries[1].poseId)
    }
}
