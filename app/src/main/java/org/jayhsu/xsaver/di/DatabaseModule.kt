package org.jayhsu.xsaver.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.jayhsu.xsaver.data.database.AppDatabase
import org.jayhsu.xsaver.data.repository.MediaRepository
import org.jayhsu.xsaver.data.repository.MediaRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {
    
    @Binds
    abstract fun bindMediaRepository(
        mediaRepositoryImpl: MediaRepositoryImpl
    ): MediaRepository

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建 tweets 表
                db.execSQL("CREATE TABLE IF NOT EXISTS `tweets` (`tweetUrl` TEXT NOT NULL, `avatarUrl` TEXT, `accountName` TEXT, `text` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`tweetUrl`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tweets_createdAt` ON `tweets` (`createdAt`)")
                // 为 media_items 添加列 (try/catch 避免重复添加异常)
                fun tryExec(sql: String) { try { db.execSQL(sql) } catch (_: Throwable) { } }
                tryExec("ALTER TABLE `media_items` ADD COLUMN `tweetId` TEXT")
                tryExec("ALTER TABLE `media_items` ADD COLUMN `avatarUrl` TEXT")
                tryExec("ALTER TABLE `media_items` ADD COLUMN `accountName` TEXT")
                // 轻量回填：假设 sourceUrl 即 tweetId
                db.execSQL("UPDATE media_items SET tweetId = sourceUrl WHERE tweetId IS NULL")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 添加索引 (若已存在忽略异常)
                fun tryExec(sql: String) { try { db.execSQL(sql) } catch (_: Throwable) { } }
                tryExec("CREATE INDEX IF NOT EXISTS `index_media_items_downloadedAt` ON `media_items` (`downloadedAt`)")
                tryExec("CREATE INDEX IF NOT EXISTS `index_media_items_tweetId` ON `media_items` (`tweetId`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `download_tasks` (" +
                            "`id` TEXT NOT NULL, `url` TEXT NOT NULL, `fileName` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                            "`sourceUrl` TEXT NOT NULL, `tweetId` TEXT, `title` TEXT, `thumbnailUrl` TEXT, `totalBytes` INTEGER, " +
                            "`downloadedBytes` INTEGER NOT NULL, `status` TEXT NOT NULL, `error` TEXT, `errorType` TEXT, `errorCode` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_tasks_status` ON `download_tasks` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_download_tasks_createdAt` ON `download_tasks` (`createdAt`)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) { // add new columns if table already exists from prior version
            override fun migrate(db: SupportSQLiteDatabase) {
                fun tryExec(sql: String) { try { db.execSQL(sql) } catch (_: Throwable) { } }
                tryExec("ALTER TABLE `download_tasks` ADD COLUMN `errorType` TEXT")
                tryExec("ALTER TABLE `download_tasks` ADD COLUMN `errorCode` INTEGER")
            }
        }

        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "xsaver_database")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()

        @Provides
        fun provideMediaDao(database: AppDatabase) = database.mediaDao()

        @Provides
        fun provideTweetDao(database: AppDatabase) = database.tweetDao()

    @Provides
    fun provideDownloadTaskDao(database: AppDatabase) = database.downloadTaskDao()
    }
}