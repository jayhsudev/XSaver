package org.jayhsu.xsaver.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.jayhsu.xsaver.data.model.DownloadTaskEntity
import org.jayhsu.xsaver.data.model.DownloadStatus

@Dao
interface DownloadTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadTaskEntity)

    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE id = :id")
    suspend fun get(id: String): DownloadTaskEntity?

    @Query("UPDATE download_tasks SET status=:status, downloadedBytes=:downloaded, totalBytes=:total, updatedAt=:updatedAt, error=:error, errorType=:errorType, errorCode=:errorCode WHERE id=:id")
    suspend fun updateProgress(id: String, status: DownloadStatus, downloaded: Long, total: Long?, updatedAt: Long, error: String?, errorType: String?, errorCode: Int?)

    @Query("DELETE FROM download_tasks WHERE id=:id")
    suspend fun delete(id: String)

    @Query("DELETE FROM download_tasks WHERE status IN ('Completed','Canceled','Error') AND updatedAt < :before")
    suspend fun cleanupFinished(before: Long)
}