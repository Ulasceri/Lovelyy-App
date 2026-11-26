# Lovelyy web sitesi (Firebase Hosting)

Google Play için gerekli herkese açık sayfalar: gizlilik politikası, kullanım şartları, hesap silme sayfası (Türkçe + İngilizce).
Adres: `https://lovelyy-native.web.app` (ücretsiz, Spark planında çalışır).

| Sayfa | Türkçe | English |
|---|---|---|
| Gizlilik | `/gizlilik.html` | `/en/privacy.html` |
| Şartlar | `/kullanim-sartlari.html` | `/en/terms.html` |
| Hesap silme (Play "hesap silme URL'si") | `/hesap-silme.html` | `/en/delete-account.html` |

## Yapı
- `pages/` metinler (düzenlenecek yer). `public/` bunlardan üretilir ve repoda durur, böylece yayınlamak için Python gerekmez.
- `CONTACT_EMAIL` tüm sayfalardaki iletişim adresi. **Yayınlamadan önce gerçek adresle değiştir.**
- Değişiklikten sonra: `python3 web/tools/build.py`, sonra yayınla.
- Kök dizindeki `firebase.json` sadece Hosting içindir; kurallar `firebase/` klasöründe ayrıdır.

## Yayınlama (Windows, bir kez kurulum)
1. Node.js LTS kur: https://nodejs.org
2. PowerShell'de repo klasörüne gir ve çalıştır:
   ```
   npx firebase-tools login
   npx firebase-tools deploy --only hosting --project lovelyy-native
   ```
   (`login` tarayıcı açar; `lovelyy-native` projesinin sahibi Google hesabıyla gir.)
3. Hosting ilk kez kullanılıyorsa Firebase Console → Build → Hosting → "Get started" ile etkinleştirmen gerekebilir.
4. `https://lovelyy-native.web.app/gizlilik.html` adresini tarayıcıda aç.

Play Console'a şunları gir: Gizlilik politikası URL'si `.../gizlilik.html`, Hesap silme URL'si `.../hesap-silme.html`.
Data safety taslağı: [docs/play-data-safety.md](../docs/play-data-safety.md).
