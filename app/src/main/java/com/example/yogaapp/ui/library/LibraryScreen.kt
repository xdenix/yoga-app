package com.example.yogaapp.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.yogaapp.domain.model.YogaLevel
import com.example.yogaapp.domain.model.YogaPose
import com.example.yogaapp.ui.components.LoadingState
import com.example.yogaapp.ui.components.PoseFigure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    onPicked: (String) -> Unit = {},
    pickerMode: Boolean = false,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (pickerMode) stringResource(R.string.action_swap)
                        else stringResource(R.string.library_title),
                    )
                },
                navigationIcon = {
                    val cd = stringResource(R.string.cd_back)
                    IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = cd }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SearchAndFilters(
                query = (state as? LibraryUiState.Content)?.query.orEmpty(),
                filter = (state as? LibraryUiState.Content)?.filter,
                onQueryChanged = viewModel::setQuery,
                onFilterChanged = viewModel::setFilter,
            )
            when (val s = state) {
                LibraryUiState.Loading -> LoadingState()
                is LibraryUiState.Error -> Text(
                    text = stringResource(s.messageRes),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp),
                )
                is LibraryUiState.Content -> PoseGrid(
                    poses = s.poses,
                    onClick = { pose ->
                        if (pickerMode) onPicked(pose.id)
                        // In normal mode the library is read-only — tapping could open a detail.
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchAndFilters(
    query: String,
    filter: YogaLevel?,
    onQueryChanged: (String) -> Unit,
    onFilterChanged: (YogaLevel?) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.library_title)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        )
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = filter == null,
                onClick = { onFilterChanged(null) },
                label = { Text(stringResource(R.string.library_filter_all)) },
            )
            YogaLevel.entries.forEach { level ->
                FilterChip(
                    selected = filter == level,
                    onClick = { onFilterChanged(level) },
                    label = { Text(stringResource(levelLabel(level))) },
                )
            }
        }
    }
}

@Composable
private fun PoseGrid(poses: List<YogaPose>, onClick: (YogaPose) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = poses, key = { it.id }) { pose ->
            PoseCard(pose = pose, onClick = { onClick(pose) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoseCard(pose: YogaPose, onClick: () -> Unit) {
    val cd = stringResource(R.string.cd_pose_card_template, pose.ruName)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = cd },
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PoseFigure(pose = pose, size = 100.dp)
            Text(
                text = pose.ruName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = pose.sanskritName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun levelLabel(level: YogaLevel): Int = when (level) {
    YogaLevel.Beginner -> R.string.level_beginner
    YogaLevel.Intermediate -> R.string.level_intermediate
    YogaLevel.Advanced -> R.string.level_advanced
}
