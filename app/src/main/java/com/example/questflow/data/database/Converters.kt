package com.example.questflow.data.database

import androidx.room.TypeConverter
import com.example.questflow.data.database.entity.XpSource
import com.example.questflow.data.database.entity.SkillType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(formatter)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let {
            LocalDateTime.parse(it, formatter)
        }
    }

    @TypeConverter
    fun fromXpSource(value: XpSource): String {
        return value.name
    }

    @TypeConverter
    fun toXpSource(value: String): XpSource {
        return XpSource.valueOf(value)
    }

    @TypeConverter
    fun fromSkillType(value: SkillType): String {
        return value.name
    }

    @TypeConverter
    fun toSkillType(value: String): SkillType {
        return SkillType.valueOf(value)
    }
}