# Lovelyy Design System

This file is the single source of truth for Lovelyy's CURRENT design system (check here first for UI work). Values were extracted from the code. Anything that could not be confirmed in the code is marked "unverified".

Sources (`android/app/src/main/`):
`java/com/lovelyy/app/ui/theme/{Color,Shape,Theme,Type}.kt`, `ui/components/{Components,Design}.kt`, `ui/{auth,pairing,home,cat,profile,setup,heartbeat}`, `res/values/{colors,themes,strings}.xml`, `res/font/quicksand_*.ttf`, `docs/design-principles.md`, and the approved design canvas "Lovelyy ekran tasarımları" (Lovelyy screen designs; claude.ai artifact FzNY4Jh2r7cyjayPKBFZk8; only the board list/notes were read, the board HTMLs were not).

---

## 1. Principles

1. **Soft and round.** Corners 12-32dp (cards 24-28dp, buttons 18-20dp, pill shapes). No hard edges, no hard shadows.
2. **Cat at the center.** The home page (Yuva, "Nest") always opens with the cat; everything is arranged around it.
3. **A "we" feeling.** The two people appear together (paired avatars, "Ulaş & Deniz", "Bizim köşemiz" / "Our corner").
4. **One main action per screen.** A single large peach button; everything else is quieter.
5. **Lively but gentle.** Micro animations (breathing, beating heart, squish on press) and a haptic tick on touches.
6. **One-handed use.** Main actions sit in the lower half of the screen, within thumb reach.
7. **Material 3 underneath.** Standard components, with our tokens.
8. **v1 is light theme only.** No dark theme (it can be added in the single file `Theme.kt`; screens must not use raw colors, see the note in section 11).

Canvas rules (approved designs): 390x844 boards (corner radius 36), no emoji (SVG icons/faces), the bottom menu is copied onto every board.

---

## 2. Color tokens

Source: `ui/theme/Color.kt`. Theme mapping: `ui/theme/Theme.kt` (`lightColorScheme`).

| Name | Hex | Compose symbol | Usage |
|---|---|---|---|
| Peach | `#FFB38A` | `Peach` | Primary: main button, selected/active accent, center heart button |
| PeachDeep | `#E8895C` | `PeachDeep` | Pressed/accent, cursor, `tertiary` |
| PeachSoft | `#FFE6D6` | `PeachSoft` | Tinted surface, selected nav pill, `primaryContainer`, `surfaceVariant` |
| PeachTint | `#FFF1E8` | `PeachTint` | Code boxes, icon circles, filled OTP box, inactive secondary button background |
| PeachCard | `#FFE9DB` | `PeachCard` | "Birlikte" (Together) card |
| Mint | `#9ED9C8` | `Mint` | Secondary: calm/fresh accent, "waiting" dot, `secondary` |
| MintSoft | `#DDF2EC` | `MintSoft` | Mint surface, `secondaryContainer` |
| MintTint | `#E4F5F0` | `MintTint` | Mint card background (lightest) |
| MintInk | `#2F5A4E` | `MintInk` | Text/icon on mint |
| MintInkSoft | `#4F6B63` | `MintInkSoft` | Secondary text inside mint cards |
| Brown | `#4A3B35` | `Brown` | Text (`onBackground`, `onSurface`, `onPrimary`) |
| BrownSoft | `#75655E` | `BrownSoft` | Secondary text (4.5:1 on cream), `onSurfaceVariant` |
| BrownHint | `#8A7A73` | `BrownHint` | Icons, placeholder tone |
| Cream | `#FFF8F2` | `Cream` | Background (`background`, `surface`) |
| CreamCard | `#FFFFFF` | `CreamCard` | Cards on cream (`surfaceContainer*`) |
| CatBg | `#FAE2C0` | `CatBg` | Behind the cat (hero card background) |
| Outline | `#EADFD6` | `Outline` | Border, divider line (`outline`, `outlineVariant`) |
| Sand | `#F3ECE6` | `Sand` | Empty avatar, bar track |
| NeedLow | `#F07B45` | `NeedLow` | Need bar, low level |
| NeedOk | `#5DB89E` | `NeedOk` | Need bar, normal level |
| ErrorRose | `#D9534F` | `ErrorRose` | Error (`error`) |
| StageMint | `#D3EFE7` | `StageMint` | Onboarding split stage: the partner's side |
| StagePeach | `#FFDCC6` | `StagePeach` | Onboarding split stage: your side |
| HeartInk | `#E0602B` | `HeartInk` | Onboarding: your drawn stroke, flying heart |
| MintDeep | `#2F8F7A` | `MintDeep` | Onboarding: the partner's drawn stroke |
| CatStripStart / CatStripEnd | `#F3EAD1` / `#EADAC0` | `CatStripStart`, `CatStripEnd` | Cat card v2: horizontal gradient of the strip above the video |
| FlameInk | `#E07A45` | `FlameInk` | Streak flame outline (fill `Peach`) |
| CheckInk | `#2F7F69` | `CheckInk` | "Cared today" check on the Kedi bakımı streak card |
| SheetHandle | `#E3D5CB` | `SheetHandle` | Bottom sheet drag handle (44x5) |
| BrownMuted | `#5F524C` | `BrownMuted` | Small text on tinted (peach) surfaces |

`res/values/colors.xml` (only for window/splash background): `peach #FFB38A`, `mint #9ED9C8`, `brown #4A3B35`, `cream #FFF8F2`, `brown_soft #8A7A72` (note: DIFFERENT from `BrownSoft #75655E` in Color.kt; `docs/design-principles.md` also lists the old `#8A7A72` value. Color.kt is correct).

Theme assignments: `primary=Peach`, `onPrimary=Brown`, `secondary=Mint`, `onSecondary=Brown`, `tertiary=PeachDeep`, `surface=background=Cream`, `surfaceContainer*=CreamCard`, `error=ErrorRose`. Note: text on buttons is `onPrimary`=Brown (not white).

### Non-token colors hardcoded in code (current state; do NOT use in new code, move to a token)
`#F6E3D6` inactive PeachButton background · `#FF8A5B` badge dot · `#9A8A83` DesignField placeholder · `#F8EDD7` LoginScreen decoration · `#8A7A73` LoginScreen secondary text (same as BrownHint) · `#E3D5CB`/`#D9C9BE` Pairing line/ring · `#F3D9C8` filled OTP border · `#F6EEE8` "Yakında" (Coming soon) card/pill background · `#E07A45` streak fire icon (now `FlameInk`; the wheel still hardcodes it) · `#FF6F91` CatSection heart tone · `#4A3B35` shadow color (=Brown).

### Emotion (heartbeat) colors
In `ui/heartbeat/EmotionVisual.kt`, raw-colored `EmotionStyle(main, light background, dark tone)`:

| Emotion | Main | Light | Dark |
|---|---|---|---|
| Özlem (Longing) | `#8EC5E8` | `#E3F1FA` | `#3F7FA8` |
| Sevgi (Love) | `#FFB38A` | `#FFE6D6` | `#E07A45` |
| Aşk (Passion) | `#FF6F91` | `#FFE0E8` | `#D94470` |
| Sarılma (Hug) | `#F5B95C` | `#FDEFD6` | `#C98A28` |
| Öpücük (Kiss) | `#F48FB1` | `#FCE4EE` | `#D0587F` |
| Huzur (Peace) | `#7CCBB4` | `#DDF2EC` | `#3F8C75` |

---

## 3. Typography

Font: **Quicksand** (`Quicksand` FontFamily, `Type.kt`). Files in `res/font/`: `quicksand_regular` (Normal 400), `quicksand_medium` (Medium 500), `quicksand_semibold` (SemiBold 600), `quicksand_bold` (Bold 700). No style defines letter spacing (default 0).

### Theme scale (`LovelyyTypography`, `MaterialTheme.typography.*`)

| Style | Size | Weight | Line height | Where used |
|---|---|---|---|---|
| displayLarge | 48sp | Bold | 56sp | unverified (no usage) |
| displayMedium | 40sp | Bold | 48sp | unverified |
| displaySmall | 34sp | Bold | 42sp | unverified |
| headlineLarge | 30sp | Bold | 38sp | unverified |
| headlineMedium | 26sp | Bold | 34sp | unverified |
| headlineSmall | 22sp | SemiBold | 30sp | `setup/SetupScreens` title |
| titleLarge | 20sp | Bold | 28sp | unverified |
| titleMedium | 17sp | SemiBold | 24sp | unverified |
| titleSmall | 15sp | SemiBold | 20sp | unverified |
| bodyLarge | 16sp | Medium | 24sp | `setup` body |
| bodyMedium | 14sp | Medium | 20sp | `ErrorText` |
| bodySmall | 12sp | Medium | 16sp | unverified |
| labelLarge | 16sp | Bold | 20sp | `PrimaryButton`/`SecondaryButton` text |
| labelMedium | 13sp | SemiBold | 16sp | unverified |
| labelSmall | 11sp | SemiBold | 14sp | unverified |

### Inline sizes actually used on screens
The newly designed screens (the `Design.kt` family) do not use theme styles; they use `fontFamily=Quicksand, fontWeight, fontSize` inline. Observed patterns:

| Role | Size / weight | Where |
|---|---|---|
| Hero title | 30sp Bold, line 36 | Karşılama (Welcome) title |
| Screen title | 28sp Bold (login) · 22sp Bold (`ScreenTitle`, logo text) · 24sp Bold ("Yakında" title) | LoginScreen, Design.kt, HomeParts |
| Section title | 18sp Bold | Home section titles |
| Card title | 16-17sp Bold | Nudge/Mood card, "Aklımdasın" |
| Body | 15-16sp Medium, line 22-24 | Welcome/login description |
| Card body | 13sp SemiBold, line 18 | Card descriptions |
| Main button | 17sp Bold (PeachButton) · 16sp Bold (OutlineButton) · 15sp Bold (GhostButton) | Design.kt |
| In-card button | 14sp Bold | Nudge/Mood small buttons |
| Nav label | 12sp, selected Bold / otherwise SemiBold | `NavItem` |
| Large number | 26sp Bold (day count) | TogetherCard |
| OTP digit | 28sp Bold · keypad 26sp Bold | PairingScreen |
| Pill / label | 13sp Bold (default), "Yakında" pill 11sp | `Pill` |
| Field text | 16sp SemiBold | `DesignField` |
| Avatar letter | 42% of size, Bold (`InitialAvatar`) | Components.kt |

Rule: on a new screen, follow the theme scale first; if an inline size is needed, use one of the patterns above and add it to DESIGN.md.

---

## 4. Shape / radius

`Shape.kt` (`LovelyyShapes`, Material shapes):

| Token | Value |
|---|---|
| extraSmall | 12dp |
| small | 16dp |
| medium | 20dp |
| large | 24dp |
| extraLarge | 32dp |

Radii actually used in components: PeachButton 20dp · OutlineButton 18dp · DesignField 18dp · GhostButton 16dp · SoftCard 24dp (default) · cat hero card 28dp · bottom nav top corners 26dp · nav selected pill 15dp (52x30) · keypad key 20dp · OTP box 16dp · in-card button 14dp · mood box 18dp · fully round (`CircleShape`/`RoundedCornerShape(50)`) avatar, icon circle, pill, center heart. `PrimaryButton`/`SecondaryButton` (legacy) use `extraLarge` (32dp).

Shadow: `Modifier.softShadow(shape, elevation=10.dp, tint=Brown)` (ambient alpha 0.10, spot alpha 0.16; design equivalent `0 6px 18px rgba(74,59,53,.06)`). Common elevations: 3-4dp (small), 10dp (card), 14dp (hero/OTP card), 16dp (bottom nav). Glowing shadow on the primary button: 14dp, ambient `Peach`, spot `PeachDeep`.

---

## 5. Spacing and layout

Observed from code (there is no official spacing token; all values in dp):

- **Screen edge margin:** Yuva/Biz tabs horizontal **20dp**; login/pairing screens **24dp** (top 16, bottom 20); `NameScreen` and `setup` **24-32dp**.
- **Card inner padding:** `SoftCard` default **18dp**; compact cards 14-16dp; TogetherCard 18x16dp.
- **Vertical rhythm between cards:** 14dp (Biz tab `spacedBy(14)`), 14-18dp on Yuva, 24dp above / 12dp below a section title.
- **Side-by-side cards:** gap 12dp; small chips/buttons 8-10dp.
- **Bottom spacing:** `bottom = 120dp` in scrolling content (room for the bottom nav + center heart).
- **Component heights:** PeachButton **58dp** · OutlineButton 54dp · `PrimaryButton/SecondaryButton` 56dp · DesignField 56dp · in-card button 44dp · keypad key 60dp · bottom nav 78dp (+ `navigationBarsPadding`) · center heart 64dp · care button circle 64dp.
- **Minimum touch target: 48dp** (rule). `GhostButton` 48dp, `CircleIconButton` 44dp (caution: 44 is below 48; use 48dp in new buttons, existing 44dp ones come from the design), nav item row ≥ 52x30 icon + label + 12dp vertical.
- **Safe area:** `statusBarsPadding()` at the top, `navigationBarsPadding()` at the bottom, `safeDrawingPadding()` on error/setup screens.
- **Scrolling:** `verticalScroll`; main content is a single column, `fillMaxWidth`.
- **Main action in the lower half.** Width limit: long body text `widthIn(max=300dp)` (pairing description).

---

## 6. Components

All under `ui/components/`. For new UI, prefer the `Design.kt` family (approved design); the legacy M3 wrappers in `Components.kt` (`PrimaryButton`, `SecondaryButton`, `LovelyyTextField`) still exist.

| Component | Function | Behavior |
|---|---|---|
| Main button | `PeachButton(text, onClick, enabled, loading, height=58.dp, leading)` | Peach background, Brown 17sp Bold text, 20dp radius, glowing shadow. When disabled `#F6E3D6` + 0.8 alpha, no shadow. When `loading`, a 22dp Brown spinner. On press 0.97 scale + `LongPress` haptic. One per screen |
| Legacy main button | `PrimaryButton` | M3 Button, 56dp, `extraLarge`, Peach, loading/spinner, 0.96 scale + `LongPress` |
| Secondary / outlined | `OutlineButton(text, onClick, enabled, loading)` | White background, 1.5dp Brown border, 54dp, 18dp radius, 0.97 scale, disabled 0.5 alpha |
| Legacy secondary | `SecondaryButton` | M3 OutlinedButton, 56dp, Outline border, `leading` slot, `TextHandleMove` haptic |
| Text button | `GhostButton(text, onClick, color=Brown, content)` | 48dp, 16dp radius, 15sp Bold ("Zaten hesabım var · Giriş yap" / "I already have an account · Log in") |
| Round icon button | `CircleIconButton(icon, contentDescription, onClick, size=44.dp, badge)` | White circle, 4dp soft shadow, 22dp icon, optional `#FF8A5B` badge dot |
| Back | `BackButton(onClick)` | `CircleIconButton` + `KeyboardArrowLeft`, description "Geri" (Back) |
| Screen title | `ScreenTitle(title, onBack, action)` | Back + 22sp Bold title row, 8dp from top |
| Text field | `DesignField(value, onValueChange, placeholder, leadingIcon, trailing, ...)` | 56dp, white, 18dp, 1.5dp border (`Peach` when focused, otherwise `Outline`), 20dp `BrownHint` icon on the left, 16sp SemiBold, cursor `PeachDeep`, own placeholder |
| Legacy field | `LovelyyTextField` | M3 OutlinedTextField, `medium` shape, focus `primary` |
| Card | `SoftCard(color=CreamCard, radius=24.dp, padding=18.dp, onClick)` | White card, soft shadow only when white; if `onClick` is given, 0.98-scale bounce |
| Icon circle | `IconCircle(icon, background, size=40.dp, tint=Brown, iconSize=22.dp)` | Tinted round icon badge (`PeachTint` etc.) |
| Pill / chip | `Pill(text, background=CreamCard, color=Brown, fontSize=13)` | Fully round, 12x6dp inner padding, single line ("Yakında", "Son adım" / "Last step") |
| Avatar | `InitialAvatar(name, color, size=48.dp)` | Round, large initial letter, Bold, 42% of size; photo later |
| Beating heart | `BeatingHeart(size=72.dp, color=primary)` | 1.0-1.12 scale, 700ms `FastOutSlowIn`, Reverse; used in place of a logo |
| Error text | `ErrorText(error: AppError?)` | `error` color, `bodyMedium`; Turkish text via `AppError.messageRes()` |
| Press effect | `Modifier.bounceClick(enabled, pressedScale=0.94f, haptic=TextHandleMove)` | No ripple; springy (MediumBouncy / Medium) scale + haptic. Used on ALL touchable things |
| Shadow | `Modifier.softShadow(shape, elevation=10.dp, tint)` | Section 4 |
| Spacing | `Gap(h)` | `Spacer(height)` |

Screen-specific components (in dedicated files, `private`):

- **Bottom navigation** (`HomeScreen.kt: BottomNav/NavItem`): white panel, top corners 26dp, 78dp, 16dp shadow. Order: **Yuva · Anılar · [center heart] · Planlar · Biz** (Nest · Memories · [center heart] · Plans · Us). Item = 22dp icon + 12sp label inside a 52x30 pill (selected `PeachSoft`); selected icon/label `Brown`, otherwise `BrownSoft`. Icons: Home, Photo, CalendarMonth, Groups (Rounded). Center heart: 64dp `Peach` circle, 4dp `Cream` border, 28dp Brown heart, heartbeat animation (1800ms keyframe, 1.12/1.08 peaks), "send heartbeat" action. Tab change `fadeIn/fadeOut`; outside Yuva the back button returns to Yuva.
- **Cat hero v2** (`ui/cat/CatSection.kt`): `CatBg` card with 28dp corners, soft shadow; nothing on the video. Top: 36dp strip (`CatStripStart`→`CatStripEnd` gradient) with the name 17sp Bold + 14dp pencil (rename) and a white 28dp streak chip (16dp flame, 13sp Bold), both with 48dp touch targets reaching above the card. Middle: the video (`CatStage`, petting + hearts), height from `CatCardGeometry` so the card keeps its old height. Below: 36dp slot with the white speech bubble (15dp corners, 14sp Bold, tail pointing up, 2dp float); banners take its place as a pill (13sp, up to 2 lines). Then the need bars (8dp top padding).
- **Seri nedir? sheet** (`ui/cat/StreakExplainerSheet.kt`, board S1): `Cream` sheet, 32dp top corners, `SheetHandle` handle, header with the two avatars + flame circle (52dp, 16dp overlap) and a 22sp title, white card with three icon rows (40dp `PeachTint`/`MintTint` circles), `PeachSoft` Plus note (info only), `PeachButton` "Anladım". Scrim `Brown` 45%.
- **Need bars** (`NeedsRow/NeedMeter`): 4 bars, equal weight, gap 10dp, bar height **8dp**, track `Sand`; fill `NeedOk` (normal) / `NeedLow` (low, 700ms pulse); value change `spring(LowBouncy, Low)`. Labels: **Tokluk · Su · Enerji · Sevgi** (Fullness · Water · Energy · Love).
- **Care buttons** (`CareButton`): 64dp circle (28dp icon) + 14sp Bold label below. Order: **Besle · Su ver · Oyna · Uyut** (Feed · Give water · Play · Put to sleep; "Uyandır" / Wake up if sleeping). On press 0.88 scale (MediumBouncy), disabled 0.45 alpha, the most needed action is highlighted with a `Peach` background + Peach shadow.
- **Together card** (`TogetherCard`): "N gündür birliktesiniz" (together for N days) counter (26sp Bold), if there is no date "Tanıştığınız günü ekleyin" (Add the day you met).
- **Aklımdasın / Dürt** (`HomeParts.kt: NudgeCard`): (Thinking of you / Nudge) SoftCard 16dp, icon circle, title, description, 44dp / 14dp-radius button (ready `Peach`, sent `PeachTint`), 8s cooldown.
- **Mood card** (`MoodCard`): the two people's mood boxes (18dp, 12dp inner padding), "Paylaş/Değiştir" (Share/Change). Note: the box uses `kind.emoji` (conflicts with the canvas rule; see section 9).
- **Pairing OTP boxes** (`PairingScreen`): 46x60dp, 16dp, filled `PeachTint`, active `Peach` 2.5dp border + 6dp translucent ring, "pop" scale when filled; 6-digit code, gap after the 3rd digit.
- **Keypad** (`NumberPad`): 3x3 + bottom row (empty, 0, delete), keys 60dp height / 20dp, white + 3dp shadow, delete key flat `Cream`; gap 10dp; digit 26sp Bold; 0.92 scale on press.
- **Code card** (`CodeCard`): white 24dp card, 14dp shadow, "EŞLEŞME KODUN" (YOUR PAIRING CODE) label, copy button 44dp / 14dp (`MintInk` icon), time counter (`Schedule` icon), "bekleniyor" (waiting) Mint pulse dot.
- **Couple avatars** (`CoupleAvatars`): two 84dp avatars, dashed line between them + 36dp beating heart.
- **Coming soon placeholder** (`ComingSoon`): 96dp `PeachTint` icon circle (44dp icon), 24sp Bold title, 15sp Medium `BrownSoft` description, centered.

---

## 7. Motion and haptics

Micro animation principle: everything is slightly alive, none of it distracts.

| Animation | Value | Where |
|---|---|---|
| Squish on press (general) | `bounceClick` 0.94 (`spring MediumBouncy, Medium`) | all touchables |
| Main button press | 0.97 | `PeachButton`, `OutlineButton` |
| Card press | 0.98 | `SoftCard(onClick)` |
| Keypad | 0.92 | `Key` |
| Care button | 0.88 | `CareButton` |
| Beating heart (logo) | 1.0→1.12, 700ms, `FastOutSlowIn`, Reverse | `BeatingHeart` |
| Center heart (nav) | 1800ms keyframe: 1 → 1.12 (270ms) → 1 → 1.08 (810ms) → 1 | `BottomNav` |
| Need bar change | `spring(LowBouncy, Low)` | `NeedMeter` |
| Low bar pulse | 700ms `FastOutSlowIn`, Reverse | `NeedMeter` |
| Speech bubble float | 0→-2dp, 1500ms, Reverse (below the video, so the tail never touches it) | `CatSection` |
| Rising hearts | 1100ms (`tween FastOutSlowIn`), 120dp rise / 24dp sway | `CatSection`; `RisingHeart` on the login screen |
| Screen/tab transition | `fadeIn` + `fadeOut` (default duration) | `LovelyyRoot`, `HomeScreen` tabs |
| Background pulse (pairing) | 1600ms keyframe, 1800ms linear loop | `PairingScreen` |

Haptics:
- `PeachButton`, `PrimaryButton`: `HapticFeedbackType.LongPress` (main action = distinct).
- Other `bounceClick` (default), `SecondaryButton`: `TextHandleMove` (light tick).
- Keypad: `TextHandleMove` (`haptic=null` on the key, given manually on click).
- Cat: care action `LONG_PRESS`, success `CONFIRM` (API 30+, otherwise `KEYBOARD_TAP`), cat touch `KEYBOARD_TAP`, petting stroke `CLOCK_TICK`.
- Heartbeat/nudge signal: `SignalCenter.vibrate` (heartbeat "dum-dum" waveform); `HEARTBEAT_PATTERN` for the notification.

---

## 8. Iconography

- **Code:** Material Icons `Rounded` family (`Icons.Rounded.*`: Home, Photo, CalendarMonth, Groups, TouchApp, Schedule, ContentCopy, Done, Share, Backspace (AutoMirrored), KeyboardArrowLeft (AutoMirrored)); heart `Icons.Filled.Favorite` / `Rounded.Favorite`; `Filled` on the cat screen (WbSunny, Bedtime; the name pencil and streak flame are design SVG paths).
- **Sizes:** list/nav 22dp · small inline 16-20dp · card icon 22dp · care button 28dp · center heart 28dp · "Yakında" large 44dp (in a 96dp circle).
- **Color:** icon `Brown` (active) / `BrownSoft` (inactive) / `BrownHint` (inside a field); icon circle background `PeachTint`/`MintTint`.
- **Canvas:** no emoji, faces and icons are SVG. A new icon is first drawn as SVG on the canvas; in code the closest Rounded Material icon or a vector is used.
- **App logo / launcher icon:** two cats sitting side by side (Peach `#FFB38A` and deep mint `#5FB39E`) whose tails form a heart, on Cream `#FFF8F2`. Adaptive icon: `res/drawable/ic_launcher_foreground.xml` + `ic_launcher_background.xml` (the foreground is also the themed/monochrome layer and the splash icon). Source SVG: `assets/brand/lovelyy-icon.svg`. Wordmark: "Lovelyy" in Quicksand Bold, Brown, with the two final "y" letters in `#F29A6B` and `#5FB39E`.
- The cat artwork is animated via `ui/cat/` (CatAnimator, CatClip); on the welcome screen, "a seated peach-colored tabby cat".

---

## 9. Copy and tone of voice

Turkish, warm, "we"-focused, short. Source: `res/values/strings.xml`. All user text goes into `strings.xml` (not hardcoded).

- **Address:** to the partner "sen" (singular), but for shared ownership "siz/ikiniz/kediniz" (you both/the two of you/your cat): "Kedinize bir isim verin", "Bugün nasılsınız?", "Bizim köşemiz", "İkinizin minik dünyası".
- **Warm and direct:** "Hoş geldin", "Başlayalım", "Sevgilini davet et", "Basılı tut, X kalbinin atışını hissetsin.", "Aklımdasın".
- **Titles are short**; actions are verbs: Besle, Su ver, Oyna, Uyut, Dürt, Paylaş, Kaydet, Vazgeç, Eşleş, Devam et, Kopyala.
- **Errors:** do not blame, suggest a fix: "İnternet bağlantısı yok gibi görünüyor.", "Bağlantı sorunu oldu, tekrar dener misin?".
- **Not-yet-built feature:** a `Yakında` pill or the toast "Bu köşe sonraki adımlarda geliyor". This is not an error; it stays in a warm tone.
- **Status lines:** "Bugün henüz ilgilenmedin", "Sen bugün ilgilendin, X bekleniyor", "Bugün ikiniz de ilgilendiniz, seri büyüdü".
- **Time:** relative ("az önce", "5 dk önce"); date `d MMMM yyyy`, `tr` locale.
- **Writing:** Sentence case, "·" separator, "…" ellipsis character, quotes “ ”.
- **Emoji note:** the canvas rule is "no emoji". The current code has some emoji in `strings.xml` (💛 💭 ✓ in `home_soon_toast`, `nudge_sent`, `pair_share_text`, etc.) and `MoodKind.emoji` in mood entries. This is the current state; do not use emoji in new copy, prefer icons/SVG. The existing ones can be handled as a separate task (decision is Ulaş's).

---

## 10. Screen inventory

The app navigates via `AppState` (no NavController): `LovelyyRoot.kt`. The `screen_view` name is the constant route name in `analytics/Screens.kt`.

> Status note: per CLAUDE.md, the only approved and built ones are Karşılama (Welcome), Giriş (Login), Eşleşme (Pairing) and Anasayfa (Home; including the cat); the other cards and tabs (other than Yuva) are "Yakında" (Coming soon). The table below notes the leftovers found in code (possibly unapproved) for the Biz tab, Nudge and Mood cards; in case of conflict CLAUDE.md prevails.

### In code

| Screen | File | Analytics name | Status |
|---|---|---|---|
| Loading | `LovelyyRoot.LoadingScreen` | (not logged) | Done |
| Firebase setup | `setup/SetupScreens.kt` | `firebase_setup` | Done |
| Error | `setup/SetupScreens.kt` | `error` | Done |
| Welcome + Login/Sign-up | `auth/LoginScreen.kt` | `login` | Done (canvas 1, 2) |
| Intro (try-it onboarding, 3 pages) | `onboarding/IntroScreen.kt`, `onboarding/SplitStage.kt` | `onboarding_intro` | Done (canvas A1-A3) |
| Name | `profile/NameScreen.kt` | `name_setup` | Done |
| Pairing (share code / enter code) | `pairing/PairingScreen.kt` | `pairing` | Done (canvas 3a, 3b) |
| Paired + notification primer + location primer | `onboarding/PairedFlow.kt` | `paired_onboarding` | Done (canvas A4-A6); no names on these screens, always "Sevgilin"/"Sen" |
| Home / Yuva tab (cat, need bars, care, together card, nudge, mood, heartbeat inbox) | `home/HomeScreen.kt`, `cat/*` | `home` | Done; some cards may still show the `Yakında` toast (which card is wired is passed via the `soon` parameter in code; unverified one by one) |
| Biz tab | `HomeScreen.kt: BizTab` | `home` | Partial: "X & Y" title, sign-out, card list; details unverified |
| Anılar tab | `HomeScreen` | `home` | **Yakında** (`ComingSoon`) |
| Planlar tab | `HomeScreen` | `home` | **Yakında** (`ComingSoon`) |
| Center heart (send heartbeat) | `BottomNav` | - | **Yakında** (`onHeart = soon`); visuals (`heartbeat/EmotionVisual.kt`) are ready |

### Designed on the canvas (missing or partial in code; approval/status unverified in code)
Approved canvas boards: 1 Karşılama (Welcome) · 2 Giriş (Login) · 3a/3b Eşleşme (Pairing) · 4 Anasayfa (Home; first open + after scrolling down) · 5 Gün sayacı (Day counter) · 6 Kalp atışı + dürtme (Heartbeat + nudge) · 7 Ruh hali (Mood) · 8 Günün sorusu (Question of the day) · 9 Mesafe (Distance) · 10 Biz sekmesi (Us tab: profile + settings) · 11 Planlar sekmesi (Plans tab: calendar + special days) · 12 Hayaller listesi (Dreams list) · 13 Karar çarkı (Decision wheel) · 14 Kedi bakımı (Cat care) · 15 Ana ekran widget'ları (Home screen widgets) · 16 Anılar sekmesi (Memories tab) · 17 Canlı çizim (Live drawing) · 18 Notlar (Notes) · 19 Zaman kapsülü (Time capsule).

Without a counterpart in code (designed, waiting to be coded): 5, 8, 9, 11, 12, 13, 16, 17, 18, 19 (and the full form of 10/14). For 15 there is a `widget/` package; its scope is unverified. The "approved" status of every board on the canvas is not in code; approval status must be confirmed with Ulaş (**unverified**).

Bottom menu note on the canvas: Yuva (cat + summary) · Anılar (live drawing, notes, time capsule) · center heart (heartbeat with one tap) · Planlar (calendar, dreams, decision wheel) · Biz (mood, distance, daily question, profile).

---

## 11. Process rule and new-screen checklist

**Process (mandatory):** Every new screen is first designed as a board on the canvas (artifact) by **Fable (claude-fable-5-2)** and **approved by Ulaş**; it is NOT coded before approval. Without approval the relevant card/tab stays `Yakında`.

Canvas rules: 390x844 board, no emoji (SVG icons/faces), the bottom menu is copied onto every board, consistent with the tokens in this file.

**New-screen checklist**
- [ ] Designed on the canvas and approved by Ulaş (board name noted).
- [ ] Colors only from tokens (`MaterialTheme.colorScheme` or the `ui/theme/Color.kt` symbols); no raw `Color(0x...)` at all. If needed, a new token is added to `Color.kt` and to this file.
- [ ] Typography from the theme scale or the patterns in section 3; font Quicksand.
- [ ] Corners from `LovelyyShapes` or the values in section 4; shadow `softShadow`.
- [ ] One main action per screen (`PeachButton`), in the lower half, reachable with one hand.
- [ ] Touch targets ≥ 48dp; `contentDescription` on icon-only buttons.
- [ ] Touchables use `bounceClick`/the component library (squish + haptic); `LongPress` on the main action.
- [ ] Ready-made components (`ui/components`) were used; not rewritten.
- [ ] Texts in `strings.xml`, Turkish, warm tone; no emoji.
- [ ] Loading / error (`ErrorText`) / empty state considered.
- [ ] Safe area (`statusBarsPadding`, `navigationBarsPadding`) and 20-24dp edge margin.
- [ ] Analytics screen name: the `AppState` case was added and a constant route name (`snake_case`, no dynamic data) was written to `analytics/Screens.kt`; events follow the rules in `docs/analytics.md`.
- [ ] The inventory in this file (section 10) was updated.

**Note (token discipline):** `docs/design-principles.md` says "screens use only `MaterialTheme.colorScheme`", but the current `Design.kt` family imports theme symbols such as `Brown`, `Peach` directly. The rule accepted in practice: no raw hex, only `ui/theme` symbols/`colorScheme`. If a dark theme is added, these direct usages will need to be moved to `colorScheme`.
