package com.pnd.android.loop.data.dao

import androidx.room.TypeConverter
import com.pnd.android.loop.util.toLocalDate
import java.time.LocalDate

class RoomTypeConverters {
    @TypeConverter
    fun fromMsTime(value: Long?): LocalDate? = value?.toLocalDate()
}