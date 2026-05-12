package ru.musikkk.player.data.me

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MeModule {
    @Binds
    @Singleton
    abstract fun bindMeRepository(impl: MeRepositoryImpl): MeRepository
}
