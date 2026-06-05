package co.farmpulse.app.di

import android.content.Context
import androidx.room.Room
import co.farmpulse.app.data.local.db.FarmPulseDatabase
import co.farmpulse.app.data.local.db.TreeAnalysisDao
import co.farmpulse.app.data.local.db.WeatherDao
import co.farmpulse.app.data.local.prefs.UserPreferencesRepository
import co.farmpulse.app.data.remote.api.RetrofitClient
import co.farmpulse.app.data.remote.api.TreeApiService
import co.farmpulse.app.data.remote.api.WeatherApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRetrofit(prefs: UserPreferencesRepository): Retrofit = RetrofitClient.create(prefs)

    @Provides
    @Singleton
    fun provideWeatherApiService(retrofit: Retrofit): WeatherApiService =
        retrofit.create(WeatherApiService::class.java)

    @Provides
    @Singleton
    fun provideTreeApiService(retrofit: Retrofit): TreeApiService =
        retrofit.create(TreeApiService::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FarmPulseDatabase {
        val db = Room.databaseBuilder(context, FarmPulseDatabase::class.java, "farmpulse_db")
            .fallbackToDestructiveMigration()
            .build()
        // Log DB path for debugging
        try {
            val path = context.getDatabasePath("farmpulse_db").absolutePath
            android.util.Log.i("AppModule", "Room DB path: $path")
        } catch (e: Exception) {
            android.util.Log.w("AppModule", "Could not read DB path", e)
        }
        return db
    }

    @Provides
    @Singleton
    fun provideWeatherDao(db: FarmPulseDatabase): WeatherDao = db.weatherDao()

    @Provides
    @Singleton
    fun provideTreeAnalysisDao(db: FarmPulseDatabase): TreeAnalysisDao = db.treeAnalysisDao()

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): co.farmpulse.app.util.NetworkMonitor =
        co.farmpulse.app.util.NetworkMonitor(context)
}
