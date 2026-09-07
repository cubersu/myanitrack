package com.myanitrack.core.database.converter

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * Room, listeleri ve java.time turlerini bilmez. Etiket listelerini ayirici ile,
 * tarihleri ise sayisal olarak sakliyoruz (siralama sorgulari icin de uygun).
 */
class RoomConverters {

    @TypeConverter
    fun stringListToString(value: List<String>?): String =
        value.orEmpty().filter { it.isNotBlank() }.joinToString(SEPARATOR)

    @TypeConverter
    fun stringToStringList(value: String?): List<String> =
        value?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun instantToEpochSecond(value: Instant?): Long? = value?.epochSecond

    @TypeConverter
    fun epochSecondToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochSecond)

    private companion object {
        /** Etiket metinlerinde gecmesi mumkun olmayan ASCII "unit separator" (0x1F). */
        val SEPARATOR: String = 31.toChar().toString()
    }
}
