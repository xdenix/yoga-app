package com.example.yogaapp.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.yogaapp.R
import com.example.yogaapp.domain.model.YogaLevel
import com.example.yogaapp.ui.components.PoseFigure
import com.example.yogaapp.domain.model.PoseLibrary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (YogaLevel) -> Unit,
) {
    val pages = rememberOnboardingPages()
    val pagerState = rememberPagerState(initialPage = 0) { pages.size }
    val scope = rememberCoroutineScope()
    var selectedLevel by remember { mutableStateOf<YogaLevel?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { /* no-op on intro pages */ }) {
                Text(stringResource(R.string.onboarding_skip))
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            OnboardingPage(pages[page])
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            pages.forEachIndexed { index, _ ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 12.dp else 8.dp)
                        .clip(CircleShape),
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = if (isSelected) androidx.compose.ui.graphics.Color.Black
                            else androidx.compose.ui.graphics.Color.Gray,
                        )
                    }
                }
            }
        }
        if (pagerState.currentPage == pages.lastIndex) {
            LevelPicker(
                selected = selectedLevel,
                onSelect = { selectedLevel = it },
            )
            Spacer(Modifier.size(16.dp))
            Button(
                onClick = { selectedLevel?.let(onComplete) },
                enabled = selectedLevel != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_start),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            Button(
                onClick = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.onboarding_next))
            }
        }
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (page.isFigure) {
            val samplePose = remember { PoseLibrary.byId("vrksasana")!! }
            PoseFigure(
                pose = samplePose,
                size = 140.dp,
                strokeColor = MaterialTheme.colorScheme.primary,
                fillColor = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(Modifier.size(20.dp))
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LevelPicker(
    selected: YogaLevel?,
    onSelect: (YogaLevel) -> Unit,
) {
    Text(
        text = stringResource(R.string.onboarding_choose_level),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.size(8.dp))
    Text(
        text = stringResource(R.string.onboarding_choose_level_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.size(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        YogaLevel.entries.forEach { level ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .padding(4.dp),
                onClick = { onSelect(level) },
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = if (selected == level) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ),
            ) {
                Text(
                    text = stringResource(levelLabel(level)),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = if (selected == level) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun rememberOnboardingPages(): List<OnboardingPage> = listOf(
    OnboardingPage(
        icon = Icons.Filled.SelfImprovement,
        titleRes = R.string.onboarding_page1_title,
        bodyRes = R.string.onboarding_page1_body,
        isFigure = true,
    ),
    OnboardingPage(
        icon = Icons.Filled.WifiOff,
        titleRes = R.string.onboarding_page2_title,
        bodyRes = R.string.onboarding_page2_body,
    ),
    OnboardingPage(
        icon = Icons.Filled.VolumeUp,
        titleRes = R.string.onboarding_page3_title,
        bodyRes = R.string.onboarding_page3_body,
    ),
)

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
    val isFigure: Boolean = false,
)

private fun levelLabel(level: YogaLevel): Int = when (level) {
    YogaLevel.Beginner -> R.string.level_beginner
    YogaLevel.Intermediate -> R.string.level_intermediate
    YogaLevel.Advanced -> R.string.level_advanced
}
