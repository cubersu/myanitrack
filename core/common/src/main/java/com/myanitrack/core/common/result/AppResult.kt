package com.myanitrack.core.common.result

/**
 * Basari/hata tasiyan minimal sonuc turu.
 *
 * Kotlin-in kendi [Result] turu yerine bunu kullaniyoruz cunku hata tarafinda
 * ham [Throwable] degil, domain seviyesinde anlamli bir [AppError] tutuyoruz.
 */
sealed interface AppResult<out T> {

    data class Success<out T>(val data: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T, R> AppResult<T>.flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
    is AppResult.Success -> transform(data)
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> = apply {
    if (this is AppResult.Success) action(data)
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> = apply {
    if (this is AppResult.Failure) action(error)
}

fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.data

fun <T> AppResult<T>.errorOrNull(): AppError? = (this as? AppResult.Failure)?.error

fun <T> AppResult<T>.getOrDefault(fallback: T): T = getOrNull() ?: fallback

val AppResult<*>.isSuccess: Boolean get() = this is AppResult.Success

fun <T> T.asSuccess(): AppResult<T> = AppResult.Success(this)

fun AppError.asFailure(): AppResult<Nothing> = AppResult.Failure(this)
