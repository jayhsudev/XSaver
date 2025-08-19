package org.jayhsu.xsaver.data.database

import androidx.room.TypeConverter
import org.jayhsu.xsaver.data.model.MediaType

class MediaTypeConverter {
    @TypeConverter
    fun fromMediaType(type: MediaType): String {
        return type.name
    }

    @TypeConverter
    fun toMediaType(type: String): MediaType {
        return MediaType.valueOf(type)
    }
}
