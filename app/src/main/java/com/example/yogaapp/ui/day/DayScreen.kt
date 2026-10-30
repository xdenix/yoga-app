package com.example.yogaapp.ui.day

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yogaapp.ui.components.IntCounter
import com.example.yogaapp.R
import com.example.yogaapp.domain.model.DayPoseEntry
import com.example.yogaapp.domain.model.PoseSide
import com.example.yogaapp.domain.model.YogaPose
import com.example.yogaapp.domain.model.YogaRules
import com.example.yogaapp.ui.components.ErrorState
import com.example.yogaapp.ui.components.LoadingState
import com.example.yogaapp.ui.components.PoseFigure
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayScreen(
    dayId: String,
    onBack: () -> Unit,
    onSwapClick: (String, Int) -> Unit,
    onStartSession: (String) -> Unit,
    viewModel: DayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DayEffect.Snackbar -> {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(effect.messageRes)) }
                }
                is DayEffect.Error -> {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(effect.messageRes)) }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleRes = when (val s = state) {
                        is DayUiState.Content -> s.day.title
                        else -> ""
                    }
                    Text(titleRes)
                },
                navigationIcon = {
                    val cd = stringResource(R.string.cd_back)
                    IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = cd }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    val showSkip = when (val s = state) {
                        is DayUiState.Content ->
                            com.example.yogaapp.domain.model.WorkoutBlocks.byId(s.day.id) == null
                        else -> false
                    }
                    if (showSkip) {
                        val cd = stringResource(R.string.day_skip)
                        IconButton(
                            onClick = { viewModel.onEvent(DayEvent.SkipDay) },
                            modifier = Modifier.semantics { contentDescription = cd },
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                        }
                    }
                },
            )
        },
        bottomBar = {
            when (val s = state) {
                is DayUiState.Content -> BottomActions(
                    totalSec = s.totalDurationSec,
                    onStart = { onStartSession(s.day.id) },
                    onReset = { viewModel.onEvent(DayEvent.ResetOverrides) },
                )
                else -> Unit
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val s = state) {
            DayUiState.Loading -> LoadingState(modifier = Modifier.padding(padding))
            DayUiState.NotFound -> ErrorState(
                messageRes = R.string.error_not_found,
                onRetry = onBack,
                modifier = Modifier.padding(padding),
            )
            is DayUiState.Error -> ErrorState(
                messageRes = s.messageRes,
                onRetry = { viewModel.onEvent(DayEvent.Retry) },
                modifier = Modifier.padding(padding),
            )
            is DayUiState.Content -> DayContent(
                state = s,
                onReorder = { from, to -> viewModel.onEvent(DayEvent.Reorder(from, to)) },
                onSwapClick = { orderIndex -> onSwapClick(s.day.id, orderIndex) },
                onSetDuration = { orderIndex, sec ->
                    viewModel.onEvent(DayEvent.SetDuration(orderIndex, sec))
                },
                onDelete = { orderIndex -> viewModel.onEvent(DayEvent.DeletePose(orderIndex)) },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun DayContent(
    state: DayUiState.Content,
    onReorder: (Int, Int) -> Unit,
    onSwapClick: (Int) -> Unit,
    onSetDuration: (Int, Int) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.day_drag_to_reorder),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = state.day.poses, key = { "${it.poseId}-${it.orderIndex}-${it.side}" }) { entry ->
                val pose = state.poseMap[entry.poseId]
                if (pose != null) {
                    PoseCard(
                        entry = entry,
                        pose = pose,
                        globalDurationSec = state.globalDurationSec,
                        onSwapClick = { onSwapClick(entry.orderIndex) },
                        onSetDuration = { sec -> onSetDuration(entry.orderIndex, sec) },
                        onDelete = { onDelete(entry.orderIndex) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PoseCard(
    entry: DayPoseEntry,
    pose: YogaPose,
    globalDurationSec: Int,
    onSwapClick: () -> Unit,
    onSetDuration: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    val cd = stringResource(R.string.cd_pose_card_template, pose.ruName)
    val currentDuration = entry.durationOverrideSec ?: globalDurationSec
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = cd },
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { /* visual feedback could be added here */ },
                    ) { change, _ ->
                        change.consume()
                    }
                },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = stringResource(R.string.cd_reorder),
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp),
                )
                PoseFigure(
                    pose = pose,
                    size = 48.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = poseTitle(pose.ruName, entry.side),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    Text(
                        text = pose.sanskritName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(
                            R.string.day_duration_template,
                            YogaRules.durationLabel(currentDuration),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
                val cdSwap = stringResource(R.string.cd_swap_pose)
                IconButton(
                    onClick = onSwapClick,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = cdSwap },
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                }
                val cdDel = stringResource(R.string.cd_delete_pose)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = cdDel },
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                IntCounter(
                    value = currentDuration,
                    onValueChange = onSetDuration,
                    valueRange = YogaRules.MIN_DURATION_SEC..YogaRules.MAX_DURATION_SEC,
                    step = 5,
                    valueText = YogaRules.durationLabel(currentDuration),
                )
            }
        }
    }
}

@Composable
private fun poseTitle(ruName: String, side: PoseSide): String = when (side) {
    PoseSide.Right -> stringResource(R.string.pose_title_side, ruName, stringResource(R.string.pose_side_right))
    PoseSide.Left -> stringResource(R.string.pose_title_side, ruName, stringResource(R.string.pose_side_left))
    PoseSide.None -> ruName
}

@Composable
private fun BottomActions(
    totalSec: Int,
    onStart: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.day_reset_overrides))
        }
        Button(onClick = onStart, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Text(
                text = " ${stringResource(R.string.action_start_session)}",
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}
