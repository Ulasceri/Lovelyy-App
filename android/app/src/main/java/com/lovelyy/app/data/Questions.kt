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
        val data = mapOf(
            "question" to QuestionBank.forDay(day),
            "answers" to mapOf(
                me.uid to mapOf(
                    "name" to me.displayName,
                    "text" to text.trim().take(MAX_ANSWER),
                    "at" to System.currentTimeMillis(),
                ),
            ),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        try {
            db.runTransaction { tx ->
                val snap = tx.get(ref)
                val answered = (snap.get("answers") as? Map<*, *>)?.keys?.filterIsInstance<String>().orEmpty()
                if (QuestionMath.isLocked(answered, me.uid)) throw AnswersLockedException()
                tx.set(ref, data, SetOptions.merge())
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
