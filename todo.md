# Yapılacaklar (elle yapılacak Console adımları)

- [ ] Merge'den sonra firebase/firestore.rules dosyasını Firebase Console > Firestore > Rules'a yapıştırıp yayınla.
- [ ] Firebase Console > App Check'te com.lovelyy.app'i Play Integrity ile kaydet (Play app signing SHA-256).
- [ ] Emülatörün logcat'teki App Check debug token'ını Console > App Check > Manage debug tokens'a ekle.
- [ ] App Check'i, Play internal testing sürümü doğrulanmış istek gösterene kadar zorunlu (enforce) yapma.
- [ ] Google Cloud Console'da Android API anahtarını com.lovelyy.app + SHA-1'lerle ve Firebase API'leriyle sınırla.
- [ ] Firebase Console > Authentication > Settings'te Email enumeration protection'ı aç.
- [ ] Play yayınından önce release build'de R8'i (isMinifyEnabled) açıp test et.
- [ ] Sinyal sınırlarını değiştirmek için Firestore'da config/limits dokümanındaki signalsPerHour / signalsPerDay / minGapSeconds alanlarını düzenle (yoksa oluştur; varsayılan 15 / 60 / 2).
- [ ] Ada ile giriş yapıp Plans ve History ekranlarındaki Türkçe tarihleri (PR #10 sonrası) gözle kontrol et.
