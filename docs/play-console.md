# Google Play Console: ilk yayın rehberi

Ulaş için adım adım tarif (27 Kasım 2026). Menü adları Türkçe Play Console'a göre yazıldı, İngilizcesi parantez içinde.
Uygulama: **Lovelyy**, paket adı `com.lovelyy.app`, Firebase projesi `lovelyy-native`.
İletişim e-postası: `applovelyy@gmail.com`. Web: `https://lovelyy-native.web.app`.

Sıra: 1 → 2 → 3 → 4 zorunlu sırayla; 5 (Uygulama içeriği) ve 7 (mağaza metni) paralel yapılabilir.
Plus ürünleri (6) ancak ilk AAB yüklendikten sonra oluşturulabilir (Play, faturalandırma izni olan bir sürüm görmek ister).

---

## 1. Yükleme anahtarı (upload key), PC'de bir kez

Google Play uygulamayı kendi **uygulama imzalama anahtarıyla** imzalar (Play App Signing). Bizim anahtarımız sadece
"bu yüklemeyi biz yaptık" demek için. Anahtar ve şifreler **asla repoya girmez**.

1. ASCII bir klasör aç: `C:\lovelyy-keys` (profil yolunda Türkçe karakter olduğu için oraya koyma).
2. PowerShell:
   ```powershell
   & "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v `
     -keystore C:\lovelyy-keys\lovelyy-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
   Bir şifre sorar (iki kez), sonra ad/şehir gibi sorular gelir (ad soyad ve `TR` yeterli, gerisini boş geçebilirsin).
3. `android\keystore.properties` dosyasını oluştur (gitignore'da, GitHub'a gitmez). Ters eğik çizgi yerine `/` kullan:
   ```properties
   storeFile=C:/lovelyy-keys/lovelyy-upload.jks
   storePassword=BURAYA_SIFRE
   keyAlias=upload
   keyPassword=BURAYA_SIFRE
   ```
   (`keytool` varsayılan olarak tek şifre kullanır; iki şifre de aynıdır.)
4. **Yedekle:** `.jks` dosyasını ve şifreyi şifre yöneticisine / güvenli bir buluta koy. Kaybolursa Play desteğinden
   yeni yükleme anahtarı istenebilir ama günler sürer.

Gradle, şifreleri ayrıca şu ortam değişkenlerinden de okuyabilir (ileride CI için):
`LOVELYY_UPLOAD_STORE_FILE`, `LOVELYY_UPLOAD_STORE_PASSWORD`, `LOVELYY_UPLOAD_KEY_ALIAS`, `LOVELYY_UPLOAD_KEY_PASSWORD`.

## 2. İmzalı AAB'yi derle

1. Android Studio → **Tools → SDK Manager → SDK Platforms** → **Android 16 (API 36)** işaretle → Apply.
   (Play, Ağustos 2026'dan beri yeni uygulamalarda `targetSdk 36` istiyor; uygulama artık 36'yı hedefliyor.)
2. `android\app\google-services.json` yerinde olmalı (Crashlytics mapping dosyasını da derlerken yükler).
3. PowerShell, `android` klasöründe:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:TEMP = "C:\gtmp"; $env:TMP = "C:\gtmp"
   .\gradlew bundleRelease
   ```
4. Çıktı: `android\app\build\outputs\bundle\release\app-release.aab`.
   Gradle "No upload key" uyarısı verirse `keystore.properties` okunmamıştır (yolu kontrol et).
5. **Her yeni yüklemede** `android/app/build.gradle.kts` içindeki `versionCode`'u bir artır (1, 2, 3...). Play aynı
   numarayı ikinci kez kabul etmez. `versionName` kullanıcıya görünen sürümdür (şu an `1.0.0`).

## 3. Uygulamayı oluştur ve dahili teste yükle

1. [play.google.com/console](https://play.google.com/console) → **Uygulama oluştur (Create app)**.
   - Uygulama adı: `Lovelyy` (mağaza başlığı sonra bölüm 7'deki gibi değiştirilebilir)
   - Varsayılan dil: **Türkçe – tr-TR**
   - Uygulama mı oyun mu: **Uygulama**; Ücretsiz mi ücretli mi: **Ücretsiz** (Plus uygulama içi satın alım, bu yine "ücretsiz")
   - Beyanları işaretle → **Uygulama oluştur**.
2. Sol menü **Test edin ve yayınlayın (Test and release) → Test → Dahili test (Internal testing)**.
   - **Test kullanıcıları** sekmesi → **E-posta listesi oluştur** → adı `Ekip`, kendi Gmail'ini ve test edecek
     Gmail hesaplarını ekle → Kaydet. Aynı sayfadaki **katılım bağlantısını (opt-in URL)** kopyala.
3. **Yeni sürüm oluştur (Create new release)**.
   - "Play Uygulama İmzalama" sorulursa **Google tarafından oluşturulan anahtarı kullan** (varsayılan) → devam.
   - `app-release.aab` dosyasını sürükle.
   - Sürüm adı otomatik dolar (`1 (1.0.0)`). Sürüm notları: `İlk dahili test sürümü.`
   - **İleri → Kaydet → Dahili teste yayınla (Start rollout)**.
4. Telefonda katılım bağlantısını aç → **Test kullanıcısı ol** → Play Store'dan yükle.
   (Dahili test incelemeye girmez, genelde birkaç dakikada hazır olur.)

## 4. Play imza anahtarını Firebase'e ekle (Google ile giriş + App Check)

Play'den yüklenen uygulama, Play'in anahtarıyla imzalanır. Bu anahtarın parmak izi Firebase'de yoksa
**Google ile giriş çalışmaz** (e-posta girişi çalışır) ve App Check token alamaz.

1. Play Console → **Test edin ve yayınlayın → Kurulum (Setup) → Uygulama imzalama (App signing)**.
   İki kart var:
   - **Uygulama imzalama anahtarı sertifikası**: SHA-1 ve SHA-256'yı kopyala.
   - **Yükleme anahtarı sertifikası**: SHA-1 ve SHA-256'yı kopyala.
2. [Firebase Console](https://console.firebase.google.com) → `lovelyy-native` → ⚙️ **Proje ayarları → Genel** →
   "Uygulamalarınız" altında `com.lovelyy.app` → **Parmak izi ekle (Add fingerprint)** → dört değeri tek tek ekle
   (debug olanlar kalsın).
3. Google girişini yeniden dene (birkaç dakika sürebilir). `google-services.json`'u yeniden indirmek gerekmez.
4. **Play Integrity'yi bağla:** Play Console → **Test edin ve yayınlayın → Uygulama bütünlüğü (App integrity)** →
   Play Integrity API → **Cloud projesini bağla (Link a Cloud project)** → `lovelyy-native`.
5. **App Check:** Firebase Console → **App Check → Uygulamalar** → `com.lovelyy.app` → **Play Integrity** →
   SHA-256 alanına **uygulama imzalama** anahtarının SHA-256'sı → Kaydet. Firestore için **zorunlu yapma**; birkaç gün
   "Doğrulanmış istekler" oranını izle, sonra zorunlu yap (ayrıntı: [security.md](security.md) bölüm 2).
6. **API anahtarı kısıtı** (todo.md'deki madde): Google Cloud Console → APIs & Services → Credentials → Android key →
   Android uygulamaları listesine `com.lovelyy.app` + yeni iki SHA-1'i de ekle.

---

## 5. Uygulama içeriği (App content) formları

Yer: Play Console → sol menü en altta **Politika ve programlar (Policy and programs) → Uygulama içeriği**.
Hepsi bitmeden kapalı teste / üretime gönderilemez; Kontrol paneli (Dashboard) eksikleri listeler.

| Form | Cevap |
|---|---|
| **Gizlilik politikası** | `https://lovelyy-native.web.app/gizlilik.html` (önce web sitesi yayınlanmalı, bkz. [web/README.md](../web/README.md)) |
| **Uygulama erişimi (App access)** | "Tüm işlevler veya bazı işlevler kısıtlı" → talimat ekle (aşağıda) |
| **Reklamlar** | **Hayır**, uygulamada reklam yok |
| **İçerik derecelendirmesi** | Aşağıdaki anket |
| **Hedef kitle ve içerik** | Aşağıda |
| **Veri güvenliği (Data safety)** | [play-data-safety.md](play-data-safety.md) |
| **Hesap silme** | URL: `https://lovelyy-native.web.app/hesap-silme.html`; uygulama içi: Biz → Hesap → Hesabı sil |
| **Devlet uygulamaları** | Hayır |
| **Finansal özellikler** | "Uygulamamın finansal özelliği yok" |
| **Sağlık** | Sağlık özelliği yok |
| **Haber uygulaması** | Hayır |
| **Reklam kimliği (Advertising ID)** | Hayır, kullanmıyor (manifest'te kapalı, reklam SDK'sı yok) |
| **Ön plan hizmeti / tam ekran bildirim / tam zamanlı alarm izinleri** | Uygulama bu izinleri istemiyor, form çıkmaz |

### Uygulama erişimi: inceleme için hesap

Uygulama girişsiz ve eşleşmesiz açılmaz, bu yüzden inceleyene **hazır eşleşmiş bir hesap** ver.
Öneri: sadece inceleme için iki yeni e-posta hesabı aç (ör. `inceleme1`/`inceleme2`), uygulamada eşleştir, kediyi
kullan ki ekranlar dolu görünsün. (Ada/Bora test hesaplarını vermek de olur, ama onlar bizim testlerimizde de kullanılıyor.)

Forma girilecek talimat:
- Ad: `İnceleme hesabı`
- Kullanıcı adı: `<inceleme e-postası>` / Şifre: `<şifre>`
- Diğer bilgiler:
  > The app is for couples. This account is already paired with a partner account, so all features are visible.
  > Sign in with "E-posta ile giriş" on the welcome screen. Lovelyy Plus is unlocked by a free 14-day trial when a
  > couple pairs; purchases use Google Play Billing.

### İçerik derecelendirmesi (IARC anketi)

1. **İçerik derecelendirmesi → Anketi başlat**. E-posta: `applovelyy@gmail.com`. Kategori: **Diğer tüm uygulama türleri
   (All other app types)** (oyun, sosyal medya veya referans değil).
2. Cevaplar:

| Soru | Cevap | Neden |
|---|---|---|
| Şiddet, kan, korku | Hayır | |
| Cinsellik, çıplaklık | Hayır | |
| Küfür / kaba dil | Hayır | Uygulamanın kendi içeriğinde yok |
| Uyuşturucu, alkol, tütün | Hayır | |
| Kumar, gerçek parayla şans oyunu | Hayır | Karar çarkı sadece seçim yapar, para/ödül yok |
| Kullanıcılar birbirleriyle etkileşebiliyor / içerik paylaşabiliyor mu? | **Evet** | Eşleşen iki kişi not, mektup, çizim paylaşıyor |
| Kullanıcı konumunu başka kullanıcılarla paylaşıyor mu? | **Evet** | İsteğe bağlı, şehir düzeyinde, yalnızca partnerle |
| Dijital ürün satın alımı var mı? | **Evet** | Lovelyy Plus |
| Kullanıcı kişisel bilgilerini paylaşabiliyor mu? | **Evet** | Ad partnerle paylaşılır |
| Uygulama, kullanıcı tarafından oluşturulan içeriği herkese açık gösteriyor mu? | Hayır | Yalnızca iki partner arasında |
| Uygulama bir web tarayıcısı / arama motoru mu? | Hayır | |

3. Beklenen sonuç: PEGI 3 / Everyone benzeri, "Kullanıcılar Etkileşimde Bulunur, Konum Paylaşır, Uygulama İçi Satın
   Alımlar" etiketleriyle. **Kaydet → Derecelendirmeyi uygula**.

### Hedef kitle ve içerik

- Hedef yaş grupları: **18 ve üzeri** (yalnızca bu). Çocuklara ve gençlere yönelik değil, Aileler politikasına girmez.
  (Kullanım şartlarında yaş alt sınırı 13; Play'deki hedef kitle seçimi bundan ayrıdır, ilişki uygulaması olarak 18+ seçiyoruz.)
- "Uygulamanız çocukların ilgisini çekebilir mi?": **Hayır**.
- Mağaza ön izlemede "çocuklara yönelik değil" uyarısı otomatik çıkar, sorun değil.

### Mağaza ayarları (Store settings)

Play Console → **Mağazada büyüme (Grow users) → Mağaza varlığı → Mağaza ayarları**:
- Uygulama kategorisi: **Yaşam tarzı (Lifestyle)**. Etiketler: İlişkiler, Sanal evcil hayvan / Kişiselleştirme (Play'in listesinden en yakın 2-5 tane).
- E-posta: `applovelyy@gmail.com`; Web sitesi: `https://lovelyy-native.web.app`; Telefon: boş bırakılabilir.

---

## 6. Lovelyy Plus ürünleri (elle kurulum)

Kodun beklediği kimlikler `data/Plus.kt` → `PlusProducts`. **Birebir aynı yazılmalı**, sonradan değiştirilemez.

| Play Console'da | Kimlik (ID) | Ayrıntı |
|---|---|---|
| Abonelik | `lovelyy_plus` | Ad: `Lovelyy Plus` |
| Temel plan | `yearly` | Otomatik yenilenen, 1 yıl, **₺499,99** (global $29.99) |
| Temel plan | `monthly` | Otomatik yenilenen, 1 ay, **₺79,99** (global $4.99) |
| `yearly` üzerinde teklif | `first-year` | İlk yıl **₺399,99**, sonra normal fiyat |
| Tek seferlik ürün | `lovelyy_plus_gift_year` | "1 yıl Plus hediye", yenilenmez, **₺499,99** (global $29.99) |

**Ücretsiz deneme (free trial) aşaması EKLEME.** 14 günlük deneme uygulamanın kendi içinde, eşleşince başlar;
Play'de deneme eklenirse ikinci bir deneme olur ve uygulamanın hesapları bozulur.

### 6.0 Ödeme profili (bir kez)
Ürün oluşturmak için satıcı (ödeme) profili gerekir: Play Console ana sayfası (tüm uygulamalar) → **Ayarlar →
Ödeme profili (Payments profile)**. Henüz yoksa **Para kazanma kurulumu**'ndan oluştur: ad/adres, banka hesabı (IBAN),
vergi bilgileri. Doğrulama birkaç gün sürebilir, bu yüzden erken yap.

### 6.1 Abonelik: `lovelyy_plus`
1. Uygulamayı seç → sol menü **Play ile para kazanın (Monetize with Play) → Ürünler → Abonelikler** →
   **Abonelik oluştur**.
2. Ürün kimliği: `lovelyy_plus` · Ad: `Lovelyy Plus` → **Oluştur**.
3. Açılan sayfada **Avantajlar (Benefits)** (isteğe bağlı, Play'in yönetim sayfasında görünür), en fazla 4 tane:
   `İkiniz için tek abonelik` · `Seri koruma ve telafi` · `Mırmır'ın Plus eşyaları` · `Sınırsız not, plan, kapsül ve çizim`
4. **Temel plan ekle (Add base plan)**:
   - Temel plan kimliği: `yearly`
   - Tür: **Otomatik yenilenen**; Faturalandırma dönemi: **1 yıl**
   - Yetkisiz kullanım süresi (Grace period): varsayılan (7 gün) kalsın; Hesap beklemeye alma: varsayılan
   - Yeniden abone olma: **İzin ver**; Fiyat değişikliği / Diğer: varsayılan
   - **Fiyatları ayarla (Set prices)** → tüm ülkeleri seç → **Fiyatı ayarla** → `29.99 USD` gir → Play diğer
     para birimlerine çevirir → listede **Türkiye**'yi bul, ✏️ → `499,99 TRY` yap → Güncelle.
   - **Kaydet → Etkinleştir (Activate)**.
5. Tekrar **Temel plan ekle**: kimlik `monthly`, otomatik yenilenen, **1 ay**, fiyat `4.99 USD`, Türkiye `79,99 TRY` →
   Kaydet → Etkinleştir.
6. `yearly` temel planının satırında **Teklif ekle (Add offer)**:
   - Teklif kimliği: `first-year`
   - Uygunluk (Eligibility): **Geliştirici tarafından belirlenir (Developer determined)**.
     (Teklifi sadece deneme bittikten sonraki 48 saatte uygulama gösteriyor. "Yeni müşteri" seçilirse Play bazı
     kişilere teklifi döndürmez ve ekrandaki ₺399,99 ile ödeme ekranındaki fiyat tutmaz.)
   - **Aşama ekle (Add phase)** → Tür: **İndirimli yinelenen ödeme (Discounted recurring payment)** → Dönem sayısı
     (billing periods): **1** → Fiyat: Türkiye `399,99 TRY`, diğer ülkeler `23.99 USD` karşılığı (Play önerisini kabul edebilirsin).
   - Etiket (tag) gerekmez → **Kaydet → Etkinleştir**.

### 6.2 Hediye: `lovelyy_plus_gift_year`
1. **Play ile para kazanın → Ürünler → Tek seferlik ürünler (One-time products)** → **Tek seferlik ürün oluştur**.
2. Ürün kimliği: `lovelyy_plus_gift_year` · Ad: `Lovelyy Plus · 1 yıl hediye` ·
   Açıklama: `Sevgilinle birlikte 1 yıl Lovelyy Plus. Yenilenmez.`
3. **Satın alma seçeneği ekle (Add purchase option)**: kimlik `buy`, tür **Satın al (Buy)**, bu seçeneği
   **Geriye dönük uyumlu (Backwards compatible)** olarak işaretle (uygulama bu seçeneği okur).
   Fiyat: `29.99 USD`, Türkiye `499,99 TRY`.
4. **Kaydet → Etkinleştir**. (Uygulama hediyeyi kaydettikten sonra "tüketir", böylece tekrar alınabilir.)

Fiyatlar uygulamaya Play'den gelir; Play'deki fiyatı değiştirirsen ekranlar otomatik güncellenir. Play'e
ulaşılamazsa uygulama `strings.xml`'deki yedek fiyatları gösterir (`plus_price_*_fallback`), onları da aynı tut.

### 6.3 Lisans test kullanıcıları (gerçek para ödemeden satın alma denemesi)
1. Play Console ana sayfası (tüm uygulamalar) → **Ayarlar → Lisans testi (License testing)**.
2. Test edecek Gmail hesaplarını ekle (dahili test listesindekilerle aynı olsun) → Lisans yanıtı: `RESPOND_NORMALLY`
   → **Değişiklikleri kaydet**.
3. Telefonda bu Gmail ile Play Store'a girili olmalı ve uygulama **dahili test bağlantısından** yüklenmiş olmalı
   (Android Studio'dan yüklenen debug sürüm Play ürünlerini göremez, sahte satın almaya düşer).
4. Satın alırken ödeme ekranında **"Test kartı, her zaman onaylanır"** seç. Para çekilmez.
5. Test aboneliklerinde süre hızlıdır: aylık 5 dakikada, yıllık 30 dakikada yenilenir (en fazla 6 kez, sonra biter).
   Deneme sonu teklifini görmek için debug sürümdeki "+1 gün" düğmesi Play sürümünde yok; teklifi gerçek 14 gün sonunda veya
   yeni eşleşen bir test çiftinde 14 gün bekleyerek görürsün.
6. Kontrol listesi: yıllık, aylık, ilk yıl teklifi, hediye, "Satın alımları geri yükle", Plus'ın iki partnerde de açılması,
   Play'den aboneliği iptal edince uygulamanın "yenilenmiyor" göstermesi.

### Otomatikleştirme hakkında
Ürünler Play Developer API ile de kurulabilir ama bunun için Google Cloud'da bir **servis hesabı** açıp Play Console'da
yetki vermek gerekir. 3 ürün için elle kurulum daha hızlı ve güvenli; şimdilik **elle** yapıyoruz (Ulaş, 27 Kasım 2026).

---

## 7. Mağaza metinleri (Ana mağaza girişi)

Yer: **Mağazada büyüme → Mağaza varlığı → Ana mağaza girişi (Main store listing)**. Görseller (simge 512×512,
öne çıkan görsel 1024×500, ekran görüntüleri) ayrı iş, Fable hazırlıyor.

### Türkçe (tr-TR, varsayılan)

**Uygulama adı** (30 karakter sınırı, 30):
```
Lovelyy: Sevgili & Çift Kedisi
```

**Kısa açıklama** (80 karakter sınırı):
```
Sevgilinle ortak bir kedi büyüt, günlerinizi ve anılarınızı birlikte biriktir.
```

**Tam açıklama:**
```
Lovelyy, sevgilinle birlikte baktığınız ortak bir kedi ve ikinize ait küçük bir dünya.

🐱 Mırmır'a birlikte bakın
Kediniz acıkır, susar, uykusu gelir, sevgi ister. Biriniz besler, diğeriniz oynar; ikiniz de bakınca bakım seriniz büyür. Mırmır'ı sevdiğinizde kalpler uçuşur, uykusu gelince kıvrılıp uyur. Ana ekran widget'ıyla nasıl olduğunu her an görürsün.

💓 Seni düşünüyorum
Tek dokunuşla kalp atışı ya da "seni düşünüyorum" gönder. Ruh halini paylaş, günün sorusunu ikiniz cevaplayın, cevaplar ikiniz de yazınca açılsın.

📅 Birlikte geçen günler
Kaç gündür birlikte olduğunuzu sayın, özel günleri ve planları ortak takvimde tutun. 100. gün ve yıl dönümlerinde küçük kutlamalar sizi bekler.

✍️ Size ait anılar
Notlar bırakın, birlikte canlı çizim yapın, hayaller listesine ekleyin, gelecekte açılacak zaman kapsülü mektupları yazın. Ne yesek, nereye gitsek, ne izlesek? Karar çarkı seçsin.

📍 Uzakta olsanız da
İsterseniz aranızdaki mesafeyi şehir düzeyinde görün. Konumunuz yuvarlanır, yalnızca partneriniz görür ve istediğiniz an kapatırsınız.

✨ Lovelyy Plus
Eşleşen her çift 14 gün Plus'ı ücretsiz dener, kart istenmez. Plus ile seri koruma ve telafi, Mırmır'ın özel eşyaları, sınırsız not, plan, kapsül ve çizim gibi ekstralar açılır. Tek abonelik ikiniz için de geçerlidir. Plus bitince hiçbir şeyiniz silinmez.

🔒 Gizlilik
Paylaştıklarınızı yalnızca siz ve eşleştiğiniz kişi görür. Reklam yok, verilerinizi satmayız. Kullanım verisini tek anahtarla kapatabilir, hesabınızı ve tüm verilerinizi uygulamadan silebilirsiniz.

Sorular ve öneriler: applovelyy@gmail.com
```

### English (en-US): şimdilik EKLEME, İngilizce arayüz gelince kullanılacak

**App name** (29):
```
Lovelyy: Couples & Shared Cat
```

**Short description:**
```
Raise a cat together with your partner and keep your days and memories close.
```

**Full description:**
```
Lovelyy is a shared cat you and your partner care for together, and a small world that belongs to the two of you.

🐱 Care for Mırmır together
Your cat gets hungry, thirsty, sleepy and wants love. One of you feeds, the other plays; when you both care, your streak grows. Hearts float up when you pet Mırmır, and it curls up to nap when it's tired. A home-screen widget shows how it's doing at a glance.

💓 Thinking of you
Send a heartbeat or a "thinking of you" with one tap. Share your mood and answer the daily question; answers open once you have both replied.

📅 Your days together
Count the days you've been together and keep special days and plans in a shared calendar. Little celebrations wait for your 100th day and anniversaries.

✍️ Memories that are yours
Leave notes, draw together live, build a bucket list and write time-capsule letters that open in the future. What to eat, where to go, what to watch? Let the decision wheel pick.

📍 Even when you're apart
If you like, see the distance between you at city level. Your location is rounded, only your partner sees it, and you can turn it off any time.

✨ Lovelyy Plus
Every couple gets a free 14-day Plus trial, no card needed. Plus adds streak protection and make-ups, Mırmır's special items, unlimited notes, plans, capsules and drawings, and more. One subscription covers both of you. Nothing is deleted when Plus ends.

🔒 Privacy
Only you and your partner see what you share. No ads, and we never sell your data. Turn usage data off with one switch, and delete your account and all data right in the app.

Questions and ideas: applovelyy@gmail.com
```

Not: Uygulamanın arayüzü şu an yalnızca Türkçe ([LOCALIZATION.md](LOCALIZATION.md)). İngilizce mağaza girişi eklenirse yabancı kullanıcı Türkçe bir uygulamayla karşılaşır ve kötü puan verir; İngilizce çeviri gelene kadar sadece Türkçe giriş yeterli. Uygulamayı şimdilik yalnızca **Türkiye**'de yayınlamak da mantıklı (Üretim → Ülkeler/bölgeler).

---

## Kalan işler (kim yapacak)

| İş | Kim |
|---|---|
| Yükleme anahtarı + `keystore.properties` (bölüm 1) | Ulaş, PC |
| SDK 36'yı kur, emülatörde dene, `bundleRelease` (bölüm 2) | Ulaş, PC |
| Web sitesini yayınla (gizlilik linki) | Ulaş, PC (`npx firebase-tools deploy --only hosting --project lovelyy-native`) |
| Play'de uygulamayı oluştur, AAB yükle (bölüm 3) | Ulaş |
| SHA'ları Firebase'e ekle, Play Integrity, App Check (bölüm 4) | Ulaş |
| Uygulama içeriği formları (bölüm 5) + inceleme hesabı | Ulaş (cevaplar burada) |
| Ödeme profili + Plus ürünleri + lisans testçileri (bölüm 6) | Ulaş |
| Mağaza görselleri | Fable |
| Kapalı test: en az 12 kişi, 14 gün | Ulaş |
| Sunucu tarafı satın alma doğrulaması | Ertelendi (Blaze gerekir, bkz. [security.md](security.md)) |
