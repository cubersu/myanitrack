package com.myanitrack.core.data.cache

import com.myanitrack.core.common.di.AppDispatcher
import com.myanitrack.core.common.di.Dispatcher
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.repository.CacheRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class CacheRepositoryImpl @Inject constructor(
    private val remoteCache: RemoteCache,
    @Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : CacheRepository {

    override suspend fun approximateSizeBytes(): Long =
        withContext(ioDispatcher) { remoteCache.approximateSizeBytes() }

    override suspend fun clear(): AppResult<Unit> =
        withContext(ioDispatcher) { remoteCache.clear() }
}
