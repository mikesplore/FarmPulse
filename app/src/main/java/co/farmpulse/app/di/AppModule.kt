package co.farmpulse.app.di

import co.farmpulse.app.BuildConfig
import co.farmpulse.app.data.remote.api.RetrofitClient
import co.farmpulse.app.data.remote.api.TreeApiService
import co.farmpulse.app.data.remote.api.WeatherApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = RetrofitClient.create(BuildConfig.WEATHER_AI_KEY)

    @Provides
    @Singleton
    fun provideWeatherApiService(retrofit: Retrofit): WeatherApiService =
        retrofit.create(WeatherApiService::class.java)

    @Provides
    @Singleton
    fun provideTreeApiService(retrofit: Retrofit): TreeApiService =
        retrofit.create(TreeApiService::class.java)
}

