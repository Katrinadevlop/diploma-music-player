package ru.musikkk.player.core.media.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.musikkk.player.core.media.AndroidNetworkTypeProvider
import ru.musikkk.player.core.media.NetworkTypeProvider

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaModule {

    @Binds
    @Singleton
    abstract fun bindNetworkTypeProvider(impl: AndroidNetworkTypeProvider): NetworkTypeProvider
}
