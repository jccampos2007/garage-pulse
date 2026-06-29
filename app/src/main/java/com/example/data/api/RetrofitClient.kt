package com.example.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    /**
     * Base URL for the GaragePulse REST API.
     * - Use 10.0.2.2 for Android emulator (maps to host machine's localhost)
     * - Use actual LAN IP (e.g. 192.168.x.x) for physical devices on same network
     * - Use localhost only for backend testing (not accessible from emulator/device)
     */
    private const val BASE_URL = "http://10.0.2.2:3000/api/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * Interceptor that injects the JWT Authorization header into every request
     * when a token is available in TokenManager.
     */
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val token = TokenManager.getToken()

        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        chain.proceed(newRequest)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    /**
     * Main GaragePulse API service for CRUD operations
     * (auth, vehicles, services, user profile)
     */
    val garageApiService: GarageApiService by lazy {
        retrofit.create(GarageApiService::class.java)
    }

    /**
     * AI Server API for audio processing, odometer scanning, and illustration generation.
     * Uses the same base URL and interceptors.
     */
    val aiServerApi: AiServerApi by lazy {
        retrofit.create(AiServerApi::class.java)
    }
}
