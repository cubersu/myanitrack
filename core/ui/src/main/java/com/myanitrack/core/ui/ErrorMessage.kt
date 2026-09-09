package com.myanitrack.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.myanitrack.core.common.result.AppError

/**
 * Domain hatasini kullaniciya gosterilecek metne cevirir.
 *
 * Tum ekranlar ayni esleme uzerinden gectigi icin hata mesajlari tutarli olur;
 * yeni bir hata turu eklendiginde `when` derleyici tarafindan zorlanir.
 */
fun AppError.toUserMessage(context: Context): String = when (this) {
    is AppError.Network -> context.getString(R.string.error_network)
    AppError.Unauthorized -> context.getString(R.string.error_unauthorized)
    AppError.Forbidden -> context.getString(R.string.error_forbidden)
    AppError.NotFound -> context.getString(R.string.error_not_found)
    is AppError.RateLimited -> retryAfterSeconds?.let {
        context.getString(R.string.error_rate_limited_seconds, it.toInt())
    } ?: context.getString(R.string.error_rate_limited)
    // MAL, Jikan and RSS share this mapping; do not blame a specific provider.
    is AppError.Server -> context.getString(R.string.error_server)
    is AppError.Http -> context.getString(R.string.error_http, code)
    is AppError.Serialization -> context.getString(R.string.error_serialization)
    is AppError.Storage -> context.getString(R.string.error_storage)
    is AppError.FeatureUnavailable -> context.getString(R.string.error_feature_unavailable)
    is AppError.Unknown -> context.getString(R.string.error_unknown)
}

@Composable
fun AppError.toUserMessage(): String = toUserMessage(LocalContext.current)
