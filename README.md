# Lovelyy 💛

Çiftler için, ortak bir kediyi birlikte büyüttüğünüz mobil uygulama. Önce Android.

Android uygulaması Kotlin + Jetpack Compose + Material 3 ile yazılır, arka uç tamamen Firebase'dir
(Auth + Firestore). Tek repoda (monorepo) Android, Firebase kuralları, ortak dosyalar ve araçlar bir arada durur.

## Özellikler ve durum

| Özellik | Durum |
|---|---|
| Karşılama, Giriş (Google + e-posta/şifre), isim belirleme | Hazır |
| İlk açılış tanıtımı: 3 sayfalık "dene" tanıtımı (besle, kalp atışı gönder, birlikte çiz); "Atla" ile geçilebilir | Hazır |
| Partnerle 6 haneli kodla eşleşme | Hazır |
| Eşleşme sonrası: "Eşleştiniz" ekranı, bildirim ve konum izni ön ekranları (izin penceresi sadece düğmeye basınca açılır) | Hazır |
| Anasayfa (Yuva / Anılar / Planlar / Biz) ve alt gezinme çubuğu | Hazır |
| Ortak animasyonlu kedi: ana sayfa + kedi bakımı ekranı, seri ve aksesuarlar | Hazır (kısa videolar `assets/cat/` içinde, ExoPlayer ile oynar) |
| Gün sayacı, kalp atışı, ruh hali, günün sorusu, notlar | Hazır |
| Planlar (takvim + özel günler), hayaller listesi, karar çarkı | Hazır |
| Anılar: canlı çizim, zaman kapsülü | Hazır |
| Biz: profil, ayarlar, mesafe (şehir düzeyinde konum paylaşımı) | Hazır |
| 4 ana ekran widget'ı (küçük kedi, geniş kedi, gün sayacı + kalp, canlı çizim) | Hazır |
| Analytics, Crashlytics, Performance, Timber katmanı | Hazır, bkz. [docs/analytics.md](docs/analytics.md) |
| Geçmiş ekranları (ruh hali, önceki sorular, kalp atışı, çark), arama | Hazır |
| Çizimlerimiz (kayıtlı çizimler, silme), açılmış mektuplar, aksesuarlar, bildirimler | Hazır |
| Hesap (giriş yöntemi, şifre sıfırlama, çıkış, hesap silme) ve eşleşmeyi kaldırma | Hazır; eşleşme kaldırılınca ortak veriler iki taraftan da kalıcı silinir (yeni Firestore kuralları yayınlanmalı) |
| Henüz tasarlanıp kodlanmamış ekranlar | Arayüzde **"Yakında"** olarak görünür |

## Localization

Arayüz metinleri tamamen `android/app/src/main/res/values/strings.xml` içindedir (Türkçe varsayılan; çeviri henüz yok).
Sayılar `<plurals>` ile, tarihler `text/AppLocale.kt` ile yerel dile göre biçimlenir. Yeni dil eklemek için:
`res/values-xx/strings.xml` oluştur (tüm anahtarlar, plurals ve `cal_weekdays_short` dahil), `res/xml/locales_config.xml`
dosyasına dili ekle, Quicksand'in kapsamadığı alfabeler için yedek yazı tipi düşün. Ayrıntılar, kalan işler
(günlük soru listesi hâlâ kodda, varsayılan kedi adı Firestore verisi) ve doğrulama notları: [docs/LOCALIZATION.md](docs/LOCALIZATION.md).

## Teknoloji

- Kotlin 2.1, Android Gradle Plugin 8.10, `minSdk 26`, `compileSdk/targetSdk 35`, Java 17
- Jetpack Compose (BOM 2025.06) + Material 3, Quicksand yazı tipi
- Firebase: Authentication, Firestore, Analytics, Crashlytics, Performance Monitoring
- Credential Manager + Google ile giriş, Media3 ExoPlayer (kedi videoları), WorkManager (widget yenileme), Timber
- Sürümlerin tamamı: [`android/gradle/libs.versions.toml`](android/gradle/libs.versions.toml)

## Klasör yapısı (monorepo)

| Klasör | İçerik |
|---|---|
| `android/` | Android uygulaması (Gradle projesi, Android Studio'da bu klasörü aç) |
| `firebase/` | Firestore / Realtime Database güvenlik kuralları, kural testleri (`tests/`), Cloud Functions iskeleti (`functions/`) |
| `assets/` | Ortak dosyalar: Quicksand fontu, `brand/` (uygulama logosu), `animations/` ve `images/` (şimdilik boş, ileri için ayrıldı) |
| `docs/` | Ürün özeti, tasarım prensipleri, mimari, analytics |
| `tools/` | Yardımcı araçlar: `tools/cat-animation/` kedi kliplerini fal.ai ile üretir (bkz. [tools/README.md](tools/README.md)) |
| `ios/` | İleride iOS uygulaması (şimdilik sadece not) |

Android paket düzeni (`android/app/src/main/java/com/lovelyy/app/`):

| Paket | Ne var |
|---|---|
| `ui/` | `LovelyyRoot`, `AppViewModel` ve ekranlar: `auth/` (Karşılama + Giriş), `onboarding/` (tanıtım + eşleşme sonrası akış), `profile/`, `pairing/`, `home/`, `cat/`, `setup/`, `theme/`, `components/` |
| `data/` | Firebase erişimi ve modeller: Auth, User, Pairing, Cat ve diğer depolar |
| `analytics/` | Tipli analytics olayları ve ekran görüntüleme takibi |
| `telemetry/` | Crashlytics, Performance, Timber ağacı, onay anahtarı |
| `signals/`, `widget/` | Sinyal metinleri ve ana ekran widget'ı |

Kedi videoları: `android/app/src/main/assets/cat/*.mp4`. Debug'a özel kodlar: `android/app/src/debug/`.
Testler: `android/app/src/test/`.

## Belgeler

- Rehber ve kurallar: [CLAUDE.md](CLAUDE.md) · Tasarım dili: [DESIGN.md](DESIGN.md)
- Ürün: [docs/product.md](docs/product.md) · Tasarım prensipleri: [docs/design-principles.md](docs/design-principles.md)
- Mimari: [docs/architecture.md](docs/architecture.md) · Analytics ve hata raporlama: [docs/analytics.md](docs/analytics.md)

## Derleme, çalıştırma, test

- Android Studio'da `android/` klasörünü aç, **▶ Run** (ayrıntılar aşağıda).
- Komut satırında (`android/` içinden): `./gradlew assembleDebug` (Windows'ta `gradlew.bat assembleDebug`),
  birim testleri için `./gradlew testDebugUnitTest`.
- Android SDK bulut konteynerinde derlenemez; derleme ve testler kullanıcının kendi bilgisayarında yapılır.
- **Windows, kullanıcı yolunda ASCII olmayan karakter varsa (ör. ş, ı, ö):** `JAVA_HOME` değerini Android Studio'nun
  `jbr` klasörüne ayarla ve `TEMP`/`TMP` değişkenlerini ASCII bir klasöre al (ör. `C:\gtmp`), yoksa Gradle hata verebilir.
- Birim testlerinde bellek hatası olursa: `./gradlew testDebugUnitTest -Dorg.gradle.jvmargs=-Xmx2048m`
- Telemetri (analytics/Crashlytics/Performance) debug'da varsayılan olarak kapalıdır; açmak için
  `./gradlew assembleDebug -Ptelemetry.debug=true` ya da [docs/analytics.md](docs/analytics.md).

---

## Uygulamayı çalıştırma (Windows + Android Studio)

### 1. Android Studio'yu kur ve projeyi aç

1. [Android Studio](https://developer.android.com/studio)'yu indir ve kur (varsayılan ayarlar yeterli).
2. Repoyu bilgisayarına indir: GitHub Desktop ile ya da terminalde
   `git clone https://github.com/Ulasceri/Lovelyy-App.git`
3. Android Studio → **Open** → repodaki **`android`** klasörünü seç (repo kökünü değil).
4. İlk açılışta Gradle her şeyi indirir, birkaç dakika sürebilir. Sağ altta ilerlemeyi görürsün.
   "Upgrade AGP / Kotlin" gibi öneriler çıkarsa şimdilik geçebilirsin.

### 2. Firebase olmadan ilk deneme

Firebase dosyası olmadan da uygulama derlenir ve açılır; ekranda **"Firebase kurulumu gerekiyor"** yazar.
Bu, her şeyin doğru kurulduğunu gösterir.

1. **Device Manager** (sağ kenardaki telefon simgesi) → **+** → **Create Virtual Device** → *Pixel 8* →
   sistem imajı olarak **Google Play** simgeli bir imaj seç (Google girişi için gerekli) → Finish.
2. Üstteki yeşil **▶ Run** butonuna bas.

Tasarımı Firebase olmadan da görebilirsin: `HomeScreen.kt` veya `LoginScreen.kt` dosyasını açıp sağ üstten
**Split / Design** görünümüne geç (Compose Preview).

### 3. Firebase projesini oluştur

1. [console.firebase.google.com](https://console.firebase.google.com) → **Proje oluştur** → adı `Lovelyy`.
   Google Analytics'i **açık** bırak: Analytics, Crashlytics ve Performance için gerekir
   (kapalı bırakırsan sonradan Proje ayarları → Entegrasyonlar'dan açıp google-services.json'u yeniden indirmen gerekir).
2. Proje ana sayfasında **Android simgesine** tıkla (Uygulama ekle):
   - **Android paket adı:** `com.lovelyy.app` (birebir aynı olmalı)
   - **Uygulama takma adı:** Lovelyy
   - **SHA-1:** Google girişi için gerekli. Repo kendi debug anahtarıyla gelir
     (`android/app/debug.keystore`), bu yüzden `signingReport` çalıştırmana gerek yok. Şunları ekle:
     - SHA-1: `19:AA:9E:5A:47:AE:5D:7F:70:51:A0:A6:17:AE:83:0E:9C:30:0A:B3`
     - SHA-256: `34:F5:C1:9F:87:19:D8:1D:83:76:06:27:7A:1F:34:C5:D2:B9:98:9D:26:47:06:50:08:81:A7:E1:59:A1:79:40`

     (Bu anahtar sadece debug içindir; aynı paket adını kullanan başka bir Firebase projesiyle
     "identical OAuth client" çakışması yaşamamak için uygulamaya özel tutulur. Release için ayrı anahtar gerekir.)
3. **google-services.json** dosyasını indir ve **`android/app/`** klasörüne koy
   (`android/app/google-services.json`). Bu dosya `.gitignore`'da, GitHub'a gitmez.

### 4. Giriş yöntemlerini aç

Firebase Console → **Build → Authentication** → **Get started** → **Sign-in method**:

1. **Email/Password** → Enable → Save.
2. **Google** → Enable → destek e-postanı seç → Save.
3. Google'ı açtıktan sonra **google-services.json'u tekrar indir** (Proje ayarları ⚙️ → Genel → Uygulamalarınız)
   ve `android/app/` içindekinin üzerine yaz. Yeni dosya Google girişi için gereken kimliği içeriyor.

### 5. Veritabanını (Firestore) oluştur ve kuralları yükle

1. Firebase Console → **Build → Firestore Database** → **Create database**.
2. Konum: **eur3 (Europe)** ya da `europe-west1`. Mod: **Production mode**.
3. **Rules** sekmesine geç, içindekini sil, bu repodaki [`firebase/firestore.rules`](firebase/firestore.rules)
   dosyasının tamamını yapıştır → **Publish**.

   > Bu kurallar eşleşmeyi güvenli yapar: sadece iki partner kendi ortak verilerini görebilir.
   > Kuralları yüklemezsen uygulama "izin hatası" verir.

### 6. Çalıştır ve eşleşmeyi dene

1. Android Studio'da **▶ Run**. Artık giriş ekranı gelir.
2. Eşleşmeyi denemek için iki hesap lazım: emülatör + kendi telefonun (telefonda *Geliştirici seçenekleri →
   USB hata ayıklama* açık, kabloyla bağla) ya da iki emülatör.
3. Birinci kişi **Kodumu paylaş** sekmesinde 6 haneli kodu görür, ikinci kişi **Kod gir** sekmesine bu kodu yazar.
   İkisi de otomatik olarak ana sayfaya geçer.

### Sık karşılaşılan sorunlar

| Sorun | Çözüm |
|---|---|
| "Google girişi henüz ayarlanmadı" | Adım 4.3: Google'ı açtıktan sonra google-services.json'u yeniden indir. |
| Google girişi açılıp hata veriyor | Repo'daki debug anahtarının SHA-1'ini Firebase'e eklediğinden emin ol (Adım 3.2), sonra google-services.json'u yeniden indir. Emülatörde Google Play'li imaj kullan ve emülatörde bir Google hesabıyla oturum aç. |
| "İzin hatası" / ana sayfaya geçmiyor | Firestore kurallarını yayınla (Adım 5.3). |
| Gradle sync hatası | File → *Invalidate Caches / Restart*. JDK olarak Android Studio'nun kendi JDK'sı (17+) seçili olmalı. |

---

## Firebase CLI (isteğe bağlı, ileri seviye)

Kuralları konsoldan yapıştırmak yerine komut satırından yüklemek için:

```
npm install -g firebase-tools
firebase login
cd firebase
firebase use --add          # projeni seç
firebase deploy --only firestore,database
```

Kural testleri (Firestore emülatörü, Java 11+ gerekir; `firebase/tests/firestore.rules.test.mjs`):

```
cd firebase/tests
npm install
npm test
```

Cloud Functions şu an kullanılmıyor (`firebase/functions` sadece iskelet). Deploy etmek Blaze (kullandıkça öde)
planı gerektirir; bildirimler gibi özellikler geldiğinde lazım olacak.
