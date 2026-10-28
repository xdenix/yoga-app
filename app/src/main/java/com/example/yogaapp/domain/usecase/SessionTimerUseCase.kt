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
 * Per-session countdown timer with optional pause between poses.
 */
class SessionTimerUseCase @Inject constructor(
    private val dispatchers: AppDispatchers,
) {

    private val _state = MutableStateFlow(SessionTimerState())
    val state: StateFlow<SessionTimerState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var pauseBetweenSec: Int = YogaRules.DEFAULT_PAUSE_BETWEEN_SEC

    fun start(
        poses: List<Pair<YogaPose, DayPoseEntry>>,
        globalDurationSec: Int?,
        pauseBetweenSec: Int = YogaRules.DEFAULT_PAUSE_BETWEEN_SEC,
    ) {
        cancel()
        this.pauseBetweenSec = pauseBetweenSec.coerceIn(YogaRules.MIN_PAUSE_SEC, YogaRules.MAX_PAUSE_SEC)
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
            isPause = false,
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
        advanceToNext(forceSkipPause = true)
    }

    fun previous() {
        val current = _state.value
        if (current.isPause) {
            _state.value = current.copy(
                isPause = false,
                remaining = current.poses[current.currentIndex].durationSec,
            )
            tickJob?.cancel()
            if (current.isRunning) startTicking()
            return
        }
        if (current.currentIndex == 0) return
        val newIndex = current.currentIndex - 1
        _state.value = current.copy(
            currentIndex = newIndex,
            remaining = current.poses[newIndex].durationSec,
            isPause = false,
        )
        tickJob?.cancel()
        if (current.isRunning) startTicking()
    }

    fun setDurationForCurrentPose(sec: Int) {
        val current = _state.value
        if (current.isFinished || current.isPause) return
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
                    advanceToNext(forceSkipPause = false)
                } else {
                    _state.value = current.copy(remaining = current.remaining - 1)
                }
            }
        }
    }

    private fun advanceToNext(forceSkipPause: Boolean) {
        val current = _state.value
        if (current.isFinished) return

        if (current.isPause) {
            goToPose(current.currentIndex + 1)
            return
        }

        val nextIndex = current.currentIndex + 1
        if (nextIndex >= current.poses.size) {
            _state.value = current.copy(
                isRunning = false,
                isFinished = true,
                remaining = 0,
                isPause = false,
            )
            tickJob?.cancel()
            return
        }

        if (!forceSkipPause && pauseBetweenSec > 0) {
            _state.value = current.copy(
                isPause = true,
                remaining = pauseBetweenSec,
            )
            return
        }

        goToPose(nextIndex)
    }

    private fun goToPose(index: Int) {
        val current = _state.value
        if (index >= current.poses.size) {
            _state.value = current.copy(
                isRunning = false,
                isFinished = true,
                remaining = 0,
                isPause = false,
            )
            tickJob?.cancel()
            return
        }
        _state.value = current.copy(
            currentIndex = index,
            remaining = current.poses[index].durationSec,
            isPause = false,
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
    val isPause: Boolean = false,
) {
    val current: SessionPose? get() = poses.getOrNull(currentIndex)
    val totalDurationSec: Int
        get() = poses.sumOf { it.durationSec }
    val elapsedSec: Int
        get() {
            val done = poses.subList(0, currentIndex).sumOf { it.durationSec }
            return if (isPause) {
                done + (current?.durationSec ?: 0)
            } else {
                done + (current?.durationSec?.minus(remaining) ?: 0)
            }
        }
}
