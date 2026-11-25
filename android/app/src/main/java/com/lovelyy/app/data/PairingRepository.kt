package com.lovelyy.app.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

/**
 * Partner pairing, done entirely on the client and protected by firestore.rules
 * (works on the free Spark plan, no Cloud Functions needed).
 *
 *  1. Partner A creates pairCodes/{CODE} and shares the 6-digit CODE.
 *  2. Partner B enters it. In ONE transaction B:
 *       - creates couples/{CODE} with members [A, B]
 *       - sets coupleId on both users/A and users/B
 *       - deletes pairCodes/{CODE} (a code works only once)
 *     The rules only accept this exact shape, so nobody can pair with a stranger
 *     without knowing their code, and nobody else can read the couple's data.
 *  3. A is listening to users/A, sees coupleId appear and lands on the home screen.
 */
class PairingRepository(private val db: FirebaseFirestore) {

    private val random = SecureRandom()

    /** Creates a fresh code for [me] (replacing any old one) and returns it. */
    suspend fun createCode(me: UserProfile): String = wrap {
        if (me.coupleId != null) throw AppException(AppError.AlreadyPaired)
        var lastError: Exception? = null
        repeat(5) {
            val code = randomCode()
            val batch = db.batch()
            me.pairCode?.let { batch.delete(codeRef(it)) }
            batch.set(
                codeRef(code),
                mapOf("ownerUid" to me.uid, "createdAt" to FieldValue.serverTimestamp()),
            )
            batch.update(userRef(me.uid), "pairCode", code)
            try {
                batch.commit().await()
                return@wrap code
            } catch (e: FirebaseFirestoreException) {
                // Very unlikely: the random code already exists. Try another one.
                lastError = e
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            }
        }
        throw lastError ?: AppException(AppError.Unknown)
    }

    /** Joins partner A using their [rawCode]. On success both users have the same coupleId. */
    suspend fun joinWithCode(me: UserProfile, rawCode: String): Unit = wrap {
        val code = normalize(rawCode)
        if (code.length != CODE_LENGTH) throw AppException(AppError.CodeNotFound)
        if (me.coupleId != null) throw AppException(AppError.AlreadyPaired)
        if (code == me.pairCode) throw AppException(AppError.OwnCode)

        db.runTransaction { tx ->
            val codeSnap = tx.get(codeRef(code))
            if (!codeSnap.exists()) throw AppException(AppError.CodeNotFound)
            val ownerUid = codeSnap.getString("ownerUid") ?: throw AppException(AppError.CodeNotFound)
            if (ownerUid == me.uid) throw AppException(AppError.OwnCode)
            val createdAt = codeSnap.getTimestamp("createdAt")
            if (createdAt != null && isExpired(createdAt)) throw AppException(AppError.CodeExpired)

            tx.set(
                coupleRef(code),
                mapOf(
                    "members" to listOf(ownerUid, me.uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "startDate" to null,
                ),
            )
            tx.update(userRef(ownerUid), mapOf("coupleId" to code, "pairCode" to null))
            tx.update(userRef(me.uid), mapOf("coupleId" to code, "pairCode" to null))
            tx.delete(codeRef(code))
            // If I was also sharing a code of my own, retire it.
            me.pairCode?.let { tx.delete(codeRef(it)) }
            null
        }.await()
    }

    private fun userRef(uid: String) = db.collection(Paths.USERS).document(uid)
    private fun coupleRef(id: String) = db.collection(Paths.COUPLES).document(id)
    private fun codeRef(code: String) = db.collection(Paths.PAIR_CODES).document(code)

    private fun randomCode() = buildString {
        repeat(CODE_LENGTH) { append(DIGITS[random.nextInt(DIGITS.length)]) }
    }

    /** When [code] was created (server time, millis), or null if it no longer exists. */
    suspend fun codeCreatedAt(code: String): Long? = wrap {
        codeRef(code).get(com.google.firebase.firestore.Source.SERVER).await().takeIf { it.exists() }?.getTimestamp("createdAt")?.toDate()?.time
    }

    private fun isExpired(createdAt: Timestamp): Boolean =
        System.currentTimeMillis() - createdAt.toDate().time > CODE_TTL_MS

    private suspend fun <T> wrap(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: AppException) {
        throw e
    } catch (e: Exception) {
        // Transactions wrap our own exceptions; unwrap them.
        val app = generateSequence<Throwable>(e) { it.cause }.filterIsInstance<AppException>().firstOrNull()
        throw app ?: when {
            e is FirebaseNetworkException -> AppException(AppError.Network, e)
            e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
                AppException(AppError.Network, e)
            e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                AppException(AppError.PermissionDenied, e)
            else -> AppException(AppError.Unknown, e)
        }
    }

    companion object {
        const val CODE_LENGTH = 6
        /** Characters a code may contain. No 0/O or 1/I/L. Must match firestore.rules. */
        const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        /** New codes are digits only (typed on the app's own number pad), 2-9 so they fit the rules above. */
        const val DIGITS = "23456789"
        /** The app treats a code as valid for 30 minutes (the rules allow at most 24h). */
        const val CODE_TTL_MS = 30L * 60 * 1000

        fun normalize(raw: String) = raw.uppercase().filter { it in ALPHABET }
    }
}
