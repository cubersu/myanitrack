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
| 2 | Jikan entegrasyonu, detay sayfası, arama, top/sezonluk listeler | ✅ Tamamlandı |
| 3 | Yayın takvimi + geri sayım bildirimleri, haberler | ✅ Tamamlandı |
| 4 | Profil sayfaları, RSS arkadaş akışı, geçmiş | ✅ Tamamlandı |
| 5 | Forum ve mesajlaşma (WebView — Seçenek A) | ✅ Tamamlandı |
| 6 | Ayarlar cilası, deep link, animasyonlar, offline mod | ✅ Tamamlandı |

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
| Arka plan | WorkManager (yayın senkronizasyonu + bildirimler) |
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
:feature:browse         Top, sezonluk, tür, arama, öneriler  ← Faz 2
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
| **MAL RSS** (resmî) | Haber akışı (Faz 3), arkadaş akışı (Faz 4) | ❌ Salt okunur |
| **WebView** (Faz 5) | Forum, özel mesajlar, profil yorumları — MAL API vermiyor | Kullanıcı sitede |

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
- Birim testleri (filtre/sıralama, use case'ler, DTO mapper'ları, repository, ViewModel)

---

## Faz 2'de neler var

**Jikan v4 entegrasyonu (salt okunur)**
- İstemci tarafı hız sınırlama: **3 istek/sn ve 60 istek/dk**, kayan pencere ile.
  429 yiyip geri çekilmek yerine limiti hiç aşmıyoruz.
- 429/500/502/503/504 için üstel geri çekilmeli otomatik yeniden deneme (1s → 2s → 4s).
  Jikan'ın upstream'i (MAL) sık sık 504 döndürüyor; bu katman onu kullanıcıya göstermiyor.
- **cache-then-network:** Room'daki `remote_cache` tablosu. Taze kayıt varsa ağa hiç
  dokunulmaz; ağ hata verir ve elde bayat kayıt varsa **bayat kayıt döndürülür** —
  Jikan çöktüğünde ekran boşalmaz. TTL: detay 24s, karakter/staff 7g, listeler 6s, türler 30g.
- Bozuk önbellek kaydı uygulamayı çökertmez: silinir, ağdan tazelenir.

**Detay sayfası**
- Özet (genişletilebilir), tür/tema/demografi, tam bilgi tablosu (tip, durum, bölüm,
  süre, yayın tarihi, sezon, yayın zamanı, kaynak, yaş sınırı, stüdyo/yazar, üye, favori)
- Karakterler (ana karakterler önce, Japonca seslendirenle birlikte) ve ekip
- İlişkili yapımlar (Sequel/Prequel/Adaptation…) — dokununca o yapıma gider
- Öneriler, açılış/kapanış müzikleri
- İncelemeler — Paging 3 ile sayfalı, spoiler'lar dokunulana kadar gizli
- Tanıtım videoları — YouTube'a devrediliyor (gömülü WebView oynatıcı yerine)
- Listeye ekleme (durum seçerek) ve liste kaydını düzenleme/silme
- **Kısmi hata toleransı:** yalnızca ana detay çağrısının başarısızlığı ekranı hataya
  düşürür; karakter/öneri/video uçlarından biri 504 verirse sayfanın geri kalanı görünür.

**Keşfet sekmesi**
- **Top:** tüm zamanlar / yayında / yakında / en popüler / en çok favorilenen
  (manga tarafında "yayında" yerine "yayımlanıyor" — geçersiz filtre otomatik sıfırlanır)
- **Sezon:** sezon + yıl ileri/geri gezinme, mevcut sezondan başlar
- **Arama:** 400 ms debounce (Jikan limiti için kritik) + tür filtresi
- **Öneriler:** "Bunu beğendiysen bunu dene" ikilileri
- Tümü Paging 3 ile sonsuz kaydırma; yükleme/hata/boş durumları ayrı ayrı ele alınıyor

**Diğer**
- Listede: **dokun → detay**, **uzun bas → hızlı düzenleme**
- Düzenleme sayfası `:core:ui`'ya taşındı; liste ve detay ekranları aynı bileşeni kullanıyor
- Paging hataları `AppErrorException` ile domain hatasına sarılıyor — UI ham HTTP istisnası görmüyor
- Alt gezinme çubuğuna **Keşfet** sekmesi eklendi; detay ekranında çubuk gizleniyor

---

## Faz 3'te neler var

**Yayın takvimi**
- Haftalık takvim, gün sekmeleri; bugünün günü seçili açılır
- Her satırda **canlı geri sayım** (dakikada bir tazelenir) ve yayın saati
  kullanıcının kendi saat diliminde
- "Listem" filtresi — yalnızca izlemekte olduğun yapımlar
- Yayın saati bilinmeyen kayıtlar listenin sonuna düşer, gizlenmez
- Bir gün alınamazsa o gün boş kalır, takvimin geri kalanı görünür

**Sonraki bölüm hesabı** (`NextEpisodeCalculator`, saf fonksiyon)
- MAL yayın saatini yayıncı diliminde (genelde Asia/Tokyo) verir; hesap önce o
  dilimde yapılıp `Instant`'a çevrilir — yaz saati geçişleri ve tarih sınırı doğru
- Yayın anı tam şimdiyse bir sonraki haftaya geçer; geri sayım sıfırda takılmaz
- Eksik yayın bilgisi (MAL sık sık boş bırakıyor) çökme değil, "bilinmiyor" demek

**Bildirimler** (push sunucusu yok, tamamen cihaz içi)
- `AiringSyncWorker` 6 saatte bir çalışır: izlediğin yapımların takvimini alır,
  sonraki 6 saatte yayınlanacakları bulur, her biri için yayından **30 dk önce**
  tetiklenecek tek seferlik bir iş kuyruğa alır
- `AiringNotificationWorker` bildirimi gösterir; çalıştığı anda tercihi yeniden
  kontrol eder ve çok gecikmiş bildirimleri (cihaz kapalıydı vb.) atlar
- **Neden AlarmManager değil:** tam zamanlı alarm Android 12+ ayrı izin ister ve pil
  kısıtlarına takılır. WorkManager birkaç dakika sapma pahasına izinsiz ve sistem dostu.
- Bildirime dokunmak ilgili detay sayfasını açar (`myanitrack://anime/<id>`)
- Android 13+ bildirim izni çalışma zamanında isteniyor; reddedilirse uygulama
  normal çalışır, yalnızca hatırlatma gösterilmez
- Ayarlardan bildirimleri kapatmak arka plan işini de iptal eder

**Haberler**
- Kaynak: MAL'ın **resmî haber RSS'i** (`rss.php?type=news`). Jikan'ın yalnızca
  yapım bazlı haber ucu var (`/anime/{id}/news`), genel akış yok — resmî RSS hem
  doğru kaynak hem de hız sınırına takılmıyor.
- Bağımlılıksız RSS ayrıştırıcı (`javax.xml.parsers`) — hem Android hem düz JVM'de
  çalıştığı için Robolectric olmadan test edilebiliyor
- Bozuk XML ya da çözülemeyen tarih akışı bozmaz; makale MAL sitesinde açılır

---

## Faz 4'te neler var

**Profil sayfası** (dört sekme: Genel / Geçmiş / Arkadaşlar / Akış)
- Genel: avatar, konum, katılma ve son görülme tarihi; anime + manga istatistikleri
  (gün, ortalama puan, toplam kayıt, bölüm/cilt, tekrar sayısı) ve durum dağılım çubuğu
- Geçmiş: son ilerlemeler (`+2 bölüm` gibi), anime/manga filtresi, dokununca detay sayfası
- Arkadaşlar: son görülmeye göre sıralı; dokununca o kişinin profili açılır
- Akış: aşağıda
- Kendi profilin sekmeden, başkasının profili arkadaş listesinden veya akıştan açılır
- Ayarlar artık profil ekranının üst çubuğunda (alt çubuk 5 sekmeye indi)

**Kaynak seçimi — MAL API v2'nin sınırı**
- MAL API v2'nin `/users/{user_name}` ucu **yalnızca `@me` kabul ediyor**; başkasının
  profiline bakmanın tek yolu Jikan. Bu yüzden kendi profilimiz de Jikan'dan okunuyor —
  iki ayrı kod yolu tutmak yerine tek yol, daha az sürpriz.

**Arkadaş akışı (RSS)**
- Kaynak: MAL'ın resmî kullanıcı RSS'i (`rss.php?type=rw|rm&u=<kullanıcı>`) — orijinal
  MALClient'ın "friends feed" özelliği tam olarak bunu okuyordu
- Önce arkadaş listesi (Jikan), sonra her arkadaş için RSS; paralel çekiliyor
- **En son çevrimiçi 15 arkadaşla sınırlı**: 100+ arkadaşı olan hesapta ekran açılışı
  dakikalar sürerdi. Bir arkadaşın beslemesi alınamazsa o kişi atlanır, akış devam eder.
- Yapım kimliği ve türü RSS'te ayrı alan olarak yok; MAL bağlantısından ayrıştırılıyor.
  Beklenen biçime uymayan satır atlanır — besleme bozulursa akış tümden çökmez.
- Akış sekmesi **tembel yüklenir**: pahalı olduğu için yalnızca sekmeye gelindiğinde
  ve bir kez çekilir

**Kısmi hata toleransı**
- Profil, geçmiş ve arkadaşlar paralel çekilir; yalnızca profil çağrısının başarısızlığı
  ekranı hataya düşürür. Geçmiş ya da arkadaş listesi alınamazsa o sekme boş görünür,
  profil bilgileri yine gösterilir.

---

## Faz 5'te neler var

**Forum, özel mesajlar ve profil yorumları — WebView (Seçenek A)**
- `:feature:forum` → `myanimelist.net/forum/`
- `:feature:messaging` → `myanimelist.net/mymessages.php`
- Profil yorumları (okuma + yazma) → `comments.php?id=<malId>`, profil menüsünden
- Üçü de `:core:ui`'daki tek bir `MalWebViewScreen` bileşenini kullanıyor

**⚠️ Oturum devri neden yok — bunu bilerek okuyun**

Prompt'ta "login sonrası cookie'leri WebView'e aktar" deniyordu. Bu, **bizim
mimarimizde mümkün değil** ve bu bir eksiklik değil, bilinçli tercihin sonucu:

- Uygulama MAL'a **OAuth2 + PKCE** ile bağlanıyor. Elimizde `api.myanimelist.net`
  için geçerli bir **bearer token** var — `myanimelist.net` için bir **oturum çerezi
  yok**. MAL'ın API kimlik doğrulaması ile web oturumu ayrı sistemler; birini
  diğerine çevirmenin desteklenen bir yolu yok.
- Orijinal MALClient çerez tabanlı giriş (login formunu scrape ederek) yaptığı için
  çerezleri devredebiliyordu — ve çökme sebebi tam olarak bu kırılganlıktı.
- Sonuç: WebView kendi kalıcı çerez kavanozunu kullanıyor. Kullanıcı forum/mesajlar
  için MAL'a **bir kez de WebView içinde** giriş yapıyor; çerezler kalıcı olduğu için
  tekrarlanmıyor. Ekranın üstünde bunu açıklayan, kapatılabilir bir bilgi şeridi var.

**Neden bu modüller aslında kırılgan değil**
- Sayfayı biz ayrıştırmıyoruz; MAL HTML'ini değiştirdiğinde bu modüller bozulmaz.
  "Kırılgan modül" adı planla tutarlı olsun diye korundu, ama Seçenek A'nın tüm
  amacı kırılganlığı ortadan kaldırmaktı.
- Her iki modül de yalnızca bir URL biliyor; uygulamanın geri kalanıyla bağlantısı
  yok. Tümden kaldırılsalar derleme etkilenmez.

**WebView ayrıntıları**
- Kalıcı çerezler (`CookieManager`, sayfa sonunda ve ekran kapanışında `flush`)
- Cihazın geri hareketi önce WebView geçmişinde ilerler, yığın bitince ekrandan çıkar
- MAL ve alt alan adları içeride kalır, diğer siteler sistem tarayıcısında açılır —
  `notmyanimelist.net` gibi benzer alan adlarına karşı test edildi
- Koyu temada sayfa da kararıyor (`ALGORITHMIC_DARKENING`, destekleyen sürümlerde)
- Yerel dosya erişimi kapalı; yalnızca ana belge hatası hata ekranı gösteriyor

**Gezinme**
- Alt çubuk 5 sekmede dolu olduğu için Forum / Mesajlar / Profil yorumları / Ayarlar
  profil ekranının taşma menüsünde toplandı (hepsi hesap bağlamlı)

---

## Faz 6'da neler var

**Çevrimdışı düzenleme kuyruğu** — Faz 6'nın asıl işi
- Ağ yokken yapılan değişiklik artık **geri alınmıyor**: yerelde `pendingSync` ile
  işaretlenip korunuyor, bağlantı gelince WorkManager tarafından MAL'a gönderiliyor
- Silme de kuyruğa girebiliyor (`pendingDelete`): kayıt listeden hemen kayboluyor,
  gönderim sonra yapılıyor
- **Geçici / kalıcı hata ayrımı tek yerde** (`AppError.isTransient`):
  ağ yok / 5xx / 429 → kuyruğa al; 401 / 403 / 404 / biçim hatası → geri al + hata göster.
  Tekrar denemekle düzelmeyecek bir hatayı kuyrukta tutmak kuyruğu tıkardı.
- Satırın kendisi istenen son durum; ayrı "işlem günlüğü" yok. Aynı kayda arka arkaya
  yapılan değişiklikler kendiliğinden birleşiyor.
- Tam senkronizasyon (`replaceAll`) bekleyen yerel değişiklikleri **koruyor** — aksi
  halde arka plandaki bir tazeleme çevrimdışı yapılan düzenlemeyi sessizce silerdi
- Kalıcı hatada bayrak temizleniyor ki kuyruk sonsuza kadar dolu kalmasın

**Çevrimdışı göstergesi**
- `NetworkMonitor`: "bağlantı var" değil "internete çıkabiliyor" ölçüyor
  (`NET_CAPABILITY_VALIDATED`), böylece captive portal'a takılmış Wi-Fi çevrimdışı sayılıyor
- Liste ekranında şerit: "Çevrimdışı — değişiklikler bağlantı gelince gönderilecek"
  ve bekleyen değişiklik sayısı. Hata değil bilgi; liste zaten tamamen çalışıyor.

**Deep link tamamlandı**
- `myanitrack://anime/5114`, `myanitrack://manga/2/Berserk`, `myanitrack://profile/<kullanıcı>`
- Orijinal `malclient://<mal-link>` deseni: `myanitrack://myanimelist.net/anime/5114/X`
- `https://myanimelist.net/anime|manga|profile/...` — başka uygulamalardan paylaşılan
  MAL adresleri uygulamada açılabiliyor (`autoVerify` yok: alan adı bize ait değil,
  Android seçim penceresi gösteriyor)
- Ayrıştırma `android.net.Uri` yerine saf bir fonksiyonda → 22 test, benzer alan adları dahil

**Ayarlarda önbellek yönetimi**
- Önbellek boyutu gösteriliyor, tek dokunuşla temizleniyor
- **Kullanıcının listesi silinmiyor** — o önbellek değil, çevrimdışı çalışmanın temeli.
  Yalnızca Jikan yanıtları ve haber beslemesi gidiyor.

**Animasyonlar**
- İç ekranlar (detay, profil, WebView) yandan kayarak açılıyor, geri dönüşte tersi
- Sekmeler arası geçiş yalnızca soluklaşıyor: sekme değiştirmek hiyerarşide ilerlemek
  değil yer değiştirmek; yatay kayma yanlış bir derinlik hissi verirdi

---

## Bilinen sınırlar

- **Forum/mesajlar için MAL sitesine ayrı giriş gerekiyor** (yukarıdaki mimari not).
  Bu kalıcı bir sonuç.
- **Arkadaş akışı 15 arkadaşla sınırlı** (yukarıdaki gerekçe). Sınır tek sabitte,
  gerekirse artırılabilir.
- **Favoriler sekmesi yok.** Jikan `/users/{u}/favorites` ucu var ama profil zaten
  dört sekme; kapsam dışı bırakıldı.
- **Stüdyo bazlı gezinme yok.** Jikan `producers` parametresi servis katmanında
  hazır ama Keşfet ekranında yalnızca tür filtresi açık.
- **Çakışma çözümü "son yazan kazanır".** Aynı kaydı hem çevrimdışıyken burada hem
  başka bir istemcide değiştirirsen, kuyruk gönderildiğinde bizimki üste yazar.
  Alan bazlı birleştirme yapılmıyor.
- **Geri sayım MAL'ın yayın bilgisine güvenir.** MAL bir yapımın `broadcast` alanını
  boş bırakırsa geri sayım gösterilemez — uydurma tahmin yapılmıyor.
- **Bildirim zamanlaması ±birkaç dakika sapabilir** (WorkManager'ın doğası). Tam
  zamanlı alarm bilinçli olarak tercih edilmedi.
- **Manga incelemeleri Jikan'da anime kadar zengin değil**; bazı başlıklarda boş gelebilir.
- **Enstrümantasyon testi yok.** 229 birim testi var; Compose UI testleri ve Room
  migration testleri yazılmadı. Şema önbellek olduğu için `fallbackToDestructiveMigration`
  kullanılıyor, migration yazılmıyor.

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
