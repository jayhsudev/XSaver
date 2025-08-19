package org.jayhsu.xsaver.di

import android.content.Context
import androidx.room.Room
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
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "xsaver_database"
            )
            .fallbackToDestructiveMigration() // 在开发阶段允许破坏性迁移
            .build()
        }

        @Provides
        fun provideMediaDao(database: AppDatabase) = database.mediaDao()
    }
}