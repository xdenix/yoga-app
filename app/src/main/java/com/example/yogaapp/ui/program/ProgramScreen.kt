package com.example.yogaapp.ui.program

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.yogaapp.R
import com.example.yogaapp.domain.model.WorkoutBlocks
import com.example.yogaapp.domain.model.YogaDay
import com.example.yogaapp.ui.components.EmptyState
import com.example.yogaapp.ui.components.ErrorState
import com.example.yogaapp.ui.components.LoadingState
import com.example.yogaapp.ui.components.WorkoutCalendarCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var showPlanDays by remember { mutableStateOf(false) }

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

    BackHandler(enabled = showPlanDays) { showPlanDays = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (showPlanDays) R.string.block_plan_title else R.string.program_hub_title,
                        ),
                    )
                },
                navigationIcon = {
                    if (showPlanDays) {
                        val cd = stringResource(R.string.cd_back)
                        IconButton(
                            onClick = { showPlanDays = false },
                            modifier = Modifier.semantics { contentDescription = cd },
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                },
                actions = {
                    if (!showPlanDays) {
                        val cdLibrary = stringResource(R.string.library_title)
                        IconButton(
                            onClick = onLibraryClick,
                            modifier = Modifier.semantics { contentDescription = cdLibrary },
                        ) {
                            Icon(Icons.Filled.AutoStories, contentDescription = null)
                        }
                        val cdSettings = stringResource(R.string.cd_settings)
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.semantics { contentDescription = cdSettings },
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = null)
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (showPlanDays) {
                val cd = stringResource(R.string.cd_create_day)
                FloatingActionButton(
                    onClick = onCreateCustomDay,
                    modifier = Modifier.semantics { contentDescription = cd },
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
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
            is ProgramUiState.Content -> {
                if (showPlanDays) {
                    PlanDaysBody(
                        state = s,
                        onDayClick = onDayClick,
                        onSessionClick = onSessionClick,
                        onSkipDay = { dayNum -> viewModel.onEvent(ProgramEvent.SkipDay(dayNum)) },
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    BlocksHubBody(
                        state = s,
                        onOpenPlan = { showPlanDays = true },
                        onDayClick = onDayClick,
                        onSessionClick = onSessionClick,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}

@Composable
private fun BlocksHubBody(
    state: ProgramUiState.Content,
    onOpenPlan: () -> Unit,
    onDayClick: (String) -> Unit,
    onSessionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = state.days.firstOrNull { it.dayNumber == state.currentDayNumber }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.calendar_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            WorkoutCalendarCard(
                trainedDates = state.trainedDates,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        item {
            Text(
                text = stringResource(R.string.blocks_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        item {
            BlockCard(
                title = stringResource(R.string.block_plan_title),
                subtitle = stringResource(
                    R.string.block_plan_subtitle,
                    state.completedCount,
                    state.days.size,
                    current?.title ?: stringResource(R.string.program_day_template, state.currentDayNumber),
                ),
                coverAssetPath = "blocks/cover_plan.jpg",
                onOpen = onOpenPlan,
                onStart = {
                    val dayId = current?.id
                    if (dayId != null) onSessionClick(dayId) else onOpenPlan()
                },
                startLabel = stringResource(R.string.block_plan_continue),
            )
        }
        item {
            BlockCard(
                title = stringResource(R.string.block_weight_loss_title),
                subtitle = stringResource(R.string.block_weight_loss_subtitle),
                coverAssetPath = "blocks/cover_weight_loss.jpg",
                onOpen = { onDayClick(WorkoutBlocks.WEIGHT_LOSS_ID) },
                onStart = { onSessionClick(WorkoutBlocks.WEIGHT_LOSS_ID) },
            )
        }
        item {
            BlockCard(
                title = stringResource(R.string.block_strength_title),
                subtitle = stringResource(R.string.block_strength_subtitle),
                coverAssetPath = "blocks/cover_strength.jpg",
                onOpen = { onDayClick(WorkoutBlocks.STRENGTH_ID) },
                onStart = { onSessionClick(WorkoutBlocks.STRENGTH_ID) },
            )
        }
    }
}

@Composable
private fun PlanDaysBody(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockCard(
    title: String,
    subtitle: String,
    coverAssetPath: String,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    startLabel: String = stringResource(R.string.action_start_session),
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        onClick = onOpen,
    ) {
        Column {
            BlockCoverImage(
                assetPath = coverAssetPath,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            )
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
                TextButton(onClick = onStart) {
                    Text(startLabel)
                }
            }
        }
    }
}

@Composable
private fun BlockCoverImage(
    assetPath: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bytes by produceState<ByteArray?>(initialValue = null, assetPath) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.assets.open(assetPath).use { it.readBytes() } }.getOrNull()
        }
    }
    val request = remember(bytes) {
        bytes?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(200)
                .build()
        }
    }
    Box(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
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
