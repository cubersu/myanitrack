# MyAniTrack — Google Play yayın dosyası

Geliştirici: **TairyFail** · Destek: **tairyfailsupport@gmail.com**
Paket: `com.myanitrack` · Sürüm: `1.0.0` · versionCode: `1`

Bu klasör yayın hazırlığıdır. Play Console'a yükleme veya mağazada yayınlama yapılmamıştır. Google'ın onayı garanti edilemez.

## Hazırlananlar

- Türkçe/İngilizce gizlilik ve veri silme metni; giriş ekranından ve Ayarlar'dan erişim.
- Hakkında/credits, MyAnimeList ve Jikan kaynakları, MALClient esin kaynağı açıklaması.
- Release bağımlılık envanteri, Apache/MIT/BSD metinleri ve paketteki lisans bildirimleri.
- Yerel veri silme: Ayarlar > Çıkış yap, Android'in `clearApplicationUserData` işlemini çağırır. Onay metni eşitlenmemiş değişikliklerin kaybolacağını belirtir. MyAnimeList hesabı silinmez.
- Android yedekleme kapalı; açık HTTP trafiği kapalı; release günlükleri kapalı; R8 ve kaynak küçültme açık.
- Harici dosya/ortam değişkeni üzerinden upload-key imzalama yapılandırması. Anahtarlar ve parolalar Git dışında.
- İnternette yayınlanmaya hazır `site/index.html` gizlilik sayfası.

## Yayına çıkmadan tamamlanması gerekenler

1. **Play Console hesabı:** kimlik/iletişim doğrulaması, uygulama oluşturma, paket adının kullanılabilirliğini kontrol etme ve Play App Signing kurulumu. Hesabın veya mevcut upload anahtarının varlığı henüz doğrulanmadı.
2. **İmzalama:** `release-signing.properties.example` dosyasını `release-signing.properties` olarak kopyala, kendi upload anahtarının bilgilerini yerel olarak doldur. Mevcut yayın varsa o uygulamanın upload anahtarını kullan; yeni anahtarla üzerine yazma. İmzalama bilgileri verilmediğinde AAB imzasız oluşur ve Play'e yüklenemez.
3. **Gizlilik URL'si:** `site/index.html` ile aynı içerik `docs/index.html` altında GitHub Pages ile yayınlandı: **https://cubersu.github.io/myanitrack/**. Login, PDF, coğrafi engel veya erişim izni istemiyor. Bu URL'yi Play Console'a gir. İçeriği güncellersen her iki dosyayı da (`distribution/site/index.html` ve `docs/index.html`) birlikte güncelle, aksi halde ikisi birbirinden sapar.
4. **Politikayı sahiplenme:** Gizlilik metnindeki destek yazışmalarının 90 günlük saklama süresini gerçekten uygulamalısın; farklı bir uygulaman varsa metni değiştirmelisin. Hesap silme talebi MyAnimeList tarafından yönetilir. OAuth sayfasından harici hesap oluşturulabildiğinden Play'in hesap oluşturma/silme sorularını bu akışa göre yanıtla; sadece “ayrı backend yok” diye muafiyet varsayma.
5. **Kaynak ve içerik hakları:** MALClient GPL-3.0 lisanslıdır. Credits tek başına GPL yükümlülüklerini karşılamaz. Bu projeye MALClient kodu veya varlıkları kopyalanmış/uyarlanmışsa uygun kaynak kodu, lisans ve telif bildirimleriyle dağıtım gerekir. Kaynak kökeni geliştirici tarafından doğrulanmalıdır. Uygulamanın tamamına bu hazırlık sırasında bir lisans atanmadı. MyAnimeList API/web içeriği ve kapakların mağaza görsellerinde kullanım koşullarını ayrıca doğrula.
6. **İçerik ve topluluk:** Katalogdaki yetişkin içerik filtresi kapatılabiliyor. Bu haliyle cinsel içerik politikası incelemesi tamamlanmış sayılmaz. Forum/mesaj WebView'larında kullanıcı içeriği, raporlama/engelleme, üçüncü taraf reklamları ve çerezler bulunabilir. Yaş derecelendirmesi, hedef kitle, reklam beyanı ve UGC koşulları bu ekranlar incelenerek tamamlanmalı; “reklam SDK'sı yok” otomatik olarak “reklam yok” anlamına gelmez.
7. **Uygulama erişimi:** İnceleme ekibinin giriş gerektiren ekranlara erişebilmesi için Play Console App access alanını doldur. Gerekirse kişisel hesabından ayrı bir test MAL hesabı hazırla; şifresini kaynak dosyalara koyma.
8. **Test:** Uygun yeni kişisel geliştirici hesaplarında 12 test kullanıcısının kesintisiz en az 14 gün katıldığı kapalı test gerekir. Hesabının bu kapsama girip girmediğini Console'da kontrol et. Pre-launch report, farklı Android sürümleri, ekranlar, çevrimdışı eşitleme ve hesap değişimini test et.
9. **16 KB sayfa boyutu:** Release'te bulunan native kütüphaneler için Play Console'un ve Android'in 16 KB uyumluluk kontrollerini tamamla. Sadece targetSdk'nin yüksek olması yeterli değildir.

## Üretim derlemesi

```powershell
.\gradlew.bat :app:exportReleaseDependencies --no-configuration-cache
powershell -NoProfile -File scripts/Collect-Licenses.ps1
.\gradlew.bat :app:bundleRelease :app:lintRelease testDebugUnitTest
```

AAB: `app/build/outputs/bundle/release/app-release.aab`
R8 eşleme: `app/build/outputs/mapping/release/mapping.txt`
Lint: `app/build/reports/lint-results-release.html`
Bağımlılık listesi: `app/build/reports/release-dependencies.tsv`

Her yeni yüklemede versionCode artırılmalıdır. İmzalama dosyasını ve anahtarını güvenli bir yedekte tut. Parolaları sohbet, Git veya mağaza açıklamasına koyma.

## Data safety — kod incelemesine dayalı doldurma taslağı

Bu tablo gönderilmiş bir beyan değildir. Son release ve gömülü MyAnimeList sayfaları üzerinden doğrulanmalıdır.

| Veri | Akış / amaç |
|---|---|
| Kullanıcı kimliği ve profil | MAL OAuth, profil görüntüleme ve hesap yönetimi; cihazda ve MyAnimeList'te |
| İzleme/okuma ilerlemesi, puan, not, etiket | Kullanıcının yaptığı liste değişiklikleri HTTPS ile MyAnimeList'e; çevrimdışı kopya cihazda |
| Aramalar ve seçilen yapımlar | İçerik isteği sırasında MyAnimeList/Jikan'a iletilir |
| Özel mesajlar ve forum paylaşımları | Kullanıcının gömülü MyAnimeList sitesinde gönderdiği içerik; MyAnimeList tarafından işlenir |
| Çerezler, IP ve web sayfası etkileşimleri | MyAnimeList WebView ve üçüncü taraf kaynaklar; hizmetlerin kendi politikaları geçerli |
| Destek e-postası | Kullanıcı doğrudan destek adresine gönderirse TairyFail tarafından işlenir |

Geliştirici backend'i, uygulama analiz/çökme raporu/reklam SDK'sı yok. Bu, üçüncü taraflara hiç veri gitmediği anlamına gelmez. Formda collection/sharing, user-initiated transfer ve service-provider istisnalarını Google'ın tanımlarına göre değerlendir. Şifre “uygulama tarafından toplanmıyor”; OAuth token'ları hesap erişimi için cihazda şifreli saklanır.

## Resmi başvuru kaynakları (9 Eylül 2026)

- [Hedef API gerekliliği: API 36 veya üstü; uygulama API 37](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Gizlilik ve kullanıcı verileri](https://support.google.com/googleplay/android-developer/answer/10144311)
- [Hesap silme kapsamı](https://support.google.com/googleplay/android-developer/answer/13327111)
- [Yeni kişisel hesaplarda test](https://support.google.com/googleplay/android-developer/answer/14151465)
- [16 KB sayfa boyutu](https://developer.android.com/guide/practices/page-sizes)
- [MALClient kaynağı ve GPL-3.0](https://github.com/Drutol/MALClient)

