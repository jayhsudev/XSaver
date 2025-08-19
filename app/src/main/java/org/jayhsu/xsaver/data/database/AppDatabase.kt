package org.jayhsu.xsaver.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.jayhsu.xsaver.data.dao.MediaDao
import org.jayhsu.xsaver.data.model.MediaItem

@Database(
    entities = [MediaItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(MediaTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}