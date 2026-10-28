package com.example.yogaapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.yogaapp.R
import com.example.yogaapp.data.audio.PracticeMelody

@Composable
fun PracticeMelody.title(): String = stringResource(
    when (this) {
        PracticeMelody.Off -> R.string.melody_off
        PracticeMelody.Morning -> R.string.melody_morning
        PracticeMelody.Breath -> R.string.melody_breath
        PracticeMelody.River -> R.string.melody_river
        PracticeMelody.Evening -> R.string.melody_evening
    },
)
