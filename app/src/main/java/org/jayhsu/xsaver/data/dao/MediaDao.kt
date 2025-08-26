package org.jayhsu.xsaver.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import org.jayhsu.xsaver.data.model.MediaItem
import org.jayhsu.xsaver.data.model.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Insert
    suspend fun insert(mediaItem: MediaItem)

    @Delete
    suspend fun delete(mediaItem: MediaItem)

    @Query("SELECT * FROM media_items ORDER BY downloadedAt DESC")
    fun getAllMediaItems(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE type = :type ORDER BY downloadedAt DESC")
    fun getMediaItemsByType(type: MediaType): Flow<List<MediaItem>>

    @Query("DELETE FROM media_items")
    suspend fun deleteAll()

    @Query("UPDATE media_items SET avatarUrl = :avatarUrl, accountName = :accountName WHERE id = :id")
    suspend fun updateMeta(id: String, avatarUrl: String?, accountName: String?)
}