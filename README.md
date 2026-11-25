# Lovelyy 💛

Çiftler için, ortak bir kediyi birlikte büyüttüğünüz mobil uygulama. Önce Android.

> **Şu anki adım (1):** Giriş (Google + e-posta), partnerle kodla eşleşme ve ana sayfa iskeleti.
> Kedi ve diğer özellikler sonraki adımlarda gelecek.

## Klasör yapısı (monorepo)

| Klasör | İçerik |
|---|---|
| `android/` | Android uygulaması (Kotlin + Jetpack Compose + Material 3) |
| `firebase/` | Firestore / Realtime Database güvenlik kuralları, Cloud Functions (TypeScript), kural testleri |
| `assets/` | Ortak dosyalar: font, ileride kedi animasyonları |
| `docs/` | Ürün özeti, tasarım prensipleri, mimari |
| `ios/` | İleride iOS uygulaması |
| `tools/` | Yardımcı scriptler (şimdilik boş) |

Tasarım: [docs/design-principles.md](docs/design-principles.md) · Mimari: [docs/architecture.md](docs/architecture.md)

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
   Google Analytics'i şimdilik kapatabilirsin.
2. Proje ana sayfasında **Android simgesine** tıkla (Uygulama ekle):
   - **Android paket adı:** `com.lovelyy.app` (birebir aynı olmalı)
   - **Uygulama takma adı:** Lovelyy
   - **SHA-1:** Google girişi için gerekli. Android Studio'da alttaki **Terminal** sekmesini aç ve yaz:
     ```
     .\gradlew signingReport
     ```
     Çıktıda `Variant: debug` altındaki `SHA1:` satırını kopyalayıp yapıştır.
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
| Google girişi açılıp hata veriyor | SHA-1'i Firebase'e eklediğinden emin ol (Adım 3.2). Emülatörde Google Play'li imaj kullan ve emülatörde bir Google hesabıyla oturum aç. |
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

Kural testleri (Firestore emülatörü, Java 11+ gerekir):

```
cd firebase/tests
npm install
npm test
```

Cloud Functions bu adımda kullanılmıyor (`firebase/functions` sadece iskelet). Deploy etmek Blaze (kullandıkça öde)
planı gerektirir; bildirimler gibi özellikler geldiğinde lazım olacak.
