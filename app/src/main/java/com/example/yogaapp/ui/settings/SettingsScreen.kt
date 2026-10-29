package com.example.yogaapp.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.yogaapp.BuildConfig
import com.example.yogaapp.R
import com.example.yogaapp.data.audio.ExternalMusicLauncher
import com.example.yogaapp.data.audio.PracticeMelody
import com.example.yogaapp.data.datastore.AudioMode
import com.example.yogaapp.data.datastore.ThemeMode
import com.example.yogaapp.domain.model.YogaRules
import com.example.yogaapp.ui.components.IntCounter
import com.example.yogaapp.ui.components.title
import com.example.yogaapp.update.CheckResult
import com.example.yogaapp.update.GithubAppUpdater
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickMusic = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.setCustomMusicUri(uri.toString())
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.Snackbar ->
                    scope.launch { snackbarHostState.showSnackbar(context.getString(effect.messageRes)) }
                is SettingsEffect.Error ->
                    scope.launch { snackbarHostState.showSnackbar(context.getString(effect.messageRes)) }
                SettingsEffect.PickMusicFile -> pickMusic.launch(arrayOf("audio/*"))
                SettingsEffect.OpenYandexMusic -> ExternalMusicLauncher.openYandexMusic(context)
                SettingsEffect.OpenSystemMusic -> ExternalMusicLauncher.openSystemMusicPicker(context)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    val cd = stringResource(R.string.cd_back)
                    IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = cd }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeader(stringResource(R.string.settings_section_appearance))
            ThemeModeRow(current = state.themeMode, onSelect = viewModel::setThemeMode)
            HorizontalDivider()
            SwitchRow(
                title = stringResource(R.string.settings_dynamic_color_title),
                subtitle = stringResource(R.string.settings_dynamic_color_subtitle),
                checked = state.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor,
            )
            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_audio))
            SwitchRow(
                title = stringResource(R.string.settings_voice_enabled),
                subtitle = stringResource(R.string.settings_voice_enabled_subtitle),
                checked = state.audioMode != AudioMode.Off,
                onCheckedChange = viewModel::setVoiceEnabled,
            )
            if (state.audioMode != AudioMode.Off) {
                AudioModeRow(current = state.audioMode, onSelect = viewModel::setAudioMode)
                Text(
                    text = if (state.hasAudioPack) {
                        stringResource(R.string.settings_audio_pack_status_installed)
                    } else {
                        stringResource(R.string.settings_audio_pack_status_missing)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Text(
                    text = stringResource(R.string.settings_audio_pack_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_melody))
            MelodyRow(current = state.melody, onSelect = viewModel::setMelody)
            OutlinedButton(
                onClick = { pickMusic.launch(arrayOf("audio/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_pick_music_file))
            }
            if (!state.customMusicUri.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.settings_custom_music_selected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Text(
                text = stringResource(R.string.settings_melody_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_session))
            Text(
                text = stringResource(R.string.settings_poses_per_day),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            IntCounter(
                value = state.posesPerDay,
                onValueChange = viewModel::setPosesPerDay,
                valueRange = YogaRules.MIN_POSES_PER_DAY..YogaRules.MAX_POSES_PER_DAY,
                step = 1,
                valueText = stringResource(R.string.settings_poses_per_day_value, state.posesPerDay),
            )
            Text(
                text = stringResource(R.string.settings_global_duration),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            IntCounter(
                value = state.globalDurationSec,
                onValueChange = viewModel::setGlobalDuration,
                valueRange = YogaRules.MIN_DURATION_SEC..YogaRules.MAX_DURATION_SEC,
                step = 5,
                valueText = stringResource(R.string.settings_global_duration_value, state.globalDurationSec),
            )
            Text(
                text = stringResource(R.string.settings_pause_between),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            IntCounter(
                value = state.pauseBetweenSec,
                onValueChange = viewModel::setPauseBetweenSec,
                valueRange = YogaRules.MIN_PAUSE_SEC..YogaRules.MAX_PAUSE_SEC,
                step = 5,
                valueText = stringResource(R.string.settings_pause_between_value, state.pauseBetweenSec),
            )
            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_progress))
            Text(
                text = stringResource(R.string.settings_reset_progress_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { viewModel.resetProgress() },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text(stringResource(R.string.settings_reset_progress))
            }
            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_updates))
            UpdateRow(snackbarHostState = snackbarHostState)
            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_about))
            AboutItem(
                label = stringResource(R.string.settings_version),
                value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            )
        }
    }
}

@Composable
private fun UpdateRow(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.settings_check_update),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f).padding(end = 16.dp),
            )
            if (checking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        OutlinedButton(
            onClick = {
                if (checking) return@OutlinedButton
                val activity = context.findActivity()
                if (activity == null) {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.update_no_activity))
                    }
                    return@OutlinedButton
                }
                // Prefer the host Activity instance so pending-install survives unknown-sources.
                val updater = (activity as? com.example.yogaapp.MainActivity)?.githubUpdaterForSettings()
                    ?: GithubAppUpdater(activity)
                checking = true
                scope.launch {
                    val result = try {
                        updater.checkExplicit()
                    } finally {
                        checking = false
                    }
                    val msg = when (result) {
                        CheckResult.NoUpdate -> context.getString(R.string.update_no_update)
                        is CheckResult.Available -> context.getString(R.string.update_found, result.label)
                        CheckResult.Error -> context.getString(R.string.update_error)
                        CheckResult.Busy -> context.getString(R.string.settings_check_update_in_progress)
                    }
                    snackbarHostState.showSnackbar(msg)
                }
            },
            enabled = !checking,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(stringResource(R.string.settings_check_update))
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun ThemeModeRow(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.System to R.string.settings_theme_system,
        ThemeMode.Light to R.string.settings_theme_light,
        ThemeMode.Dark to R.string.settings_theme_dark,
        ThemeMode.DustyRose to R.string.settings_theme_dusty_rose,
    )
    options.forEach { (mode, labelRes) ->
        val label = stringResource(labelRes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    role = Role.RadioButton
                    contentDescription = label
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = current == mode, onClick = { onSelect(mode) })
            Text(label, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun MelodyRow(current: PracticeMelody, onSelect: (PracticeMelody) -> Unit) {
    PracticeMelody.entries.forEach { melody ->
        val label = melody.title()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    role = Role.RadioButton
                    contentDescription = label
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = current == melody, onClick = { onSelect(melody) })
            Text(label, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun AudioModeRow(current: AudioMode, onSelect: (AudioMode) -> Unit) {
    val options = listOf(
        AudioMode.Tts to R.string.settings_audio_mode_tts,
        AudioMode.Mp3Pack to R.string.settings_audio_mode_mp3,
    )
    options.forEach { (mode, labelRes) ->
        val label = stringResource(labelRes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    role = Role.RadioButton
                    contentDescription = label
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = current == mode, onClick = { onSelect(mode) })
            Text(label, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AboutItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
