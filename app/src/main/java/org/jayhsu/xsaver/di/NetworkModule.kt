package org.jayhsu.xsaver.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Cache
import java.util.concurrent.TimeUnit
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import org.jayhsu.xsaver.BuildConfig
import org.jayhsu.xsaver.network.ApiService
import retrofit2.Retrofit

/**
 * 网络相关依赖注入模块：提供 OkHttp、Retrofit 与 ApiService。
 * 解决之前 Hilt 构建失败的 MissingBinding(ApiService) 问题。
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

	@Provides
	@Singleton
	fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
		val cacheSize = 50L * 1024 * 1024 // 50MB
		val cache = Cache(File(context.cacheDir, "http_cache"), cacheSize)
		val builder = OkHttpClient.Builder()
			.cache(cache)
			.connectTimeout(10, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.writeTimeout(30, TimeUnit.SECONDS)
			.connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
		// 在 Debug 下添加日志拦截器
		if (BuildConfig.DEBUG) {
			val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
			builder.addInterceptor(logging)
		}
		return builder.build()
	}

	@Provides
	@Singleton
	fun provideJson(): Json = Json {
		ignoreUnknownKeys = true
		isLenient = true
		encodeDefaults = false
	}

	@Provides
	@Singleton
	fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
		val contentType = "application/json".toMediaType()
		return Retrofit.Builder()
			.baseUrl(BuildConfig.API_BASE_URL)
			.client(okHttpClient)
			.addConverterFactory(json.asConverterFactory(contentType))
			.build()
	}

	@Provides
	@Singleton
	fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)
}