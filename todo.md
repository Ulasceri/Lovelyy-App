# Yapılacaklar (elle yapılacak Console adımları)

- [ ] Firebase Console > App Check'te com.lovelyy.app'i Play Integrity ile kaydet (Play app signing SHA-256).
- [ ] Emülatörün logcat'teki App Check debug token'ını Console > App Check > Manage debug tokens'a ekle.
- [ ] App Check'i, Play internal testing sürümü doğrulanmış istek gösterene kadar zorunlu (enforce) yapma.
- [ ] Google Cloud Console'da Android API anahtarını com.lovelyy.app + SHA-1'lerle ve Firebase API'leriyle sınırla.
- [ ] Firebase Console > Authentication > Settings'te Email enumeration protection'ı aç.
- [ ] Sinyal sınırlarını değiştirmek için Firestore'da config/limits dokümanındaki signalsPerHour / signalsPerDay / minGapSeconds alanlarını düzenle (yoksa oluştur; varsayılan 15 / 60 / 2).
- [ ] Herkese açık iletişim e-postasını `web/CONTACT_EMAIL` içine yaz ve `python3 web/tools/build.py` çalıştır (şimdilik yer tutucu).
- [ ] Web sitesini `npx firebase-tools deploy --only hosting --project lovelyy-native` ile yayınla.
- [ ] Bildirimler için FCM eklenirse gizlilik politikasını ve Data safety taslağını güncelle.
- [ ] Ada ile giriş yapıp Plans ve History ekranlarındaki Türkçe tarihleri (PR #10 sonrası) gözle kontrol et.
- [ ] Eşleşme ekranındaki hesap penceresini ve hesap silmeyi Google ile giriş yapan bir hesapla da dene.
- [ ] Mırmır'ın eşyalarını ana ekran widget'larında da göster (tasarımı gerekir).
- [ ] Ruh hali geçmişi yalnızca son 30 günü gösteriyor, daha eski aylara geçiş eklenmeli mi karar ver.
- [ ] Kaydırma takılmasını (jank) emülatör yerine gerçek bir telefonda ölç.
- [ ] Zayıf ağ (yavaş/gecikmeli bağlantı) testini emülatör konsol izni ayarlanınca tekrar yap.
