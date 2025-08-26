package org.jayhsu.xsaver.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.jayhsu.xsaver.data.model.TweetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TweetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tweet: TweetEntity)

    @Query("SELECT * FROM tweets WHERE tweetUrl = :url LIMIT 1")
    suspend fun getTweet(url: String): TweetEntity?

    @Query("SELECT * FROM tweets ORDER BY createdAt DESC")
    fun getAllTweets(): Flow<List<TweetEntity>>
}
