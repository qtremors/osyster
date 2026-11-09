package dev.qtremors.osyster.core.runtime.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * The dispatchers data layers use, injected so tests can swap them. Data code never names
 * Dispatchers.IO directly (ArchitectureBoundaryTest checks this).
 */
data class AppDispatchers(
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
    val main: CoroutineDispatcher,
    /** A small shared pool so heavy scans (codecs, certificates, cameras) throttle each other. */
    val heavy: CoroutineDispatcher
) {
    companion object {
        /** The production set, shared by Hilt and by constructors that default it outside DI. */
        @OptIn(ExperimentalCoroutinesApi::class)
        val Standard: AppDispatchers by lazy {
            AppDispatchers(
                io = Dispatchers.IO,
                default = Dispatchers.Default,
                main = Dispatchers.Main,
                heavy = Dispatchers.IO.limitedParallelism(2)
            )
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @Singleton
    fun provideDispatchers(): AppDispatchers = AppDispatchers.Standard
}
