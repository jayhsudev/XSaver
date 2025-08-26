package org.jayhsu.xsaver.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Stores tweet level metadata so multiple media downloads can link back.
 */
@Entity(tableName = "tweets", indices = [Index(value = ["createdAt"])])
data class TweetEntity(
    @PrimaryKey val tweetUrl: String, // use full URL as primary key
    val avatarUrl: String?,
    val accountName: String?,
    val text: String?,
    val createdAt: Long = System.currentTimeMillis()
)
