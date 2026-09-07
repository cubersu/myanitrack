package com.myanitrack.core.common.result

/**
 * Domain seviyesindeki hata turleri.
 *
 * Ag katmanindaki her istisna (IOException, HttpException, SerializationException...)
 * repository sinirinda bu turlerden birine cevrilir; UI katmani hicbir zaman ham
 * istisna gormez. Boylece her ekran ayni sekilde anlamli mesaj gosterebilir.
 */
sealed interface AppError {

    /** Ag yok, DNS hatasi ya da zaman asimi. */
    data class Network(val cause: Throwable? = null) : AppError

    /** 401 - token gecersiz veya suresi dolmus ve yenilenemedi. Yeniden giris gerekir. */
    data object Unauthorized : AppError

    /** 403 - istek kimlik dogrulandi ama yetki yok. */
    data object Forbidden : AppError

    /** 404 - kaynak bulunamadi. */
    data object NotFound : AppError

    /** 429 - istek limiti asildi (ozellikle Jikan: 3 istek/sn, 60 istek/dk). */
    data class RateLimited(val retryAfterSeconds: Long? = null) : AppError

    /** 5xx - sunucu tarafi hata. */
    data class Server(val code: Int) : AppError

    /** Yukaridakilere girmeyen HTTP hatalari. */
    data class Http(val code: Int, val body: String? = null) : AppError

    /** Beklenmeyen govde formati / JSON cozumleme hatasi. */
    data class Serialization(val message: String? = null) : AppError

    /** Yerel veritabani hatasi. */
    data class Storage(val cause: Throwable? = null) : AppError

    /**
     * Kirilgan modullerde (forum, ozel mesajlasma, profil yorumlari) MAL tarafi
     * degistigi icin ozellik su an calismiyor. Kullaniciya net mesaj gosterilir,
     * uygulamanin geri kalani etkilenmez.
     */
    data class FeatureUnavailable(val feature: String, val cause: Throwable? = null) : AppError

    /** Siniflandirilamayan hata. */
    data class Unknown(val cause: Throwable? = null) : AppError
}
