package com.myanitrack.core.network.util

import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/**
 * Retrofit cagrilarini domain seviyesine tasiyan tek sarmalayici.
 *
 * Buradan sonra hicbir katman ham istisna gormez; her sey [AppError] olur.
 * Coroutine iptali ([CancellationException]) hata degildir, yeniden firlatilir.
 */
suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    AppResult.Failure(throwable.toAppError())
}

fun Throwable.toAppError(): AppError = when (this) {
    is HttpException -> toAppError()
    is SocketTimeoutException -> AppError.Network(this)
    is UnknownHostException -> AppError.Network(this)
    is IOException -> AppError.Network(this)
    is SerializationException -> AppError.Serialization(message)
    else -> AppError.Unknown(this)
}

fun HttpException.toAppError(): AppError = when (val statusCode = code()) {
    401 -> AppError.Unauthorized
    403 -> AppError.Forbidden
    404 -> AppError.NotFound
    429 -> AppError.RateLimited(retryAfterSeconds())
    in 500..599 -> AppError.Server(statusCode)
    else -> AppError.Http(statusCode, runCatching { response()?.errorBody()?.string() }.getOrNull())
}

private fun HttpException.retryAfterSeconds(): Long? =
    response()?.headers()?.get("Retry-After")?.toLongOrNull()
