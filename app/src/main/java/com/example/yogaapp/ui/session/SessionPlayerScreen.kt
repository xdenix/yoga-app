package com.example.yogaapp.ui.session

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yogaapp.R
import com.example.yogaapp.data.audio.PracticeMelody
import com.example.yogaapp.ui.components.LoadingState
import com.example.yogaapp.ui.components.PoseFigure
import com.example.yogaapp.ui.components.title
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionPlayerScreen(
    dayId: String,
    onFinish: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val melody by viewModel.melody.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SessionEffect.Completed, SessionEffect.Close -> onFinish()
                is SessionEffect.Error -> {
                    scope.launch {
                        // Simple inline toast-like effect
                        android.widget.Toast.makeText(context, effect.messageRes, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.session_title)) },
                navigationIcon = {
                    val cd = stringResource(R.string.cd_back)
                    IconButton(onClick = onFinish, modifier = Modifier.semantics { contentDescription = cd }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                SessionUiState.Loading -> LoadingState()
                is SessionUiState.Running -> RunningContent(
                    state = s,
                    melody = melody,
                    onEvent = viewModel::onEvent,
                    onMelody = viewModel::setMelody,
                )
                SessionUiState.Finished -> FinishedContent(
                    onComplete = { viewModel.onEvent(SessionEvent.Complete) },
                    onClose = { viewModel.onEvent(SessionEvent.Finish) },
                )
                is SessionUiState.Error -> Text(
                    text = stringResource(s.messageRes),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
}

@Composable
private fun RunningContent(
    state: SessionUiState.Running,
    melody: PracticeMelody,
    onEvent: (SessionEvent) -> Unit,
    onMelody: (PracticeMelody) -> Unit,
) {
    val current = state.current
    val progress = if (state.totalSec > 0) {
        state.elapsedSec.toFloat() / state.totalSec.toFloat()
    } else 0f
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.session_pose_n_of_m, state.currentIndex + 1, state.total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (current != null) {
            PoseFigure(pose = current.pose, size = 48.dp)
            Text(
                text = stringResource(R.string.session_melody),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PracticeMelody.entries.forEach { item ->
                    FilterChip(
                        selected = melody == item,
                        onClick = { onMelody(item) },
                        label = { Text(item.title()) },
                    )
                }
            }
            Text(
                text = current.pose.ruName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = current.pose.sanskritName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    text = current.pose.ruDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.session_remaining, state.remainingSec),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.primary,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val cdPrev = stringResource(R.string.cd_prev_pose)
            IconButton(onClick = { onEvent(SessionEvent.Previous) }, modifier = Modifier.semantics { contentDescription = cdPrev }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = null)
            }
            val pauseOrResume = if (state.isPaused) {
                Icons.Filled.PlayArrow to R.string.cd_resume
            } else {
                Icons.Filled.Pause to R.string.cd_pause
            }
            val (icon, labelRes) = pauseOrResume
            val cdPause = stringResource(labelRes)
            IconButton(onClick = {
                onEvent(if (state.isPaused) SessionEvent.Resume else SessionEvent.Pause)
            }, modifier = Modifier.semantics { contentDescription = cdPause }) {
                Icon(icon, contentDescription = null)
            }
            val cdSkip = stringResource(R.string.cd_skip_pose)
            IconButton(onClick = { onEvent(SessionEvent.Skip) }, modifier = Modifier.semantics { contentDescription = cdSkip }) {
                Icon(Icons.Filled.SkipNext, contentDescription = null)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val cdListen = stringResource(R.string.cd_listen_audio)
            IconButton(onClick = { onEvent(SessionEvent.ListenAgain) }, modifier = Modifier.semantics { contentDescription = cdListen }) {
                Icon(Icons.Filled.VolumeUp, contentDescription = null)
            }
            val cdStop = stringResource(R.string.cd_stop_audio)
            IconButton(onClick = { onEvent(SessionEvent.StopAudio) }, modifier = Modifier.semantics { contentDescription = cdStop }) {
                Icon(Icons.Filled.Stop, contentDescription = null)
            }
        }
    }
}

@Composable
private fun FinishedContent(
    onComplete: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.session_finished_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.session_finished_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.session_mark_done))
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_close))
        }
    }
}
