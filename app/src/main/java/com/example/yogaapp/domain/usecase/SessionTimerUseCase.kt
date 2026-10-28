package com.example.yogaapp.domain.usecase

import com.example.yogaapp.domain.model.DayPoseEntry
import com.example.yogaapp.domain.model.YogaPose
import com.example.yogaapp.domain.model.YogaRules
import com.example.yogaapp.domain.util.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Per-session countdown timer.
 *
 * Drives the [com.example.yogaapp.ui.session.SessionPlayerScreen] timer UI:
 *  - holds the current [remaining] seconds
 *  - holds the current pose index in the session sequence
 *  - ticks once per second on [dispatchers.default]
 *  - emits a [SessionTickListener.onPoseFinished] when [remaining] hits 0, advancing to the
 *    next pose (or emitting onSessionFinished if it was the last)
 *
 * Pause/resume/skip/back all manipulate the internal coroutine job; the timer state is
 * always observable via [state] so the UI re-renders automatically.
 *
 * Pure domain logic — no Android imports — so the timer is JVM-testable.
 */
class SessionTimerUseCase @Inject constructor(
    private val dispatchers: AppDispatchers,
) {

    private val _state = MutableStateFlow(SessionTimerState())
    val state: StateFlow<SessionTimerState> = _state.asStateFlow()

    private var tickJob: Job? = null

    /**
     * Initialises the timer with the session's poses + per-pose durations.
     * [globalDurationSec] overrides individual pose durations when non-null.
     */
    fun start(poses: List<Pair<YogaPose, DayPoseEntry>>, globalDurationSec: Int?) {
        cancel()
        val entries = poses.mapIndexed { idx, (pose, entry) ->
            val duration = globalDurationSec
                ?: entry.durationOverrideSec
                ?: pose.defaultDurationSec
            SessionPose(
                pose = pose,
                entry = entry,
                durationSec = duration.coerceIn(YogaRules.MIN_DURATION_SEC, YogaRules.MAX_DURATION_SEC),
                index = idx,
            )
        }
        _state.value = SessionTimerState(
            poses = entries,
            currentIndex = 0,
            remaining = entries.firstOrNull()?.durationSec ?: 0,
            isRunning = true,
            isFinished = entries.isEmpty(),
        )
        startTicking()
    }

    fun pause() {
        tickJob?.cancel()
        _state.value = _state.value.copy(isRunning = false)
    }

    fun resume() {
        if (_state.value.isRunning || _state.value.isFinished) return
        _state.value = _state.value.copy(isRunning = true)
        startTicking()
    }

    fun skip() {
        advanceToNext()
    }

    fun previous() {
        val current = _state.value
        if (current.currentIndex == 0) return
        val newIndex = current.currentIndex - 1
        _state.value = current.copy(
            currentIndex = newIndex,
            remaining = current.poses[newIndex].durationSec,
        )
        tickJob?.cancel()
        if (current.isRunning) startTicking()
    }

    /** Adjusts the current pose's duration on the fly (user changed the per-pose slider). */
    fun setDurationForCurrentPose(sec: Int) {
        val current = _state.value
        if (current.isFinished) return
        val clamped = sec.coerceIn(YogaRules.MIN_DURATION_SEC, YogaRules.MAX_DURATION_SEC)
        val updatedPoses = current.poses.toMutableList().also { list ->
            list[current.currentIndex] = list[current.currentIndex].copy(durationSec = clamped)
        }
        _state.value = current.copy(
            poses = updatedPoses,
            remaining = clamped,
        )
    }

    fun cancel() {
        tickJob?.cancel()
        _state.value = SessionTimerState()
    }

    private fun startTicking() {
        val scope = CoroutineScope(dispatchers.default)
        tickJob = scope.launch {
            while (_state.value.isRunning && _state.value.remaining > 0 && !_state.value.isFinished) {
                delay(TICK_INTERVAL_MS)
                val current = _state.value
                if (current.remaining <= 1) {
                    advanceToNext()
                } else {
                    _state.value = current.copy(remaining = current.remaining - 1)
                }
            }
        }
    }

    private fun advanceToNext() {
        val current = _state.value
        if (current.isFinished) return
        val nextIndex = current.currentIndex + 1
        if (nextIndex >= current.poses.size) {
            _state.value = current.copy(
                isRunning = false,
                isFinished = true,
                remaining = 0,
            )
            tickJob?.cancel()
            return
        }
        _state.value = current.copy(
            currentIndex = nextIndex,
            remaining = current.poses[nextIndex].durationSec,
        )
    }

    companion object {
        private const val TICK_INTERVAL_MS = 1_000L
    }
}

data class SessionPose(
    val pose: YogaPose,
    val entry: DayPoseEntry,
    val durationSec: Int,
    val index: Int,
)

data class SessionTimerState(
    val poses: List<SessionPose> = emptyList(),
    val currentIndex: Int = 0,
    val remaining: Int = 0,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
) {
    val current: SessionPose? get() = poses.getOrNull(currentIndex)
    val totalDurationSec: Int get() = poses.sumOf { it.durationSec }
    val elapsedSec: Int
        get() = poses.subList(0, currentIndex).sumOf { it.durationSec } +
            (current?.durationSec?.minus(remaining) ?: 0)
}
