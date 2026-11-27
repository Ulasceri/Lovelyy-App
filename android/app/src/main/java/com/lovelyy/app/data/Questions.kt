package com.lovelyy.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/** One partner's answer. */
data class Answer(val uid: String, val name: String, val text: String, val at: Long)

/** couples/{id}/questions/{yyyy-MM-dd}: the day's question and both answers. */
data class DayQuestion(val day: String, val question: String, val answers: Map<String, Answer>)

/**
 * Günün sorusu. The question of a day comes from a fixed list, picked by the date, so both
 * phones show the same question without a server. The partner's answer is shown only after
 * you answered too (the app hides it; both answers live in the couple's own document).
 */
object QuestionBank {
    fun forDay(day: LocalDate): String = QUESTIONS[Math.floorMod(day.toEpochDay(), QUESTIONS.size.toLong()).toInt()]

    val QUESTIONS = listOf(
        "İlk buluşmamızdan en çok neyi hatırlıyorsun?",
        "Bende en sevdiğin üç şey ne?",
        "Birlikte gitmek istediğin ilk yer neresi?",
        "Beni ilk gördüğünde aklından ne geçti?",
        "Seni en çok ne güldürür?",
        "Hayalindeki sıradan bir pazar günü nasıl geçer?",
        "Birlikte öğrenmek istediğin bir şey var mı?",
        "Sana kendini en sevilmiş hissettiren şey ne?",
        "Çocukluğundan en sevdiğin anın hangisi?",
        "Beş yıl sonra bizi nerede görüyorsun?",
        "Bana söylemeye çekindiğin küçük bir şey var mı?",
        "En sevdiğin ortak anımız hangisi?",
        "Bir gün hayatımızı değiştirebilsek ilk neyi değiştirirdin?",
        "Benimle ilgili seni en çok şaşırtan şey ne oldu?",
        "Zor bir günde benden ne duymak istersin?",
        "Birlikte yapmayı en çok sevdiğin şey ne?",
        "Kedimiz konuşabilseydi bize ne derdi?",
        "Hangi şarkı seni bana hatırlatıyor?",
        "Birlikte izlediğimiz en iyi film ya da dizi hangisi?",
        "Sence ilişkimizin süper gücü ne?",
        "Bugün seni mutlu eden küçük bir şey neydi?",
        "Benimle ilgili en sevdiğin alışkanlık hangisi?",
        "Birlikte bir restoran açsak adı ne olurdu?",
        "Seni sakinleştiren şey ne?",
        "Hayatında en çok minnettar olduğun şey ne?",
        "Bana bir gün boyunca sürpriz yapacak olsan ne yapardın?",
        "Kendinde değiştirmek istediğin bir şey var mı?",
        "İlk tartışmamızı hatırlıyor musun? Ne öğrendik?",
        "Birlikte yaşlandığımızı düşününce aklına ne geliyor?",
        "Sence ben hangi hayvana benziyorum, neden?",
        "En sevdiğin yemeği birlikte yapsak hangisi olurdu?",
        "Bu hafta benim için yapabileceğim en güzel şey ne?",
        "Hiç söylemediğin bir iltifatı şimdi söyler misin?",
        "Hangi mevsim bize daha çok yakışıyor?",
        "Birlikte gitmek istediğin bir konser ya da etkinlik var mı?",
        "En çok hangi anımızı tekrar yaşamak isterdin?",
        "Sence birbirimizden ne öğrendik?",
        "Mükemmel bir randevu nasıl olurdu?",
        "Bugün beni ne zaman düşündün?",
        "Bize ait küçük bir gelenek başlatsak ne olurdu?",
        "Seni en çok neyin heyecanlandırdığını merak ediyorum, nedir?",
        "Birlikte bir rekor kırsak hangi konuda olurdu?",
        "Sana yazdığım ya da söylediğim en güzel şey neydi?",
        "Bir haftalığına başka bir şehirde yaşasak hangisini seçerdin?",
        "Kendini en çok hangi anlarda güçlü hissediyorsun?",
        "Birlikte okumak istediğin bir kitap var mı?",
        "Sence ben en çok neye gülüyorum?",
        "Bu yıl birlikte başarmak istediğin bir şey ne?",
        "Bana hangi lakabı takmak isterdin?",
        "Şu an yanında olsam ilk ne yapardık?",
        "Birlikte yaşadığımız en komik anı hatırlıyor musun?",
        "Sabah gözünü açınca aklına ilk ne geliyor?",
        "Bir günlüğüne görünmez olsan ne yapardın?",
        "Birlikte yeni bir hobi edinsek hangisi olsun isterdin?",
        "Bugüne kadar bende seni en çok gururlandıran şey ne oldu?",
        "Seni en çok rahatlatan koku hangisi?",
        "Çocukken büyüyünce ne olmak istiyordun?",
        "Birlikte yaptığımız en güzel yolculuk hangisiydi?",
        "Evimizin duvarına büyük bir fotoğraf asacak olsak hangisi olurdu?",
        "Bir süper gücün olsa hangisini seçerdin?",
        "Bu hafta duyduğun en güzel söz neydi?",
        "Hayatında yaptığın en cesur şey ne?",
        "Benimle ilk konuştuğumuz günü anlatır mısın?",
        "Şu an en çok neyi özlüyorsun?",
        "Bir günü hiçbir şey yapmadan birlikte geçirsek nasıl olurdu?",
        "En sevdiğin çocukluk yemeği hangisi?",
        "Bana kısa bir mektup yazsan ilk cümlen ne olurdu?",
        "Sence mutlu bir evin olmazsa olmazı nedir?",
        "Hangi anımızı tek bir fotoğraf karesine sığdırırdın?",
        "Gelecekte birlikte yaşamak istediğin ev nasıl görünüyor?",
        "Bana öğrettiğin en değerli şey ne?",
        "Kahvaltıda genelde ne yemeyi seversin?",
        "Bir günlüğüne dünyanın herhangi bir yerinde olabilsen neresi olurdu?",
        "Sence birlikte en iyi olduğumuz şey ne?",
        "Hiç unutamadığın bir koku ya da ses var mı?",
        "Bana güvenmeye ilk ne zaman başladığını hatırlıyor musun?",
        "Yağmurlu bir günü birlikte nasıl geçirmek isterdin?",
        "En sevdiğin tatlı hangisi, bu hafta birlikte yapalım mı?",
        "Hayatını bir filme çevirsek adı ne olurdu?",
        "Bugün kendin için yaptığın güzel bir şey var mı?",
        "Sence bizi biz yapan şey ne?",
        "Hangi ortak anımız seni hâlâ gülümsetiyor?",
        "Küçükken en çok korktuğun şey neydi?",
        "Uzun bir yolculuğun şarkı listesine hangi şarkıyı eklerdin?",
        "Bir yıl sonra bugünü nasıl hatırlamak isterdin?",
        "Bana şimdiye kadar kimseye anlatmadığın küçük bir şey anlatır mısın?",
        "Birbirimizin hangi huyuna zamanla alıştık?",
        "Sana göre mükemmel bir akşam yemeği nasıl olur?",
        "En son ne zaman içten bir kahkaha attın?",
        "Birlikte bir ağaç dikseydik nereye dikmek isterdin?",
        "Çocukken en sevdiğin oyun hangisiydi?",
        "Hayatımıza hangi küçük şeyi daha sık katmalıyız?",
        "Birbirimize en çok hangi konuda destek olduk?",
        "Bir sabah uyanıp yeni bir dil konuşabilsen hangisi olurdu?",
        "Bizim en tatlı tartışma konumuz ne?",
        "Beni en çok hangi kıyafetimle seviyorsun?",
        "Gün içinde bana yazmak isteyip yazmadığın bir şey oldu mu?",
        "Sana huzur veren bir yer var mı?",
        "Evde en sevdiğin köşe neresi?",
        "Bir gün ünlü olsak neyle tanınmak isterdik?",
        "Birlikte pişirdiğimiz en iyi yemek neydi?",
        "Gelecek yaz için aklında nasıl bir plan var?",
        "Birbirimize verdiğimiz en güzel hediye hangisiydi?",
        "Hangi konuda daha sabırlı olmak isterdin?",
        "Sence bizi bir araya getiren şey neydi?",
        "En sevdiğin renk hangisi ve neden?",
        "Sana en iyi gelen sabah rutini nedir?",
        "Bir hafta boyunca sadece atıştırmalık yesek ne seçerdin?",
        "Başka bir ülkede yaşamak ister miydin, nerede?",
        "Hangi anılarımızı bir kutuya koyup saklardın?",
        "Benden küçük bir şey istesen ne isterdin?",
        "Birlikte bir macera yaşasak nasıl bir macera olurdu?",
        "Beni tek kelimeyle anlatsan hangi kelimeyi seçerdin?",
        "Küçük bir kutlamayı hak eden son başarın ne oldu?",
        "Bir ay boyunca her gün aynı yemeği yesen hangisi olurdu?",
        "Seni en çok ne yoruyor ve sana nasıl yardım edebilirim?",
        "Birlikte yürüyüşe çıksak hangi rotayı seçerdik?",
        "Anneannenden ya da dedenden aklında kalan bir anı var mı?",
        "Sana söylenmiş en güzel sözü hatırlıyor musun?",
        "Bu hafta kendine ayıracağın küçük bir zaman var mı?",
        "Hayatta hiç pişman olmadığın bir karar hangisi?",
        "Bir saat boyunca hiç konuşmadan yan yana otursak ne hissederdin?",
        "Bir şarkıyı bizim şarkımız ilan etsek hangisi olurdu?",
        "Kendini en çok hangi yerde evinde hissediyorsun?",
        "Bende seni en çok şaşırtan huy hangisi?",
        "Hep denemek isteyip denemediğin bir şey var mı?",
        "Yaşlandığımızda hangi huyumuzun hâlâ aynı kalacağını düşünüyorsun?",
        "Bugün gökyüzü nasıldı, fark ettin mi?",
        "En sevdiğin mevsim hangisi ve onda ne var?",
        "İyi bir arkadaşın ilk özelliği sence nedir?",
        "Yeni biriyle tanışırken seni ne heyecanlandırır?",
        "Beni en iyi anlatan anı hangisi?",
        "Birlikte bir bahçemiz olsa içine ne ekerdik?",
        "Çocukluk evinden aklında kalan bir detay var mı?",
        "Bir günlüğüne mesleğini değiştirsen ne olurdun?",
        "Birbirimize haftada bir yapabileceğimiz küçük bir iyilik fikrin var mı?",
        "Hangi filmi her izlediğinde yeniden seviyorsun?",
        "Bana en çok hangi konuda güveniyorsun?",
        "Birlikte yapabileceğimiz bir günübirlik gezi önerir misin?",
        "Sence sevgi en çok neyle büyür?",
        "Son hatırladığın rüya neydi?",
        "Bir yıl boyunca her ay başka bir ülkeye gitsek ilk hangisi olurdu?",
        "Kendine bir söz verecek olsan ne olurdu?",
        "Beni gülümsetmenin en kolay yolu ne?",
        "Bir akşam birlikte ne yapmak seni en çok mutlu eder?",
        "Yeni yıl için dileğin ne?",
        "Hayatımızdaki en şanslı tesadüf hangisiydi?",
        "Sence ilişkide dürüstlük ne demek?",
        "Bugün seni en çok ne düşündürdü?",
        "Yarın için küçük bir hedefin var mı?",
        "Sana en iyi gelen mola nedir?",
        "Birlikte yapmak isteyip henüz yapmadığımız bir şey ne?",
        "Hangi sesi duyunca gülümsüyorsun?",
        "Kedimize bir arkadaş alsak ona ne ad verirdik?",
        "Çok yorucu bir günün akşamında sana en iyi ne gelir?",
        "Seni nasıl daha iyi destekleyebilirim?",
        "Bir ödül kazansan ne için olurdu?",
        "Hangi eski fotoğrafımıza bakınca içinin ısındığını hissediyorsun?",
        "Bir hafta tatilimiz olsa dağ mı deniz mi?",
        "Hayatındaki en güzel sürpriz neydi?",
        "Beni bir yemekle anlatsan hangisi olurdu?",
        "Bana bugün söylemek istediğin güzel bir şey var mı?",
        "Birlikte gün doğumunu izlesek nerede olmak isterdin?",
        "Evde sessiz bir akşamı mı, kalabalık bir akşamı mı seçersin?",
        "Küçük bir jest bir günü sence nasıl değiştirir?",
        "Birlikte henüz görmediğimiz hangi yeri görmek isterdin?",
        "Hayatında seni en çok değiştiren kişi kim?",
        "Benimle geçirdiğin en güzel sabah hangisiydi?",
        "Çocukluğundan bir yaz tatilini anlatır mısın?",
        "Gelecekte çocuklarımıza ya da yeğenlerimize hangi hikâyeyi anlatırdık?",
        "Seni en çok motive eden şey nedir?",
        "Hangi eşyayı hep yanında taşırdın?",
        "Birlikte bir günü baştan tasarlasak sabahtan akşama nasıl olurdu?",
        "Bir ilişki için en güzel alışkanlık sence nedir?",
        "Kendini ifade etmekte zorlandığın bir an oldu mu?",
        "Hangi dizi ya da kitap karakteri sana benziyor?",
        "Bir günlüğüne zamanda yolculuk yapabilsen nereye giderdin?",
        "Şu anki en büyük hayalin nedir?",
        "Birlikte gittiğimiz yerlerden hangisine yeniden dönmek isterdin?",
        "Sana kendini özel hissettiren küçük bir davranış hangisi?",
        "Bu ay birlikte yapmak istediğin en basit şey ne?",
        "En iyi uykuyu nasıl çekersin?",
        "Lunaparka gitsek ilk hangi oyuncağa binerdin?",
        "Bana en çok hangi anlarda ihtiyaç duyuyorsun?",
        "Birlikte hangi şarkıyı yüksek sesle söylemek isterdin?",
        "Hayatından bir günü tekrar yaşayabilsen hangisini seçerdin?",
        "Küçük bir şeye bile çok sevindiğin bir anı hatırlıyor musun?",
        "Sana bir şiir ya da şarkı yazsam konusu ne olurdu?",
        "Birlikte kamp yapsak yanımıza neleri alırdık?",
        "Hangi eşyamız bize en çok anı biriktirdi?",
        "Bu haftanın en güzel anı neydi?",
        "Birlikte gün batımını izlemek için nerede olmak isterdin?",
        "Çocukken en sevdiğin çizgi film hangisiydi?",
        "Hayatta kendini güvende hissettiren üç şey ne?",
        "İlişkimizde en güzel dönüm noktası hangisiydi?",
        "Evimize bir hayvan daha gelse hangisi olurdu?",
        "Her gün tekrarladığın ve seni mutlu eden bir şey var mı?",
        "Birlikte sabaha kadar uyanık kalsak ne yapardık?",
        "Bir günlüğüne istediğin yemeği sınırsız yiyebilsen hangisi olurdu?",
        "Beni gördüğünde ilk ne hissediyorsun?",
        "Bu yıl seni en çok gururlandıran an ne oldu?",
        "Yıllık bir anı defterimiz olsa ilk sayfasına ne yazardık?",
        "Sana göre güzel bir aile akşamı nasıl geçer?",
        "Bizim için en önemli üç değer ne olurdu?",
        "Birlikte gönüllü bir iş yapsak hangi konuda olmasını isterdin?",
        "Hangi koku sana çocukluk evini hatırlatıyor?",
        "Bana en son ne zaman teşekkür etmek istedin?",
        "Kendinle ilgili en sevdiğin yön ne?",
        "Sabah mı akşam mı, hangisinde daha canlı hissediyorsun?",
        "Özel bir günü kalabalıkla mı, sadece ikimiz mi kutlamak istersin?",
        "Birlikte bir sergi gezsek nasıl bir sergi seçerdin?",
        "Bir günü yıllık tatil ilan etsek hangi gün olurdu?",
        "Sence aşkın dili nedir?",
        "Bugünü tek cümleyle özetlesen ne derdin?",
        "Şimdi bir hediye alabilsen ne seçerdin?",
        "Çocukken hangi yemekten nefret ederdin, hâlâ yemiyor musun?",
        "Birlikte bir sürpriz parti düzenlesek kimin için olurdu?",
        "Bir güzel haberi ilk bana vermek ister miydin?",
        "Birbirimizi ilk kez güldürdüğümüz anı hatırlıyor musun?",
        "Gün içinde en çok sevdiğin saat hangisi?",
        "Hayalindeki bilim kurgu dünyasında neyi görmek isterdin?",
        "Birlikte üç kelimeyle bugünü anlatsak hangileri olurdu?",
        "En sevdiğin sokak lezzeti hangisi?",
        "Bana bir şey öğretmek istesen ne öğretirdin?",
        "Bir yıl sonra bu zamanlarda ne yapıyor olmak istersin?",
        "Günün en zor kısmı genelde ne oluyor?",
        "Bana ilk sarıldığın anı hatırlıyor musun?",
        "Yeni bir şehirde kaybolsak ilk ne yapardık?",
        "Hangi anımız bir filmde olsa en romantik sahne olurdu?",
        "Hayatında en çok kime teşekkür etmek istiyorsun?",
        "Çocukluğunda seni en çok ne mutlu ederdi?",
        "Bu akşam birlikte ne izlemek istersin?",
        "Yaşlandığımızda en çok neyi özleyeceğimizi düşünüyorsun?",
        "Seni gülümseten bir kelime ya da ifade var mı?",
        "Birlikte bir yemek tarifi icat etsek içinde ne olurdu?",
        "Hafta sonu için en sevdiğin plan nedir?",
        "Hep gitmek isteyip gidemediğin bir yer var mı?",
        "Bir sabah çatıda yağmur sesini dinleseydik, ne yapardık?",
        "Bir gün telefonları kapatsak ne yapardık?",
        "Sence hangi konuda en çok anlaşıyoruz?",
        "Eski bir fotoğraf albümümüz olsa ilk sayfasında ne olurdu?",
        "Daha önce öğrenmiş olmayı istediğin bir şey var mı?",
        "İyi bir günün sana göre tarifi nedir?",
        "Hayatımızı kolaylaştıracak ufak bir icat ne olurdu?",
        "Bugün birine iyilik yaptın mı?",
        "Kendini bir şehirle anlatsan hangisi olurdu?",
        "Bir günü sadece yürüyerek geçirsek nereyi gezmek isterdin?",
        "Yeni bir şeye başlarken seni en çok ne cesaretlendirir?",
        "Hangi hayalini ertelemeyi bırakmak istersin?",
        "Çay ya da kahve eşliğinde sohbetimiz genelde nereye gidiyor?",
        "Bir günlüğüne kedimiz olsan neyle meşgul olurdun?",
        "Hangi güne geri dönüp bir şeyi yeniden yaşamak isterdin?",
        "Seni mutlu eden küçük bir alışkanlığın var mı?",
        "Gelecekte birlikte kutlamak istediğin bir kilometre taşı ne?",
        "Beni hangi konuda en iyi tanıdığını düşünüyorsun?",
        "Uzun bir yolculuğa çıksak ilk durağımız neresi olurdu?",
        "Seni sürekli gülümseten bir hatıra var mı?",
        "Bir çatı katında mı, bahçeli bir evde mi yaşamak isterdin?",
        "Bugün hangi küçük şey için minnettarsın?",
        "Bir akşam birlikte müzik dinleyerek ne yapmak istersin?",
        "Bir şeyi tek başına yapmak mı, birlikte yapmak mı seni daha çok mutlu eder?",
        "Birlikte hangi oyunu oynamak isterdin?",
        "Beni ilk fark ettiğin anı hatırlıyor musun?",
        "En son ne zaman kendini tamamen rahatlamış hissettin?",
        "Hayatın sana öğrettiği en güzel ders ne oldu?",
        "Sence iyi bir dinleyici nasıl olur?",
        "Bu yıl hâlâ yapmak istediğin küçük bir şey var mı?",
        "Birlikte hangi markette alışveriş yapmayı seviyoruz?",
        "Kendini hangi şarkıyla anlatırsın?",
        "Yeni yıla girerken birbirimize hangi dileği dilesek?",
        "Seni bir günlüğüne şımartacak olsam ne yapardım?",
        "Hangi tatlı alışkanlığımızı hiç kaybetmek istemezsin?",
        "Birlikte çıktığımız en eğlenceli akşam neydi?",
        "Yaşlandığımızda birbirimize ne söylerdik sence?",
        "Bugün sana bir mesaj yazsam ne yazmamı isterdin?",
        "Çocukken hayalinde yaşadığın bir yer var mıydı?",
        "Bu hafta birlikte ne pişirmek istersin?",
        "Hiç beklemediğin bir anda aklıma geldiğin oldu mu?",
        "Beni en çok hangi konuda destekledin?",
        "Hayatta en çok önem verdiğin şey nedir?",
        "Bir anı defterimizin ilk sayfasına hangi günü yazardık?",
        "Bende seninle en tuhaf ortak noktamız ne?",
        "Küçük bir ödül olarak kendine ne alırdın?",
        "Gece yarısı acıksak ne yerdik?",
        "Bugün seni en çok ne gülümsetti?",
        "İyi bir arkadaşlığın sırrı sence ne?",
        "Birlikte bir dil öğrensek ilk hangi kelimeyi öğrenirdik?",
        "Hangi mevsimde doğmuş olmak isterdin?",
        "Sana göre ev ne demek?",
        "Hayatından hangi dönemi yeniden yaşamak isterdin?",
        "Bana bir tavsiye vermek istesen ne olurdu?",
        "Bir film sahnesine girebilsen hangisi olurdu?",
        "Yılbaşında birlikte ne yapmak isterdin?",
        "En sevdiğin sabah içeceği nedir?",
        "Hafta sonu kaçamağı için nasıl bir yer seçerdin?",
        "Bana sadece bizim kullanacağımız bir isim koysan ne olurdu?",
        "Kendini en çok ne zaman güvende hissediyorsun?",
        "Birbirimiz için yapabileceğimiz küçük bir ritüel ne olabilir?",
        "En sevdiğin şehir neresi?",
        "Bir yıl önce bugün neler yapıyorduk, hatırlıyor musun?",
        "Birlikte yazacağımız bir hikâyenin ilk cümlesi ne olurdu?",
        "Seni en iyi anlatan eşya hangisi?",
        "Bugün için küçük bir dileğin var mı?",
        "Sence hangi film bizi anlatıyor?",
        "Bir şeyi baştan başlayabilsek neyi seçerdin?",
        "Beni en çok neyle hatırlıyorsun, bir kokuyla mı bir şarkıyla mı?",
        "Önümüzdeki on yılda birlikte kaç ülke görmek istersin?",
        "Kendini en cesur ne zaman hissediyorsun?",
        "Hayatımızı güzelleştiren en basit şey ne?",
        "Küçük bir not bırakmak sana nasıl hissettirir?",
        "Birlikte bir tatil albümü yapsak ilk fotoğraf hangisi olurdu?",
        "Sence bir gülümseme neyi değiştirir?",
        "Bu akşam yıldızları izlesek ne konuşurduk?",
        "Aynı anda aynı şeyi düşündüğümüz bir an oldu mu hiç?",
        "Hangi değerimizi sevdiklerimize aktarmak isterdin?",
        "Hayalindeki evde ilk neyi yapardık?",
        "Birlikte yeni bir şey denediğimiz son zaman ne zamandı?",
        "Bir günlüğüne planı ben yapsam nasıl olmasını isterdin?",
        "Bu ayın sana en çok kattığı şey ne oldu?",
        "Bir daha asla yapmak istemediğin bir şey var mı?",
        "Hangi anımızı her ay yeniden hatırlamak isterdin?",
        "Bir ilişkide en önemli üç şey sence ne?",
        "Bana sormak isteyip sormadığın bir soru var mı?",
        "Bu hafta seni en çok neye sevindirdi?",
        "Birlikte en çok güldüğümüz şey ne?",
        "Sence küçük kutlamalar neden önemli?",
        "Çocukluk arkadaşlarından kimi hâlâ hatırlıyorsun?",
        "Telefonsuz bir pazar günü nasıl geçerdi?",
        "Seni en çok hangi kitap etkiledi?",
        "Birlikte bir festivale gitsek nasıl olurdu?",
        "Hayatındaki en güzel mutfak kokusu hangisi?",
        "Bir yılda bir şey öğrenmek için zamanın olsa ne öğrenirdin?",
        "Gece geç saatte ne konuşmayı seversin?",
        "Beraber bir tablo yapsak ne çizerdik?",
        "Bu hafta ne zaman birbirimize daha çok zaman ayırabiliriz?",
        "Sana en çok hangi sözcük iyi gelir?",
        "Hayatındaki en sevdiğin kıyafet hangisi?",
        "Bir gün birlikte bir ormanda yürüsek ne konuşurduk?",
        "Birbirimize ilk kez ne zaman sevgimizi hissettirdik?",
        "Sana göre bir sürpriz ne zaman en güzel olur?",
        "Bugüne kadar aldığın en iyi tavsiye neydi?",
        "Birlikte bir günü şehir dışında geçirsek ne yapardık?",
        "Küçük bir korkunu yenmek için ne yapardın?",
        "Hangi anımızı bir şarkıyla anlatırdın?",
        "Sence hayatta en güzel şeyler neden küçük şeylerdir?",
        "Önümüzdeki ay için birlikte bir hedef koysak ne olurdu?",
        "Sana ilk söylediğim sevgi sözünü hatırlıyor musun?",
        "Hep birlikte yapmak istediğin bir tatil geleneği var mı?",
        "Birbirimize yazacağımız bir zaman kapsülü mektubuna ne yazardın?",
        "Sence dünyadaki en güzel ses nedir?",
        "Bu hafta kendine nasıl iyi bakabilirsin?",
        "Gün boyunca en çok hangi anı yeniden yaşamak isterdin?",
        "Birlikte hangi spor ya da hareketi denemek isterdin?",
        "Seni en çok hangi mekân huzurlu yapar?",
        "Birisi bize bir gün hakkında bir film çekse hangi günü seçerdik?",
        "Bana en çok hangi yönümden ilham alıyorsun?",
        "Çocukken hayalinde kurduğun gizli bir yer var mıydı?",
        "Yeni bir yıl için bir listemiz olsa ilk maddesi ne olurdu?",
        "Birlikte yaptığımız en iyi alışveriş neydi?",
        "Sence ortak bir hayat kurmak ne demek?",
        "Küçük bir ev işini birlikte yapmayı sever misin?",
        "Seninle birlikte olduğum zamanlarda zaman hızlı mı akıyor yoksa yavaş mı?",
        "Sen olmasan bugün neyi kaçırırdım sence?",
        "Bu dönemde hangi şeyi daha çok yapmak istersin?",
        "Beni hangi lezzet ya da yiyecekle eşleştirirsin?",
        "Bir hafta sonu boyunca sadece film maratonu yapsak ne izlerdik?",
    )
}

/** Both partners answered the day's question, so my answer can no longer change. */
class AnswersLockedException : Exception("Both partners answered; answers are locked")

/** Pure helpers for the question screen (unit-tested in QuestionMathTest). */
object QuestionMath {
    /**
     * My answer can be changed only until the partner has answered too: once both answered
     * ([answeredUids] has mine and someone else's), the answers are locked. A first answer is always allowed.
     */
    fun isLocked(answeredUids: Collection<String>, me: String): Boolean =
        me in answeredUids && answeredUids.any { it != me }

    /**
     * Days in a row on which both partners answered, ending today (or yesterday, while today
     * is still open). [days] can be in any order.
     */
    fun streak(days: List<DayQuestion>, today: LocalDate): Int {
        val complete = days.filter { it.answers.size >= 2 }.map { it.day }.toSet()
        var d = if (today.toString() in complete) today else today.minusDays(1)
        var n = 0
        while (d.toString() in complete) { n++; d = d.minusDays(1) }
        return n
    }
}

class QuestionRepository(private val db: FirebaseFirestore) {

    private fun questions(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.QUESTIONS)

    /**
     * Saves my answer for [day]. Throws [AnswersLockedException] if I already answered and the partner has
     * answered too (see [QuestionMath.isLocked]); checked in a transaction because both phones may race.
     * Offline, a transaction can't run; then the answer is written as before, and the screen's own lock
     * (it hides editing once both answers are visible) is the only guard.
     */
    suspend fun answer(coupleId: String, day: LocalDate, me: UserProfile, text: String) {
        val ref = questions(coupleId).document(day.toString())
        fun data(question: String) = mapOf(
            "question" to question,
            "answers" to mapOf(
                me.uid to mapOf(
                    "name" to me.displayName,
                    "text" to text.trim().take(MAX_ANSWER),
                    "at" to System.currentTimeMillis(),
                ),
            ),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        val data = data(QuestionBank.forDay(day))
        try {
            db.runTransaction { tx ->
                val snap = tx.get(ref)
                val answered = (snap.get("answers") as? Map<*, *>)?.keys?.filterIsInstance<String>().orEmpty()
                if (QuestionMath.isLocked(answered, me.uid)) throw AnswersLockedException()
                // The first answerer fixes the day's question; a partner on another app version (other list
                // size) must not overwrite it, or the two phones would show different questions.
                val stored = snap.getString("question")?.takeIf { it.isNotBlank() }
                tx.set(ref, if (stored != null) data(stored) else data, SetOptions.merge())
                null
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.causeOf<AnswersLockedException>()?.let { throw it }
            if (!e.isOffline()) throw e
            ref.set(data, SetOptions.merge()).await()
        }
    }

    /** The days of the last [limit] days that have answers, newest first. */
    fun observeRecent(coupleId: String, limit: Long = 60): Flow<List<DayQuestion>> = callbackFlow {
        // Document ids are yyyy-MM-dd, so an ascending id range from the cutoff picks the recent days.
        // (Ordering by id descending needs a composite index that the project does not have.)
        val from = CoupleClock.today().minusDays(limit).toString()
        val reg = questions(coupleId)
            .whereGreaterThanOrEqualTo(com.google.firebase.firestore.FieldPath.documentId(), from)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                } else if (snap != null) {
                    trySend(
                        snap.documents.map { d ->
                            @Suppress("UNCHECKED_CAST")
                            val raw = d.get("answers") as? Map<String, Map<String, Any?>> ?: emptyMap()
                            DayQuestion(
                                day = d.id,
                                question = d.getString("question") ?: QuestionBank.forDay(LocalDate.parse(d.id)),
                                answers = raw.mapValues { (uid, a) ->
                                    Answer(uid, a["name"] as? String ?: "", a["text"] as? String ?: "", (a["at"] as? Number)?.toLong() ?: 0L)
                                },
                            )
                        }.sortedByDescending { it.day },
                    )
                }
            }
        awaitClose { reg.remove() }
    }

    companion object {
        const val MAX_ANSWER = 500
    }
}
