package com.example.yogaapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.yogaapp.R
import com.example.yogaapp.data.audio.PracticeMelody

@Composable
fun PracticeMelody.title(): String = stringResource(
    when (this) {
        PracticeMelody.Off -> R.string.melody_off
        PracticeMelody.BuiltIn -> R.string.melody_builtin_dervish
        PracticeMelody.FromFile -> R.string.melody_from_file
        PracticeMelody.YandexMusic -> R.string.melody_yandex
        PracticeMelody.External -> R.string.melody_external
    },
)
