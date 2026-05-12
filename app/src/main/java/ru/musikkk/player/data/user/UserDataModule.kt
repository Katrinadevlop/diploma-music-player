package ru.musikkk.player.data.user

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit
import ru.musikkk.player.core.network.api.UserDataApi
import ru.musikkk.player.core.network.di.AuthRetrofit

@Module
@InstallIn(SingletonComponent::class)
object UserDataModule {

    @Provides
    @Singleton
    fun provideUserDataApi(@AuthRetrofit retrofit: Retrofit): UserDataApi =
        retrofit.create(UserDataApi::class.java)
}
