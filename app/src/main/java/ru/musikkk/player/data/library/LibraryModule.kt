package ru.musikkk.player.data.library

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit
import ru.musikkk.player.core.network.api.LibraryApi
import ru.musikkk.player.core.network.di.AuthRetrofit

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryModule {

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    companion object {
        @Provides
        @Singleton
        fun provideLibraryApi(@AuthRetrofit retrofit: Retrofit): LibraryApi =
            retrofit.create(LibraryApi::class.java)
    }
}
