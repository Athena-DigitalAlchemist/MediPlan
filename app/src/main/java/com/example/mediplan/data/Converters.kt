package com.example.mediplan.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun fromLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun fromLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it) }
    }

    @TypeConverter
    fun localTimeToString(time: LocalTime?): String? {
        return time?.toString()
    }

    @TypeConverter
    fun fromMedicationUnit(value: String?): MedicationUnit? {
        return value?.let { MedicationUnit.valueOf(it) }
    }

    @TypeConverter
    fun medicationUnitToString(unit: MedicationUnit?): String? {
        return unit?.name
    }

    @TypeConverter
    fun fromFrequencyType(value: String?): FrequencyType? {
        return value?.let { FrequencyType.valueOf(it) }
    }

    @TypeConverter
    fun frequencyTypeToString(type: FrequencyType?): String? {
        return type?.name
    }

    @TypeConverter
    fun fromMedicationStatus(value: String?): MedicationStatus? {
        return value?.let { MedicationStatus.valueOf(it) }
    }

    @TypeConverter
    fun medicationStatusToString(status: MedicationStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun stringListToString(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun fromDayOfWeekSet(value: String?): Set<DayOfWeek> {
        if (value == null) return emptySet()
        val type = object : TypeToken<Set<DayOfWeek>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun dayOfWeekSetToString(set: Set<DayOfWeek>?): String? {
        return set?.let { gson.toJson(it) }
    }
}
