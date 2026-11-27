package com.lovelyy.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
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
    fun forDay(day: LocalDate): String {
        val q = QUESTIONS
        return q[Math.floorMod(day.toEpochDay(), q.size.toLong()).toInt()]
    }

    @Volatile private var questions: List<String> = emptyList()

    /**
     * Loads the questions (R.array.daily_questions; call from Application.onCreate and again on a locale change).
     * Every language must keep the same order and size, so the day index stays the same for both partners.
     */
    fun init(list: List<String>) {
        if (list.isNotEmpty()) questions = list
    }

    /** The loaded questions (a one-item fallback while [init] has not run). */
    val QUESTIONS: List<String> get() = questions.ifEmpty { FALLBACK }

    /** Only if something runs before [init]; Application.onCreate always runs first. */
    private val FALLBACK = listOf("Bugün seni en çok ne mutlu etti?")
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

    /**
     * The question to write with an answer: the day's [stored] question when there is one (the first answerer
     * fixed it), else [picked].
     */
    fun questionToWrite(stored: String?, picked: String): String = stored?.takeIf { it.isNotBlank() } ?: picked
}

class QuestionRepository(private val db: FirebaseFirestore) {

    private fun questions(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.QUESTIONS)

    /**
     * Saves my answer for [day]. Throws [AnswersLockedException] if I already answered and the partner has
     * answered too (see [QuestionMath.isLocked]); checked in a transaction because both phones may race.
     * Offline, a transaction can't run; then the answer is written as before, and the screen's own lock
     * (it hides editing once both answers are visible) is the only guard. The offline write keeps the day's
     * stored question too: it takes it from the local cache (the screen listens to this document, so it is
     * there whenever the phone has seen the day's question), and only without one falls back to [QuestionBank.forDay]
     * (the rules need a question when the document is created).
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
        try {
            db.runTransaction { tx ->
                val snap = tx.get(ref)
                val answered = (snap.get("answers") as? Map<*, *>)?.keys?.filterIsInstance<String>().orEmpty()
                if (QuestionMath.isLocked(answered, me.uid)) throw AnswersLockedException()
                // The first answerer fixes the day's question; a partner on another app version (other list
                // size) must not overwrite it, or the two phones would show different questions.
                tx.set(ref, data(QuestionMath.questionToWrite(snap.getString("question"), QuestionBank.forDay(day))), SetOptions.merge())
                null
            }.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.causeOf<AnswersLockedException>()?.let { throw it }
            if (!e.isOffline()) throw e
            // No server to ask: keep the question this phone last saw stored for the day, not the date's pick.
            val cached = try {
                ref.get(Source.CACHE).await().getString("question")
            } catch (c: CancellationException) {
                throw c
            } catch (notCached: Exception) {
                null
            }
            ref.set(data(QuestionMath.questionToWrite(cached, QuestionBank.forDay(day))), SetOptions.merge()).await()
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
