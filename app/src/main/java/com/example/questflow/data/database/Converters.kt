package com.example.questflow.data.database

import androidx.room.TypeConverter
import com.example.questflow.data.database.entity.*
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

    // Task Metadata Type Converters
    @TypeConverter
    fun fromMetadataType(value: MetadataType): String {
        return value.name
    }

    @TypeConverter
    fun toMetadataType(value: String): MetadataType {
        return MetadataType.valueOf(value)
    }

    @TypeConverter
    fun fromPhoneType(value: PhoneType): String {
        return value.name
    }

    @TypeConverter
    fun toPhoneType(value: String): PhoneType {
        return PhoneType.valueOf(value)
    }

    @TypeConverter
    fun fromEmailType(value: EmailType): String {
        return value.name
    }

    @TypeConverter
    fun toEmailType(value: String): EmailType {
        return EmailType.valueOf(value)
    }

    @TypeConverter
    fun fromAddressType(value: AddressType): String {
        return value.name
    }

    @TypeConverter
    fun toAddressType(value: String): AddressType {
        return AddressType.valueOf(value)
    }

    @TypeConverter
    fun fromUrlType(value: UrlType): String {
        return value.name
    }

    @TypeConverter
    fun toUrlType(value: String): UrlType {
        return UrlType.valueOf(value)
    }

    @TypeConverter
    fun fromNoteFormat(value: NoteFormat): String {
        return value.name
    }

    @TypeConverter
    fun toNoteFormat(value: String): NoteFormat {
        return NoteFormat.valueOf(value)
    }
}