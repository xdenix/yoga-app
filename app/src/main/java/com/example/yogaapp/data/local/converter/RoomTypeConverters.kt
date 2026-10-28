package com.example.yogaapp.data.local.converter

import androidx.room.TypeConverter
import com.example.yogaapp.domain.model.BodyFocus
import com.example.yogaapp.domain.model.YogaLevel

class RoomTypeConverters {

    @TypeConverter
    fun levelToName(level: YogaLevel): String = level.name

    @TypeConverter
    fun nameToLevel(name: String): YogaLevel = YogaLevel.valueOf(name)

    @TypeConverter
    fun focusesToString(focuses: List<BodyFocus>): String =
        focuses.joinToString(separator = ",") { it.name }

    @TypeConverter
    fun stringToFocuses(value: String): List<BodyFocus> =
        if (value.isBlank()) emptyList()
        else value.split(",").map { BodyFocus.valueOf(it.trim()) }

    @TypeConverter
    fun stringListToString(list: List<String>): String =
        list.joinToString(separator = ",")

    @TypeConverter
    fun stringToStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(",")
}
