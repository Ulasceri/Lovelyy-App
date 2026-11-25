# Lovelyy Tasarım Sistemi

Bu dosya Lovelyy'nin MEVCUT tasarım sisteminin tek doğruluk kaynağıdır (UI işlerinde önce buraya bak). Değerler koddan çıkarıldı. Kodda doğrulanamayan her şey "doğrulanamadı" diye işaretlidir.

Kaynaklar (`android/app/src/main/`):
`java/com/lovelyy/app/ui/theme/{Color,Shape,Theme,Type}.kt`, `ui/components/{Components,Design}.kt`, `ui/{auth,pairing,home,cat,profile,setup,heartbeat}`, `res/values/{colors,themes,strings}.xml`, `res/font/quicksand_*.ttf`, `docs/design-principles.md`, ve onaylı tasarım tuvali "Lovelyy ekran tasarımları" (claude.ai artifact FzNY4Jh2r7cyjayPKBFZk8; yalnızca pano listesi/notları okundu, pano HTML'leri okunmadı).

---

## 1. İlkeler

1. **Yumuşak ve yuvarlak.** Köşeler 12-32dp (kartlar 24-28dp, butonlar 18-20dp, hap şekilleri). Sert kenar ve sert gölge yok.
2. **Kedi merkezde.** Ana sayfa (Yuva) her zaman kediyle açılır; her şey onun çevresindedir.
3. **"Biz" hissi.** İki kişi birlikte görünür (çift avatar, "Ulaş & Deniz", "Bizim köşemiz").
4. **Ekran başına tek ana eylem.** Tek büyük şeftali buton; gerisi daha sessiz.
5. **Canlı ama nazik.** Mikro animasyonlar (nefes, atan kalp, basınca ezilme) ve dokunuşlarda haptik tık.
6. **Tek elle kullanım.** Ana eylemler ekranın alt yarısında, başparmak erişiminde.
7. **Altta Material 3.** Standart bileşenler, bizim token'larımızla.
8. **v1 yalnızca açık tema.** Koyu tema yok (`Theme.kt` tek dosyada eklenebilir; ekranlar ham renk kullanmamalı, bkz. bölüm 11 not).

Tuval kuralları (onaylı tasarımlar): 390x844 panolar (köşe yarıçapı 36), emoji yok (SVG ikon/yüz), alt menü her panoda kopyalanır.

---

## 2. Renk token'ları

Kaynak: `ui/theme/Color.kt`. Tema eşlemesi: `ui/theme/Theme.kt` (`lightColorScheme`).

| Ad | Hex | Compose sembolü | Kullanım |
|---|---|---|---|
| Peach | `#FFB38A` | `Peach` | Birincil: ana buton, seçili/aktif vurgu, orta kalp butonu |
| PeachDeep | `#E8895C` | `PeachDeep` | Basılı/vurgu, imleç, `tertiary` |
| PeachSoft | `#FFE6D6` | `PeachSoft` | Tinted yüzey, seçili nav hapı, `primaryContainer`, `surfaceVariant` |
| PeachTint | `#FFF1E8` | `PeachTint` | Kod kutuları, ikon daireleri, dolu OTP kutusu, pasif ikincil buton zemini |
| PeachCard | `#FFE9DB` | `PeachCard` | "Birlikte" kartı |
| Mint | `#9ED9C8` | `Mint` | İkincil: sakin/taze vurgu, "bekleniyor" noktası, `secondary` |
| MintSoft | `#DDF2EC` | `MintSoft` | Mint yüzey, `secondaryContainer` |
| MintTint | `#E4F5F0` | `MintTint` | Mint kart zemini (en açık) |
| MintInk | `#2F5A4E` | `MintInk` | Mint üzerindeki metin/ikon |
| MintInkSoft | `#4F6B63` | `MintInkSoft` | Mint kart içi ikincil metin |
| Brown | `#4A3B35` | `Brown` | Metin (`onBackground`, `onSurface`, `onPrimary`) |
| BrownSoft | `#75655E` | `BrownSoft` | İkincil metin (cream üstünde 4.5:1), `onSurfaceVariant` |
| BrownHint | `#8A7A73` | `BrownHint` | İkonlar, placeholder tonu |
| Cream | `#FFF8F2` | `Cream` | Arka plan (`background`, `surface`) |
| CreamCard | `#FFFFFF` | `CreamCard` | Krem üstündeki kartlar (`surfaceContainer*`) |
| CatBg | `#FAE2C0` | `CatBg` | Kedinin arkası (hero kart zemini) |
| Outline | `#EADFD6` | `Outline` | Kenarlık, ayırıcı çizgi (`outline`, `outlineVariant`) |
| Sand | `#F3ECE6` | `Sand` | Boş avatar, bar izi (track) |
| NeedLow | `#F07B45` | `NeedLow` | İhtiyaç barı düşük seviye |
| NeedOk | `#5DB89E` | `NeedOk` | İhtiyaç barı normal seviye |
| ErrorRose | `#D9534F` | `ErrorRose` | Hata (`error`) |

`res/values/colors.xml` (yalnızca pencere/splash arka planı için): `peach #FFB38A`, `mint #9ED9C8`, `brown #4A3B35`, `cream #FFF8F2`, `brown_soft #8A7A72` (not: Color.kt'deki `BrownSoft #75655E` ile FARKLI; `docs/design-principles.md` de eski `#8A7A72` değerini yazıyor. Doğru olan Color.kt).

Tema atamaları: `primary=Peach`, `onPrimary=Brown`, `secondary=Mint`, `onSecondary=Brown`, `tertiary=PeachDeep`, `surface=background=Cream`, `surfaceContainer*=CreamCard`, `error=ErrorRose`. Not: butonlarda metin `onPrimary`=Brown'dur (beyaz değil).

### Token dışı, kodda sabitlenmiş renkler (mevcut durum; yeni kodda KULLANMA, token'a taşı)
`#F6E3D6` pasif PeachButton zemini · `#FF8A5B` rozet noktası · `#9A8A83` DesignField placeholder · `#F8EDD7` LoginScreen dekor · `#8A7A73` LoginScreen ikincil metin (BrownHint ile aynı) · `#E3D5CB`/`#D9C9BE` Pairing çizgi/halka · `#F3D9C8` dolu OTP kenarlığı · `#F6EEE8` "Yakında" kart/hap zemini · `#E07A45` seri ateş ikonu · `#FF6F91` CatSection kalp tonu · `#4A3B35` gölge rengi (=Brown).

### Duygu (kalp atışı) renkleri
`ui/heartbeat/EmotionVisual.kt` içinde ham renkli `EmotionStyle(ana, açık zemin, koyu ton)`:

| Duygu | Ana | Açık | Koyu |
|---|---|---|---|
| Özlem | `#8EC5E8` | `#E3F1FA` | `#3F7FA8` |
| Sevgi | `#FFB38A` | `#FFE6D6` | `#E07A45` |
| Aşk | `#FF6F91` | `#FFE0E8` | `#D94470` |
| Sarılma | `#F5B95C` | `#FDEFD6` | `#C98A28` |
| Öpücük | `#F48FB1` | `#FCE4EE` | `#D0587F` |
| Huzur | `#7CCBB4` | `#DDF2EC` | `#3F8C75` |

---

## 3. Tipografi

Font: **Quicksand** (`Quicksand` FontFamily, `Type.kt`). Dosyalar `res/font/`: `quicksand_regular` (Normal 400), `quicksand_medium` (Medium 500), `quicksand_semibold` (SemiBold 600), `quicksand_bold` (Bold 700). Letter spacing hiçbir stilde tanımlı değil (varsayılan 0).

### Tema ölçeği (`LovelyyTypography`, `MaterialTheme.typography.*`)

| Stil | Boyut | Ağırlık | Satır yüksekliği | Kullanıldığı yer |
|---|---|---|---|---|
| displayLarge | 48sp | Bold | 56sp | doğrulanamadı (kullanım yok) |
| displayMedium | 40sp | Bold | 48sp | doğrulanamadı |
| displaySmall | 34sp | Bold | 42sp | doğrulanamadı |
| headlineLarge | 30sp | Bold | 38sp | doğrulanamadı |
| headlineMedium | 26sp | Bold | 34sp | doğrulanamadı |
| headlineSmall | 22sp | SemiBold | 30sp | `setup/SetupScreens` başlık |
| titleLarge | 20sp | Bold | 28sp | doğrulanamadı |
| titleMedium | 17sp | SemiBold | 24sp | doğrulanamadı |
| titleSmall | 15sp | SemiBold | 20sp | doğrulanamadı |
| bodyLarge | 16sp | Medium | 24sp | `setup` gövde |
| bodyMedium | 14sp | Medium | 20sp | `ErrorText` |
| bodySmall | 12sp | Medium | 16sp | doğrulanamadı |
| labelLarge | 16sp | Bold | 20sp | `PrimaryButton`/`SecondaryButton` metni |
| labelMedium | 13sp | SemiBold | 16sp | doğrulanamadı |
| labelSmall | 11sp | SemiBold | 14sp | doğrulanamadı |

### Ekranlarda fiilen kullanılan satır içi boyutlar
Yeni tasarımlı ekranlar (`Design.kt` ailesi) tema stilini değil `fontFamily=Quicksand, fontWeight, fontSize` ile satır içi kullanıyor. Gözlenen kalıplar:

| Rol | Boyut / ağırlık | Nerede |
|---|---|---|
| Hero başlık | 30sp Bold, satır 36 | Karşılama başlığı |
| Ekran başlığı | 28sp Bold (giriş) · 22sp Bold (`ScreenTitle`, logo yazısı) · 24sp Bold ("Yakında" başlığı) | LoginScreen, Design.kt, HomeParts |
| Bölüm başlığı | 18sp Bold | Home bölüm başlıkları |
| Kart başlığı | 16-17sp Bold | Nudge/Mood kartı, "Aklımdasın" |
| Gövde | 15-16sp Medium, satır 22-24 | Karşılama/giriş açıklaması |
| Kart gövdesi | 13sp SemiBold, satır 18 | Kart açıklamaları |
| Ana buton | 17sp Bold (PeachButton) · 16sp Bold (OutlineButton) · 15sp Bold (GhostButton) | Design.kt |
| Kart içi buton | 14sp Bold | Nudge/Mood küçük butonlar |
| Nav etiketi | 12sp, seçili Bold / değil SemiBold | `NavItem` |
| Büyük sayı | 26sp Bold (gün sayısı) | TogetherCard |
| OTP hanesi | 28sp Bold · tuş takımı 26sp Bold | PairingScreen |
| Hap / etiket | 13sp Bold (varsayılan), "Yakında" hapı 11sp | `Pill` |
| Alan metni | 16sp SemiBold | `DesignField` |
| Avatar harfi | boyutun %42'si Bold (`InitialAvatar`) | Components.kt |

Kural: yeni ekranda önce tema ölçeğine uy; satır içi boyut gerekiyorsa yukarıdaki kalıplardan birini kullan ve DESIGN.md'ye ekle.

---

## 4. Şekil / yarıçap

`Shape.kt` (`LovelyyShapes`, Material şekilleri):

| Token | Değer |
|---|---|
| extraSmall | 12dp |
| small | 16dp |
| medium | 20dp |
| large | 24dp |
| extraLarge | 32dp |

Bileşenlerde fiilen kullanılan yarıçaplar: PeachButton 20dp · OutlineButton 18dp · DesignField 18dp · GhostButton 16dp · SoftCard 24dp (varsayılan) · kedi hero kartı 28dp · alt nav üst köşeler 26dp · nav seçili hapı 15dp (52x30) · tuş takımı tuşu 20dp · OTP kutusu 16dp · kart içi buton 14dp · mood kutusu 18dp · tam yuvarlak (`CircleShape`/`RoundedCornerShape(50)`) avatar, ikon dairesi, hap, orta kalp. `PrimaryButton`/`SecondaryButton` (eski) `extraLarge` (32dp) kullanır.

Gölge: `Modifier.softShadow(shape, elevation=10.dp, tint=Brown)` (ambient alpha 0.10, spot alpha 0.16; tasarım karşılığı `0 6px 18px rgba(74,59,53,.06)`). Yaygın yükseklikler: 3-4dp (küçük), 10dp (kart), 14dp (hero/OTP kartı), 16dp (alt nav). Birincil butonda parlayan gölge: 14dp, ambient `Peach`, spot `PeachDeep`.

---

## 5. Boşluk ve yerleşim

Koddan gözlenen (resmi bir spacing token'ı yok; hepsi dp):

- **Ekran kenar boşluğu:** Yuva/Biz sekmeleri yatay **20dp**; giriş/eşleşme ekranları **24dp** (üst 16, alt 20); `NameScreen` ve `setup` **24-32dp**.
- **Kart iç boşluğu:** `SoftCard` varsayılan **18dp**; kompakt kartlar 14-16dp; TogetherCard 18x16dp.
- **Kartlar arası dikey ritim:** 14dp (Biz sekmesi `spacedBy(14)`), Yuva'da 14-18dp, bölüm başlığı üstü 24dp / altı 12dp.
- **Yan yana kartlar:** aralık 12dp; küçük çipler/butonlar 8-10dp.
- **Alt boşluk:** kaydırılan içerikte `bottom = 120dp` (alt nav + orta kalp için yer).
- **Bileşen yükseklikleri:** PeachButton **58dp** · OutlineButton 54dp · `PrimaryButton/SecondaryButton` 56dp · DesignField 56dp · kart içi buton 44dp · tuş takımı tuşu 60dp · alt nav 78dp (+ `navigationBarsPadding`) · orta kalp 64dp · bakım butonu dairesi 64dp.
- **Minimum dokunma hedefi: 48dp** (kural). `GhostButton` 48dp, `CircleIconButton` 44dp (dikkat: 44, 48'in altında; yeni butonlarda 48dp kullan, mevcut 44dp'ler tasarımdan gelir), nav öğesi satırı ≥ 52x30 ikon + etiket + 12dp dikey.
- **Güvenli alan:** üstte `statusBarsPadding()`, altta `navigationBarsPadding()`, hata/kurulum ekranlarında `safeDrawingPadding()`.
- **Kaydırma:** `verticalScroll`; ana içerik tek sütun, `fillMaxWidth`.
- **Ana eylem alt yarıda.** Genişlik sınırı: uzun gövde metni `widthIn(max=300dp)` (eşleşme açıklaması).

---

## 6. Bileşenler

Hepsi `ui/components/` altında. Yeni UI için `Design.kt` ailesi (onaylı tasarım) tercih edilir; `Components.kt`'deki eski M3 sarmalayıcılar (`PrimaryButton`, `SecondaryButton`, `LovelyyTextField`) hâlâ var.

| Bileşen | Fonksiyon | Davranış |
|---|---|---|
| Ana buton | `PeachButton(text, onClick, enabled, loading, height=58.dp, leading)` | Şeftali zemin, Brown 17sp Bold metin, 20dp yarıçap, parlayan gölge. Pasifken `#F6E3D6` + 0.8 alpha, gölgesiz. `loading` iken 22dp Brown spinner. Basınca 0.97 ölçek + `LongPress` haptiği. Ekran başına tek |
| Eski ana buton | `PrimaryButton` | M3 Button, 56dp, `extraLarge`, Peach, yük/spinner, 0.96 ölçek + `LongPress` |
| İkincil / çerçeveli | `OutlineButton(text, onClick, enabled, loading)` | Beyaz zemin, 1.5dp Brown kenar, 54dp, 18dp yarıçap, 0.97 ölçek, pasif 0.5 alpha |
| Eski ikincil | `SecondaryButton` | M3 OutlinedButton, 56dp, Outline kenarlık, `leading` slot'u, `TextHandleMove` haptiği |
| Metin butonu | `GhostButton(text, onClick, color=Brown, content)` | 48dp, 16dp yarıçap, 15sp Bold ("Zaten hesabım var · Giriş yap") |
| Yuvarlak ikon butonu | `CircleIconButton(icon, contentDescription, onClick, size=44.dp, badge)` | Beyaz daire, 4dp yumuşak gölge, 22dp ikon, isteğe bağlı `#FF8A5B` rozet noktası |
| Geri | `BackButton(onClick)` | `CircleIconButton` + `KeyboardArrowLeft`, açıklama "Geri" |
| Ekran başlığı | `ScreenTitle(title, onBack, action)` | Geri + 22sp Bold başlık satırı, üstten 8dp |
| Metin alanı | `DesignField(value, onValueChange, placeholder, leadingIcon, trailing, ...)` | 56dp, beyaz, 18dp, 1.5dp kenar (odakta `Peach`, değilse `Outline`), solda 20dp `BrownHint` ikon, 16sp SemiBold, imleç `PeachDeep`, kendi placeholder'ı |
| Eski alan | `LovelyyTextField` | M3 OutlinedTextField, `medium` şekil, odak `primary` |
| Kart | `SoftCard(color=CreamCard, radius=24.dp, padding=18.dp, onClick)` | Beyaz kart, yalnız beyazken yumuşak gölge; `onClick` verilirse 0.98 ölçekli bounce |
| İkon dairesi | `IconCircle(icon, background, size=40.dp, tint=Brown, iconSize=22.dp)` | Tinted yuvarlak ikon rozeti (`PeachTint` vb.) |
| Hap / çip | `Pill(text, background=CreamCard, color=Brown, fontSize=13)` | Tam yuvarlak, 12x6dp iç boşluk, tek satır ("Yakında", "Son adım") |
| Avatar | `InitialAvatar(name, color, size=48.dp)` | Yuvarlak, baş harf büyük, Bold, boyutun %42'si; fotoğraf sonra |
| Atan kalp | `BeatingHeart(size=72.dp, color=primary)` | 1.0-1.12 ölçek, 700ms `FastOutSlowIn`, Reverse; logo yerine |
| Hata metni | `ErrorText(error: AppError?)` | `error` rengi, `bodyMedium`; `AppError.messageRes()` ile Türkçe metin |
| Basma efekti | `Modifier.bounceClick(enabled, pressedScale=0.94f, haptic=TextHandleMove)` | Ripple yok; yaylı (MediumBouncy / Medium) ölçek + haptik. TÜM dokunulabilir şeylerde bu kullanılır |
| Gölge | `Modifier.softShadow(shape, elevation=10.dp, tint)` | Bölüm 4 |
| Boşluk | `Gap(h)` | `Spacer(height)` |

Ekrana özel bileşenler (özel dosyalarda, `private`):

- **Alt navigasyon** (`HomeScreen.kt: BottomNav/NavItem`): beyaz panel, üst köşeler 26dp, 78dp, 16dp gölge. Sıra: **Yuva · Anılar · [orta kalp] · Planlar · Biz**. Öğe = 52x30 hap (seçili `PeachSoft`) içinde 22dp ikon + 12sp etiket; seçili ikon/etiket `Brown`, değilse `BrownSoft`. İkonlar: Home, Photo, CalendarMonth, Groups (Rounded). Orta kalp: 64dp `Peach` daire, 4dp `Cream` kenar, 28dp Brown kalp, kalp atışı animasyonu (1800ms keyframe, 1.12/1.08 tepeleri), "kalp atışı gönder" eylemi. Sekme değişimi `fadeIn/fadeOut`; Yuva dışında geri tuşu Yuva'ya döner.
- **Kedi hero** (`ui/cat/CatSection.kt`): 28dp köşeli `CatBg` kartı, yumuşak gölge, ismi/seri çipi (ateş ikonu), konuşma balonu (4s'lik süzülme), kediyi sevme.
- **İhtiyaç barları** (`NeedsRow/NeedMeter`): 4 bar, eşit ağırlık, aralık 10dp, bar yüksekliği **8dp**, iz `Sand`; dolgu `NeedOk` (normal) / `NeedLow` (düşük, 700ms nabız); değer değişimi `spring(LowBouncy, Low)`. Etiketler: **Tokluk · Su · Enerji · Sevgi**.
- **Bakım butonları** (`CareButton`): 64dp daire (ikon 28dp) + 14sp Bold etiket altta. Sıra: **Besle · Su ver · Oyna · Uyut** (uyuyorsa "Uyandır"). Basınca 0.88 ölçek (MediumBouncy), pasif 0.45 alpha, en çok ihtiyaç duyulan eylem `Peach` zemin + Peach gölge ile vurgulanır.
- **Birlikte kartı** (`TogetherCard`): "N gündür birliktesiniz" sayacı (26sp Bold), tarih yoksa "Tanıştığınız günü ekleyin".
- **Aklımdasın / Dürt** (`HomeParts.kt: NudgeCard`): SoftCard 16dp, ikon dairesi, başlık, açıklama, 44dp / 14dp yarıçaplı buton (hazır `Peach`, gönderildi `PeachTint`), 8sn bekleme süresi.
- **Ruh hali kartı** (`MoodCard`): iki kişinin ruh hali kutuları (18dp, 12dp iç boşluk), "Paylaş/Değiştir". Not: kutuda `kind.emoji` kullanılıyor (tuval kuralıyla çelişir; bkz. bölüm 9).
- **Eşleşme OTP kutuları** (`PairingScreen`): 46x60dp, 16dp, dolu `PeachTint`, aktif `Peach` 2.5dp kenar + 6dp yarı saydam halka, dolunca "pop" ölçeği; 6 haneli kod, 3. haneden sonra boşluk.
- **Tuş takımı** (`NumberPad`): 3x3 + alt satır (boş, 0, silme), tuşlar 60dp yükseklik / 20dp, beyaz + 3dp gölge, silme tuşu `Cream` düz; aralık 10dp; rakam 26sp Bold; basınca 0.92 ölçek.
- **Kod kartı** (`CodeCard`): beyaz 24dp kart, 14dp gölge, "EŞLEŞME KODUN" etiketi, kopyala butonu 44dp / 14dp (`MintInk` ikon), süre sayacı (`Schedule` ikonu), "bekleniyor" Mint nabız noktası.
- **Çift avatarlar** (`CoupleAvatars`): iki 84dp avatar, aralarında kesikli çizgi + 36dp atan kalp.
- **Yakında yer tutucu** (`ComingSoon`): 96dp `PeachTint` ikon dairesi (ikon 44dp), 24sp Bold başlık, 15sp Medium `BrownSoft` açıklama, ortalı.

---

## 7. Hareket ve haptik

Mikro animasyon ilkesi: her şey hafifçe canlı, hiçbiri dikkat dağıtmaz.

| Animasyon | Değer | Yer |
|---|---|---|
| Basınca ezilme (genel) | `bounceClick` 0.94 (`spring MediumBouncy, Medium`) | tüm dokunulabilirler |
| Ana buton basma | 0.97 | `PeachButton`, `OutlineButton` |
| Kart basma | 0.98 | `SoftCard(onClick)` |
| Tuş takımı | 0.92 | `Key` |
| Bakım butonu | 0.88 | `CareButton` |
| Atan kalp (logo) | 1.0→1.12, 700ms, `FastOutSlowIn`, Reverse | `BeatingHeart` |
| Orta kalp (nav) | 1800ms keyframe: 1 → 1.12 (270ms) → 1 → 1.08 (810ms) → 1 | `BottomNav` |
| İhtiyaç barı değişimi | `spring(LowBouncy, Low)` | `NeedMeter` |
| Düşük bar nabzı | 700ms `FastOutSlowIn`, Reverse | `NeedMeter` |
| Konuşma balonu süzülmesi | 0→-4dp, 1500ms, Reverse | `CatSection` |
| Yükselen kalpler | 1100ms (`tween FastOutSlowIn`), 120dp yükselme / 24dp salınım | `CatSection`; giriş ekranında `RisingHeart` |
| Ekran/sekme geçişi | `fadeIn` + `fadeOut` (varsayılan süre) | `LovelyyRoot`, `HomeScreen` sekmeleri |
| Arka plan nabzı (eşleşme) | 1600ms keyframe, 1800ms lineer döngü | `PairingScreen` |

Haptik:
- `PeachButton`, `PrimaryButton`: `HapticFeedbackType.LongPress` (ana eylem = belirgin).
- Diğer `bounceClick` (varsayılan), `SecondaryButton`: `TextHandleMove` (hafif tık).
- Tuş takımı: `TextHandleMove` (tuşta `haptic=null`, tıklamada elle verilir).
- Kedi: bakım eylemi `LONG_PRESS`, başarı `CONFIRM` (API 30+, değilse `KEYBOARD_TAP`), kedi dokunuşu `KEYBOARD_TAP`, sevme çizgisi `CLOCK_TICK`.
- Kalp atışı/dürtme sinyali: `SignalCenter.vibrate` (kalp atışı "dum-dum" dalga biçimi); bildirimde `HEARTBEAT_PATTERN`.

---

## 8. İkonografi

- **Kod:** Material Icons `Rounded` ailesi (`Icons.Rounded.*`: Home, Photo, CalendarMonth, Groups, TouchApp, Schedule, ContentCopy, Done, Share, Backspace (AutoMirrored), KeyboardArrowLeft (AutoMirrored)); kalp `Icons.Filled.Favorite` / `Rounded.Favorite`; kedi ekranında `Filled` (Edit, LocalFireDepartment, WbSunny, Bedtime).
- **Boyutlar:** liste/nav 22dp · satır içi küçük 16-20dp · kart ikonu 22dp · bakım butonu 28dp · orta kalp 28dp · "Yakında" büyük 44dp (96dp dairede).
- **Renk:** ikon `Brown` (aktif) / `BrownSoft` (pasif) / `BrownHint` (alan içi); ikon dairesinde zemin `PeachTint`/`MintTint`.
- **Tuval:** emoji yok, yüzler ve ikonlar SVG. Yeni ikon önce tuvalde SVG olarak çizilir, kodda en yakın Rounded Material ikonu ya da vektör kullanılır.
- Kedi çizimi `ui/cat/` (CatAnimator, CatClip) ile hazırlanmış animasyonlu; karşılama ekranında "oturmuş şeftali renkli tekir kedi".

---

## 9. Metin ve ses tonu

Türkçe, sıcak, "biz" odaklı, kısa. Kaynak: `res/values/strings.xml`. Tüm kullanıcı metni `strings.xml`'e girer (sabit kodlanmaz).

- **Hitap:** sevgilisine "sen" (tekil) ama ortak sahiplikte "siz/ikiniz/kediniz": "Kedinize bir isim verin", "Bugün nasılsınız?", "Bizim köşemiz", "İkinizin minik dünyası".
- **Sıcak ve doğrudan:** "Hoş geldin", "Başlayalım", "Sevgilini davet et", "Basılı tut, X kalbinin atışını hissetsin.", "Aklımdasın".
- **Başlıklar kısa**; eylemler fiil: Besle, Su ver, Oyna, Uyut, Dürt, Paylaş, Kaydet, Vazgeç, Eşleş, Devam et, Kopyala.
- **Hata:** suçlamaz, çözüm önerir: "İnternet bağlantısı yok gibi görünüyor.", "Bağlantı sorunu oldu, tekrar dener misin?".
- **Henüz yapılmamış özellik:** `Yakında` hapı ya da toast "Bu köşe sonraki adımlarda geliyor". Bu bir hata değildir; sıcak tonda kalır.
- **Durum satırları:** "Bugün henüz ilgilenmedin", "Sen bugün ilgilendin, X bekleniyor", "Bugün ikiniz de ilgilendiniz, seri büyüdü".
- **Zaman:** göreli ("az önce", "5 dk önce"); tarih `d MMMM yyyy`, `tr` yerel ayarı.
- **Yazım:** Sentence case, "·" ayraç, "…" üç nokta karakteri, tırnak “ ”.
- **Emoji notu:** tuval kuralı "emoji yok". Mevcut kodda `strings.xml`'de bazı emojiler var (💛 💭 ✓ `home_soon_toast`, `nudge_sent`, `pair_share_text` vb.) ve ruh hali girişlerinde `MoodKind.emoji`. Bu mevcut durum; yeni metinlerde emoji kullanma, ikon/SVG tercih et. Mevcutlar ayrı iş olarak ele alınabilir (karar Ulaş'ta).

---

## 10. Ekran envanteri

Uygulama `AppState` ile gezinir (NavController yok): `LovelyyRoot.kt`. `screen_view` adı `analytics/Screens.kt`'deki sabit rota adıdır.

> Durum notu: CLAUDE.md'ye göre onaylı ve yapılmış olanlar yalnızca Karşılama, Giriş, Eşleşme ve Anasayfa'dır (kedi dahil); diğer kartlar ve sekmeler (Yuva dışı) "Yakında"dır. Aşağıdaki tabloda Biz sekmesi, Dürt ve Ruh hali kartları için kodda bulunan (onaysız olabilir) kalıntılar not edildi; çelişkide CLAUDE.md geçerlidir.

### Kodda var

| Ekran | Dosya | Analytics adı | Durum |
|---|---|---|---|
| Yükleme | `LovelyyRoot.LoadingScreen` | (loglanmaz) | Yapıldı |
| Firebase kurulum | `setup/SetupScreens.kt` | `firebase_setup` | Yapıldı |
| Hata | `setup/SetupScreens.kt` | `error` | Yapıldı |
| Karşılama + Giriş/Kayıt | `auth/LoginScreen.kt` | `login` | Yapıldı (tuval 1, 2) |
| İsim | `profile/NameScreen.kt` | `name_setup` | Yapıldı |
| Eşleşme (kodu paylaş / kodu gir) | `pairing/PairingScreen.kt` | `pairing` | Yapıldı (tuval 3a, 3b) |
| Ana sayfa / Yuva sekmesi (kedi, ihtiyaç barları, bakım, birlikte kartı, dürtme, ruh hali, kalp atışı gelen kutusu) | `home/HomeScreen.kt`, `cat/*` | `home` | Yapıldı; bazı kartlar henüz `Yakında` toast'ı gösterebilir (hangi kartın bağlı olduğu kodda `soon` parametresiyle geçiliyor; tek tek doğrulanamadı) |
| Biz sekmesi | `HomeScreen.kt: BizTab` | `home` | Kısmen: "X & Y" başlığı, çıkış, kart listesi; ayrıntı doğrulanamadı |
| Anılar sekmesi | `HomeScreen` | `home` | **Yakında** (`ComingSoon`) |
| Planlar sekmesi | `HomeScreen` | `home` | **Yakında** (`ComingSoon`) |
| Orta kalp (kalp atışı gönder) | `BottomNav` | - | **Yakında** (`onHeart = soon`); görseller (`heartbeat/EmotionVisual.kt`) hazır |

### Tuvalde tasarlanmış (kodda yok ya da kısmi; onay/durum kodda doğrulanamadı)
Onaylı tuval panoları: 1 Karşılama · 2 Giriş · 3a/3b Eşleşme · 4 Anasayfa (ilk açılış + aşağı kaydırınca) · 5 Gün sayacı · 6 Kalp atışı + dürtme · 7 Ruh hali · 8 Günün sorusu · 9 Mesafe · 10 Biz sekmesi (profil + ayarlar) · 11 Planlar sekmesi (takvim + özel günler) · 12 Hayaller listesi · 13 Karar çarkı · 14 Kedi bakımı · 15 Ana ekran widget'ları · 16 Anılar sekmesi · 17 Canlı çizim · 18 Notlar · 19 Zaman kapsülü.

Koddaki karşılığı olmayanlar (tasarlandı, kodlanmayı bekliyor): 5, 8, 9, 11, 12, 13, 16, 17, 18, 19 (ve 10/14'ün tam hâli). 15 için `widget/` paketi var; kapsamı doğrulanamadı. Tuvaldeki her panonun "onaylandı" bilgisi kodda yok; onay durumu Ulaş'tan teyit edilmeli (**doğrulanamadı**).

Tuvaldeki alt menü notu: Yuva (kedi + özet) · Anılar (canlı çizim, notlar, zaman kapsülü) · ortadaki kalp (tek dokunuşla kalp atışı) · Planlar (takvim, hayaller, karar çarkı) · Biz (ruh hali, mesafe, günlük soru, profil).

---

## 11. Süreç kuralı ve yeni ekran kontrol listesi

**Süreç (zorunlu):** Her yeni ekran önce **Fable (claude-fable-5-2)** tarafından tuvalde (artifact) bir pano olarak tasarlanır ve **Ulaş tarafından onaylanır**; onaydan ÖNCE kodlanmaz. Onay yoksa ilgili kart/sekme `Yakında` olarak kalır.

Tuval kuralları: 390x844 pano, emoji yok (SVG ikon/yüz), alt menü her panoda kopyalanır, bu dosyadaki token'larla tutarlı.

**Yeni ekran kontrol listesi**
- [ ] Tuvalde tasarlandı ve Ulaş onayladı (pano adı not edildi).
- [ ] Renkler yalnızca token'lardan (`MaterialTheme.colorScheme` veya `ui/theme/Color.kt` sembolleri); hiçbir ham `Color(0x...)` yok. Gerekirse yeni token `Color.kt`'e ve bu dosyaya eklenir.
- [ ] Tipografi tema ölçeğinden ya da bölüm 3'teki kalıplardan; font Quicksand.
- [ ] Köşeler `LovelyyShapes` veya bölüm 4'teki değerlerden; gölge `softShadow`.
- [ ] Ekran başına tek ana eylem (`PeachButton`), alt yarıda, tek elle ulaşılır.
- [ ] Dokunma hedefleri ≥ 48dp; ikon-only butonlarda `contentDescription`.
- [ ] Dokunulabilirler `bounceClick`/bileşen kütüphanesiyle (ezilme + haptik); ana eylemde `LongPress`.
- [ ] Hazır bileşenler (`ui/components`) kullanıldı; tekrar yazılmadı.
- [ ] Metinler `strings.xml`'de, Türkçe, sıcak ton; emoji yok.
- [ ] Yüklenme / hata (`ErrorText`) / boş durum düşünüldü.
- [ ] Güvenli alan (`statusBarsPadding`, `navigationBarsPadding`) ve 20-24dp kenar boşluğu.
- [ ] Analytics ekran adı: `AppState` durumu eklendi ve `analytics/Screens.kt`'e sabit rota adı (`snake_case`, dinamik veri yok) yazıldı; olaylar `docs/analytics.md` kurallarına göre.
- [ ] Bu dosyadaki envanter (bölüm 10) güncellendi.

**Not (token disiplini):** `docs/design-principles.md` "ekranlar yalnızca `MaterialTheme.colorScheme` kullanır" der, ama mevcut `Design.kt` ailesi `Brown`, `Peach` gibi tema sembollerini doğrudan import eder. Pratikte kabul edilen kural: ham hex yok, yalnızca `ui/theme` sembolleri/`colorScheme`. Koyu tema eklenirse bu doğrudan kullanımların `colorScheme`'e taşınması gerekir.
