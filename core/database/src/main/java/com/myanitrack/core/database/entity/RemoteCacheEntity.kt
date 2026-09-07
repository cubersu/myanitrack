package com.myanitrack.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Jikan yanitlari icin genel amacli, anahtar-deger onbellegi.
 *
 * Jikan salt okunur ve zengin sekilde ic ice gecmis veri donduruyor (detay,
 * karakter, review, top listeler...). Her uc nokta icin ayri tablo ve entity
 * yazmak yerine, seri hale getirilmis govdeyi anahtarla sakliyoruz. Neden:
 *
 * - Jikan yanitlarini biz sorgulamiyoruz, sadece bir butun olarak okuyoruz;
 *   iliskisel sema hicbir sorgu kazanci saglamazdi.
 * - Jikan zaten 24 saat kendi onbellegini tutuyor; bizim amacimiz tekrar
 *   sorgulamayi en aza indirmek (3/sn - 60/dk limiti), veri modellemek degil.
 * - Yeni bir uc nokta eklemek sema degisikligi gerektirmiyor.
 *
 * Kullanicinin kendi listesi bunun disindadir; o [MediaListEntryEntity] icinde
 * duzgun sekilde iliskisel olarak tutulur cunku uzerinde filtreleme/siralama yapiyoruz.
 */
@Entity(tableName = "remote_cache")
data class RemoteCacheEntity(
    @PrimaryKey val cacheKey: String,
    val payload: String,
    val fetchedAtEpochSeconds: Long,
)
