# Google Play "Data safety" formu taslağı

Kodu incelenerek hazırlandı (26 Kasım 2026). Play Console → Uygulama içeriği → Veri güvenliği. Veri toplama/işleme
ayrımında: tüm veriler Firebase (Google) sunucularına gider, Google "service provider" olduğu için **paylaşım (shared) sayılmaz**; hepsi "toplanır (collected)".

## Genel sorular
| Soru | Cevap |
|---|---|
| Uygulama kullanıcı verisi topluyor/paylaşıyor mu? | Evet, topluyor |
| Veriler aktarımda şifreleniyor mu? | Evet (HTTPS/TLS) |
| Kullanıcı verisinin silinmesini isteyebilir mi? | Evet. Uygulama içi (Biz → Hesap → Hesabı sil) + web: `https://lovelyy-native.web.app/hesap-silme.html` |
| Gizlilik politikası URL'si | `https://lovelyy-native.web.app/gizlilik.html` |
| Reklam var mı? | Hayır (reklam kimliği toplanmıyor) |

## Veri türleri
Hepsi: **Toplanır = Evet, Paylaşılır = Hayır**, işlenme "zorunlu değil" yazılacaklar belirtildi.

| Kategori / tür | Toplanır | Amaç | Not |
|---|---|---|---|
| Kişisel bilgi → E-posta adresi | Evet | Uygulama işlevi, Hesap yönetimi | Giriş için zorunlu |
| Kişisel bilgi → Ad | Evet | Uygulama işlevi | Görünen ad; partner görür |
| Kişisel bilgi → Kullanıcı kimlikleri | Evet | Uygulama işlevi, Hesap yönetimi | Firebase uid, eşleşme kodu, çift kimliği |
| Konum → Yaklaşık konum | Evet | Uygulama işlevi | **İsteğe bağlı** (kullanıcı açar). Şehir + ~10 km'ye yuvarlanmış koordinat; sadece ön planda |
| Mesajlar → Diğer uygulama içi mesajlar | Evet | Uygulama işlevi | Notlar, mektuplar, günün sorusu cevapları, ruh hali notları |
| Fotoğraf ve videolar | Hayır | | Fotoğraf yüklenmiyor. Google profil foto bağlantısı yalnızca kullanıcı bilgisi olarak saklanır |
| Uygulama etkinliği → Uygulama içi eylemler | Evet | Analiz | Analytics olayları, kimliksiz |
| Uygulama etkinliği → Diğer kullanıcı tarafından oluşturulan içerik | Evet | Uygulama işlevi | Çizimler, hayaller listesi, karar çarkı, takvim, özel günler |
| Uygulama bilgileri ve performansı → Çökme günlükleri | Evet | Analiz | Crashlytics |
| Uygulama bilgileri ve performansı → Tanılama | Evet | Analiz | Performance Monitoring |
| Cihaz veya diğer kimlikler | Evet* | Analiz | *Firebase yükleme kimliği (installation ID/app-instance ID); reklam kimliği toplanmıyor. Play'in yönlendirmesine göre işaretle |
| Finansal, sağlık, kişiler, takvim (cihaz), arama, dosyalar, web geçmişi | Hayır | | "Takvim" = uygulama içi planlar, cihaz takvimi okunmaz |

## Güvenlik uygulamaları
- Veriler aktarımda şifrelenir: Evet
- Silme talebi mekanizması: Evet (yukarıdaki URL)
- Bağımsız güvenlik incelemesi: Hayır
- Play Aile Politikası: hedef kitle 13+ (çocuklara yönelik değil)

## Dikkat / açık noktalar (Ulas'a)
1. **Telemetri varsayılan AÇIK** (release) ve uygulamada kapatma düğmesi yok (`TelemetryConsent` hazır, arayüzü yok). AB/KVKK açısından riskli olabilir; Biz → Ayarlar'a bir "Kullanım verisi" anahtarı eklemek iyi olur. Politika şu an bunu "meşru menfaat" olarak yazıyor.
2. Cloud Firestore kuralları henüz yayınlanmadıysa "yalnızca siz ve partner görür" ifadesi kurallar yayınlanana kadar doğru olmaz. Yayınlanmalı.
3. Bildirimler şu an uygulama içi/yerel; FCM (sunucu push) eklenirse cihaz belirteci ("Cihaz kimliği") ve politika güncellenmeli.
4. Politikadaki iletişim e-postası `web/CONTACT_EMAIL` ile belirlenir.
