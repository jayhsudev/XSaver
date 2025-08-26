package org.jayhsu.xsaver.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.jayhsu.xsaver.data.dao.MediaDao
import org.jayhsu.xsaver.data.dao.DownloadTaskDao
import org.jayhsu.xsaver.data.dao.TweetDao
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.TweetEntity
import org.jayhsu.xsaver.data.model.DownloadTaskEntity

@Database(
    entities = [MediaItem::class, TweetEntity::class, DownloadTaskEntity::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(MediaTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun tweetDao(): TweetDao
    abstract fun downloadTaskDao(): DownloadTaskDao
}