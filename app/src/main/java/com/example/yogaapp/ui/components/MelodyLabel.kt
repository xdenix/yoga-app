package com.example.yogaapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.yogaapp.R
import com.example.yogaapp.data.audio.PracticeMelody

@Composable
fun PracticeMelody.title(): String = stringResource(
    when (this) {
        PracticeMelody.Off -> R.string.melody_off
        PracticeMelody.SufiMorning -> R.string.melody_sufi_morning
        PracticeMelody.SufiBreath -> R.string.melody_sufi_breath
        PracticeMelody.SufiRiver -> R.string.melody_sufi_river
        PracticeMelody.SufiEvening -> R.string.melody_sufi_evening
        PracticeMelody.SufiWhirl -> R.string.melody_sufi_whirl
    },
)
