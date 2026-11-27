# Yapılacaklar (elle yapılacak Console adımları)

- [ ] Firebase Console > App Check'te com.lovelyy.app'i Play Integrity ile kaydet (Play app signing SHA-256).
- [ ] Emülatörün logcat'teki App Check debug token'ını Console > App Check > Manage debug tokens'a ekle.
- [ ] App Check'i, Play internal testing sürümü doğrulanmış istek gösterene kadar zorunlu (enforce) yapma.
- [ ] Google Cloud Console'da Android API anahtarını com.lovelyy.app + SHA-1'lerle ve Firebase API'leriyle sınırla.
- [ ] Firebase Console > Authentication > Settings'te Email enumeration protection'ı aç.
- [ ] Play yayınından önce release build'de R8'i (isMinifyEnabled) açıp test et.
- [ ] Sinyal sınırlarını değiştirmek için Firestore'da config/limits dokümanındaki signalsPerHour / signalsPerDay / minGapSeconds alanlarını düzenle (yoksa oluştur; varsayılan 15 / 60 / 2).
- [ ] Herkese açık iletişim e-postasını `web/CONTACT_EMAIL` içine yaz ve `python3 web/tools/build.py` çalıştır (şimdilik yer tutucu).
- [ ] Web sitesini `npx firebase-tools deploy --only hosting --project lovelyy-native` ile yayınla.
- [ ] Bildirimler için FCM eklenirse gizlilik politikasını ve Data safety taslağını güncelle.
- [ ] Ada ile giriş yapıp Plans ve History ekranlarındaki Türkçe tarihleri (PR #10 sonrası) gözle kontrol et.
- [ ] Eşleşme ekranındaki hesap penceresini ve hesap silmeyi Google ile giriş yapan bir hesapla da dene.
- [ ] Kod girme ekranı açılır açılmaz yapılan ilk tuş dokunuşunun bazen algılanmamasını incele.
- [ ] `assets/items/` içindeki 12 eşya görselini Plus'ın eşya aşamasında uygulamaya bağla (seçim ekranı, anasayfa, widget).
- [ ] Eşyaya özel kedi animasyonlarını üret; pilot olarak tüy ile oyun klibinden başla.
