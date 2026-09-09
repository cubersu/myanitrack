# Yayın hazırlığı doğrulaması

Tarih: 9 Eylül 2026

- Release AAB ve küçültülmüş release APK: başarıyla derlendi.
- Birim testleri: 263 test, 0 başarısızlık, 0 hata.
- `:app:lintRelease`: 0 hata, 10 uyarı. Uyarılar çoğunlukla yeni bağımlılık sürümleri ve Android sürümlerine göre kullanılan özniteliklerle ilgili; lint raporu ayrıntıları korur.
- Release native kütüphaneleri: 8 `.so`, tüm LOAD segmentlerinde en az 16 KB hizalama.
- `zipalign -c -P 16 4`: başarılı. Bu statik kontroller gerçek 16 KB cihaz/emülatör testinin yerine geçmez.
- Release bağımlılık envanteri: 230 bileşen; POM/upstream lisans metadata'sı çözülemeyen bileşen yok. Gömülü NOTICE/LICENSE dosyaları ve ortak tam lisans metinleri dahil edildi. Bu kontrol projenin MALClient kaynak kökenini veya içerik kullanım iznini doğrulamaz.
- AAB imzası: **yok**. Upload anahtarı yapılandırılmadığından Google Play'e henüz yüklenemez.
- AAB SHA-256: `4E75BF8B7E5F14BC996A8FFC2EC67A2B4F5D6725F249EB0F16190806A8D2F983`.
- Gizlilik/credits ekranlarının son sürümü gerçek cihazda doğrulanamadı: tablet bağlantısı kesildi. Kullanıcının gerçek hesabında veri silme işlemi test amacıyla çalıştırılmadı.
- Mağaza simgesi: `store-assets/icon-512.png` (512×512).
- Tanıtım görseli: `store-assets/feature-1024x500.png` (1024×500).
- Mağaza ekran görüntüleri henüz hazırlanmadı; son sürümden gerçek telefon/tablet ekran görüntüleri alınmalı.

Google Play incelemesine gönderim veya web sitesi yayınlama yapılmadı. Yayından önceki kalan adımlar için `PLAY-RELEASE.md` dosyasını takip edin.
