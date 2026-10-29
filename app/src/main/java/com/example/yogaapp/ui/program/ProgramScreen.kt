package com.example.yogaapp.ui.program

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yogaapp.R
import com.example.yogaapp.domain.model.YogaDay
import com.example.yogaapp.ui.components.EmptyState
import com.example.yogaapp.ui.components.ErrorState
import com.example.yogaapp.ui.components.LoadingState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramScreen(
    onDayClick: (String) -> Unit,
    onSessionClick: (String) -> Unit,
    onLibraryClick: () -> Unit,
    onCreateCustomDay: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ProgramViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProgramEffect.ShowError,
                is ProgramEffect.ShowSnackbar -> {
                    val res = when (effect) {
                        is ProgramEffect.ShowError -> effect.messageRes
                        is ProgramEffect.ShowSnackbar -> effect.messageRes
                        else -> R.string.error_unknown
                    }
                    scope.launch { snackbarHostState.showSnackbar(context.getString(res)) }
                }
                ProgramEffect.RequestLevelSelection -> Unit // no-op — onboarding handles this
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.program_title)) },
                actions = {
                    val cdLibrary = stringResource(R.string.library_title)
                    IconButton(onClick = onLibraryClick, modifier = Modifier.semantics { contentDescription = cdLibrary }) {
                        Icon(Icons.Filled.AutoStories, contentDescription = null)
                    }
                    val cdSettings = stringResource(R.string.cd_settings)
                    IconButton(onClick = onSettingsClick, modifier = Modifier.semantics { contentDescription = cdSettings }) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            val cd = stringResource(R.string.cd_create_day)
            FloatingActionButton(
                onClick = onCreateCustomDay,
                modifier = Modifier.semantics { contentDescription = cd },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val s = state) {
            ProgramUiState.Loading -> LoadingState(modifier = Modifier.padding(padding))
            ProgramUiState.Empty -> EmptyState(
                icon = Icons.Outlined.CalendarToday,
                titleRes = R.string.program_empty_title,
                bodyRes = R.string.program_empty_body,
                actionLabelRes = R.string.program_select_level,
                onAction = { viewModel.onEvent(ProgramEvent.EnsurePlan) },
                modifier = Modifier.padding(padding),
            )
            is ProgramUiState.Error -> ErrorState(
                messageRes = s.messageRes,
                onRetry = { viewModel.onEvent(ProgramEvent.Retry) },
                modifier = Modifier.padding(padding),
            )
            is ProgramUiState.Content -> ProgramBody(
                state = s,
                onDayClick = onDayClick,
                onSessionClick = onSessionClick,
                onSkipDay = { dayNum -> viewModel.onEvent(ProgramEvent.SkipDay(dayNum)) },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ProgramBody(
    state: ProgramUiState.Content,
    onDayClick: (String) -> Unit,
    onSessionClick: (String) -> Unit,
    onSkipDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            WorkoutBlocksSection(onDayClick = onDayClick, onSessionClick = onSessionClick)
        }
        item {
            ProgressHeader(
                completedCount = state.completedCount,
                totalCount = state.days.size,
            )
        }
        items(items = state.days, key = { it.id }) { day ->
            val isCompleted = state.completedDayIds.contains(day.id)
            val isCurrent = day.dayNumber == state.currentDayNumber
            DayCard(
                day = day,
                isCurrent = isCurrent,
                isCompleted = isCompleted,
                onClick = { onDayClick(day.id) },
                onStartSession = { onSessionClick(day.id) },
                onSkip = { onSkipDay(day.dayNumber) },
            )
        }
    }
}

@Composable
private fun WorkoutBlocksSection(
    onDayClick: (String) -> Unit,
    onSessionClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.blocks_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        BlockCard(
            title = stringResource(R.string.block_weight_loss_title),
            subtitle = stringResource(R.string.block_weight_loss_subtitle),
            onOpen = { onDayClick(com.example.yogaapp.domain.model.WorkoutBlocks.WEIGHT_LOSS_ID) },
            onStart = { onSessionClick(com.example.yogaapp.domain.model.WorkoutBlocks.WEIGHT_LOSS_ID) },
        )
        BlockCard(
            title = stringResource(R.string.block_strength_title),
            subtitle = stringResource(R.string.block_strength_subtitle),
            onOpen = { onDayClick(com.example.yogaapp.domain.model.WorkoutBlocks.STRENGTH_ID) },
            onStart = { onSessionClick(com.example.yogaapp.domain.model.WorkoutBlocks.STRENGTH_ID) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockCard(
    title: String,
    subtitle: String,
    onOpen: () -> Unit,
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        onClick = onOpen,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
            androidx.compose.material3.TextButton(onClick = onStart) {
                Text(stringResource(R.string.action_start_session))
            }
        }
    }
}

@Composable
private fun ProgressHeader(completedCount: Int, totalCount: Int) {
    val pct = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
            text = stringResource(R.string.program_progress, completedCount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayCard(
    day: YogaDay,
    isCurrent: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    onStartSession: () -> Unit,
    onSkip: () -> Unit,
) {
    val cardColors = if (isCurrent) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors()
    }
    val cd = stringResource(R.string.cd_day_template, day.dayNumber)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = cd },
        colors = cardColors,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${day.dayNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = day.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${day.poses.size} " + stringResource(R.string.program_poses_count_template, day.poses.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                isCompleted -> Text(
                    text = stringResource(R.string.program_completed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                isCurrent -> Text(
                    text = stringResource(R.string.program_today),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                day.isSkipped -> Text(
                    text = stringResource(R.string.program_skipped),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
