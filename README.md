# MyAniTrack

MyAnimeList için modern, native bir Android istemcisi — Google Play'den kaldırılan
[MALClient](https://github.com/Drutol/MALClient) (Xamarin/C#, UWP+Android) uygulamasının
Kotlin + Jetpack Compose ile yeniden yapımı.

Orijinal uygulama MAL'ın cookie tabanlı web scraping'ine bel bağlıyordu. Bu proje
mümkün olan her yerde **resmî ve stabil API'leri** kullanır; scraping gerektiren
özellikler izole, kırılgan modüllere hapsedilir.

---

## Durum

| Faz | Kapsam | Durum |
|-----|--------|-------|
| 1 | Proje iskeleti, MAL OAuth2 girişi, liste CRUD, liste görünümleri | ✅ Tamamlandı |
| 2 | Jikan entegrasyonu, detay sayfası, arama, top/sezonluk listeler | ⬜ Sırada |
| 3 | Yayın takvimi + geri sayım bildirimleri, haberler | ⬜ |
| 4 | Profil sayfaları, RSS arkadaş akışı, geçmiş | ⬜ |
| 5 | Forum ve mesajlaşma (WebView — Seçenek A) | ⬜ |
| 6 | Ayarlar cilası, deep link, animasyonlar, offline mod | ⬜ |

---

## Kurulum

### 1. MAL Client ID al

1. <https://myanimelist.net/apiconfig> adresine git, **Create ID**.
2. **App Type:** `other`
3. **App Redirect URL:** `myanitrack://auth`
4. Oluşan **Client ID**'yi kopyala.

### 2. `local.properties` içine ekle

```properties
MAL_CLIENT_ID=buraya_client_id
```

Bu dosya `.gitignore`'da; Client ID **hiçbir zaman** kaynak koda gömülmez.
`:core:network` modülü onu `BuildConfig.MAL_CLIENT_ID` olarak okur.
CI için `MAL_CLIENT_ID` ortam değişkeni de desteklenir.

### 3. Derle

```bash
./gradlew assembleDebug        # APK
./gradlew testDebugUnitTest    # birim testleri
```

Client ID tanımlı değilse uygulama derlenir ve açılır; giriş ekranı ne yapılması
gerektiğini açıkça söyler.

---

## Teknoloji yığını

| Katman | Seçim |
|--------|-------|
| Dil | Kotlin 2.4, Coroutines + Flow |
| UI | Jetpack Compose + Material 3 (dinamik renk, açık/koyu) |
| Mimari | Clean Architecture (data / domain / presentation) + MVVM |
| DI | Hilt 2.60 |
| Ağ | Retrofit 3 + OkHttp 5 + kotlinx.serialization |
| Veritabanı | Room 2.8 |
| Tercihler | DataStore (Preferences + Keystore ile şifreli token deposu) |
| Görsel | Coil 3 |
| Navigasyon | Navigation-Compose (type-safe routes) |
| Arka plan | WorkManager (Faz 3'ten itibaren) |
| Test | JUnit 5 + MockK + Turbine |
| Build | AGP 9.2.1 (built-in Kotlin), Gradle 9.4.1, JDK 21, minSdk 26 / targetSdk 37 |

---

## Modül yapısı

```
:app                    Application, MainActivity, navigasyon, tema kabuğu
:core:common            AppResult / AppError, dispatcher qualifier'ları, OAuth redirect bus
:core:model             Saf domain modelleri (Android bağımlılığı yok)
:core:domain            Repository arayüzleri, use case'ler, filtre/sıralama kuralları
:core:data              Repository implementasyonları (network + database + datastore)
:core:network           MAL API v2, OAuth2 PKCE, interceptor/authenticator, DTO + mapper
:core:database          Room: entity, DAO, tip dönüştürücüler
:core:datastore         DataStore, Keystore/AES-GCM şifreli oturum deposu
:core:designsystem      Tema, renk paleti, tipografi, ortak bileşenler
:core:ui                Ekranlar arası ortak UI (hata mesajı eşlemesi, olay yardımcıları)

:feature:auth           MAL OAuth2 girişi (Custom Tabs)
:feature:mylist         Anime/manga liste yönetimi          ← Faz 1
:feature:settings       Tema, liste tercihleri, çıkış        ← Faz 1 (temel)
:feature:details        Anime/manga detay sayfası            ← Faz 2
:feature:browse         Top, sezonluk, tür/stüdyo, arama     ← Faz 2
:feature:calendar       Yayın takvimi + geri sayım           ← Faz 3
:feature:news           MAL haberleri                        ← Faz 3
:feature:profile        Profil, arkadaş akışı, geçmiş        ← Faz 4
:feature:forum          Forum (WebView)                      ← Faz 5
:feature:messaging      Özel mesajlar (WebView)              ← Faz 5
```

> Prompt'taki listeye ek olarak `:core:domain` ve `:core:data` eklendi. Clean
> Architecture'ın domain katmanının somut bir yeri olması için: repository
> *arayüzleri* domain'de, *implementasyonları* data'da yaşıyor.

---

## Veri kaynağı stratejisi

| Kaynak | Ne için | Yazma? |
|--------|---------|--------|
| **MAL API v2** (OAuth2 + PKCE) | Giriş, liste CRUD, profil temel bilgileri, arama | ✅ Tüm yazma işlemleri **yalnızca** buradan |
| **Jikan v4** (Faz 2) | Top/sezonluk listeler, karakter & staff, review, öneri, haber, takvim | ❌ Salt okunur |
| **MAL RSS** (Faz 4) | Arkadaş akışı / liste güncellemeleri | ❌ Salt okunur |
| **WebView** (Faz 5) | Forum ve özel mesajlar — MAL'ın API'si yok | Kullanıcı sitede |

Forum/mesajlaşma için **Seçenek A (WebView)** seçildi: MAL'ın HTML yapısı değişse
bile uygulama bozulmaz. Native scraping (Seçenek B) istenirse ayrı bir dalda,
aynı repository arayüzünün arkasında denenebilir.

---

## Faz 1'de neler var

**Kimlik doğrulama**
- MAL OAuth2 + PKCE (`code_challenge_method=plain` — MAL yalnızca bunu destekler)
- Custom Tabs ile tarayıcıda yetkilendirme, `myanitrack://auth` ile geri dönüş
- CSRF'e karşı `state` doğrulaması
- Token'lar AndroidKeyStore'da üretilen AES/GCM anahtarıyla şifrelenip DataStore'a yazılır
- Süresi dolmadan 5 dk önce sessiz yenileme; 401'de tek seferlik zorunlu yenileme
  (`Mutex` ile eşzamanlı isteklerde tek yenileme garantisi)
- Çıkışta token + Keystore anahtarı silinir

**Liste yönetimi**
- Anime ve manga listeleri, durum sekmeleri (sayaçlarla) + "Tümü"
- Üç görünüm: **Izgara**, **Kompakt**, **Detaylı** (orijinal MALClient'taki gibi)
- Sıralama: başlık, puanım, ilerleme, son güncelleme, yayın tarihi, MAL puanı, popülerlik
  — aynı ölçüte tekrar basınca yön tersine döner, tercih kalıcı
- Liste içi arama, etiket filtresi, NSFW gizleme
- Satır içi **+1** düğmesi: son bölümde otomatik "Completed" + bitiş tarihi,
  "Plan to watch"tan ilk bölümde otomatik "Watching" + başlangıç tarihi
- Tam düzenleme sayfası: durum, puan (1–10, MAL etiketleriyle), bölüm/cilt sayacı,
  tekrar izleme/okuma + sayaç, başlangıç/bitiş tarihi (tarih seçici, temizleme dahil),
  etiketler, notlar, kayıt silme
- **İyimser güncelleme:** değişiklik önce yerelde uygulanır, istek başarısız olursa geri alınır
- Aşağı çekerek senkronizasyon; `paging.next` takibiyle tüm sayfalar toplanır

**Çevrimdışı**
- Liste tümüyle Room'da; ekran her zaman yerelden okur, ağ yalnızca tazeler
- Ağ yokken önbellekteki liste görünür; tazeleme hatası önbellek varken kullanıcıyı rahatsız etmez

**Hata yönetimi**
- Her ağ hatası repository sınırında `AppError`'a çevrilir
  (`Network`, `Unauthorized`, `Forbidden`, `NotFound`, `RateLimited`, `Server`,
  `Http`, `Serialization`, `Storage`, `FeatureUnavailable`, `Unknown`)
- UI hiçbir zaman ham istisna görmez; tek bir eşleme tablosundan mesaj üretilir

**Diğer**
- Material 3 dinamik renk (Android 12+), açık/koyu/sistem teması
- Arayüz metinleri İngilizce, Türkçe çeviri (`values-tr`) dahil
- 50 birim testi (filtre/sıralama, use case'ler, DTO mapper'ları, repository, ViewModel)

---

## Bilinen sınırlar

- **Detay sayfası henüz yok.** Listede bir kayda dokunmak düzenleme sayfasını açar;
  tür/karakter/review gibi zengin veri Faz 2'de Jikan ile gelecek.
- **Alt gezinme çubuğunda şimdilik 2 sekme var** (Listem, Ayarlar). Keşfet/Takvim/
  Haberler/Profil sekmeleri ilgili fazlarda eklenecek.
- **Çevrimdışı düzenleme kuyruğu yok.** Ağ yokken yapılan değişiklik geri alınır ve
  hata gösterilir. `pendingSync` alanı ve DAO sorgusu bu iş için hazır bekliyor (Faz 6).
- **Deep link kısmen hazır.** `myanitrack://` şeması kayıtlı ve OAuth dönüşü çalışıyor;
  `myanitrack://anime/<id>` gibi içerik bağlantıları detay sayfasıyla birlikte gelecek.
- **Bildirim izni manifest'te tanımlı** ama çalışma zamanı isteği Faz 3'te eklenecek.

---

## Geliştirme notu: `gradle.properties` kodlaması

`org.gradle.jvmargs` içindeki `-Dfile.encoding=windows-1252` **bilinçli** ve
kaldırılmamalı. Bu makinede kullanıcı dizini ASCII dışı karakter içeriyor
(`C:\Users\Ömer`). JDK'nın `sun.jnu.encoding` değeri `Cp1252`; Gradle ise test
worker'ının classpath argfile'ını daemon'un `file.encoding` değeriyle yazıyor.
UTF-8 yazılan dosyayı `java` launcher'ı Cp1252 olarak okuyunca yol bozuluyor ve
tüm testler `Could not find or load main class ...GradleWorkerMain` ile çöküyor.
Daemon'u Cp1252 ile çalıştırmak iki tarafı hizalıyor. Kaynak dosyalar yine
UTF-8 okunuyor (her modülde `compileOptions.encoding = "UTF-8"`).

Kalıcı alternatif: `GRADLE_USER_HOME`'u ASCII bir yola taşımak veya Windows'un
"Beta: Use Unicode UTF-8" seçeneğini açmak.

---

## Lisans / teşekkür

Orijinal fikir ve özellik seti [Drutol/MALClient](https://github.com/Drutol/MALClient)
projesine aittir. Bu depo kişisel kullanım için yapılmış bağımsız bir yeniden yazımdır.
MyAnimeList adı ve verileri MyAnimeList Co., Ltd.'ye aittir.
