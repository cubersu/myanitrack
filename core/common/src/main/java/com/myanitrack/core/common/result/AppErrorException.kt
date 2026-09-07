package com.myanitrack.core.common.result

/**
 * [AppError]-u istisna olarak tasimak zorunda kaldigimiz yerler icin sarmalayici.
 *
 * Paging 3 hatalari `LoadState.Error(Throwable)` olarak tasidigi ve [AppResult]
 * kabul etmedigi icin, PagingSource icinde hatayi burada sariyoruz. Boylece hata
 * eslemesi yine veri katmaninda kaliyor ve UI katmani ham HTTP/IO istisnasi
 * yorumlamak zorunda kalmiyor.
 */
class AppErrorException(val error: AppError) : Exception(error.toString())

/** Paging-den gelen [Throwable]-i domain hatasina cevirir. */
fun Throwable.asAppError(): AppError = when (this) {
    is AppErrorException -> error
    else -> AppError.Unknown(this)
}
