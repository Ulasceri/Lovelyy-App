package com.lovelyy.app.text

import java.util.Locale

/**
 * Turkish case endings for names, so screens can say "Elif'e", "Ulaş'a", "Elif'in", "Ulaş'ın".
 * Follows vowel harmony on the last vowel, adds a buffer letter after a vowel ("Ayşe'ye", "Ayşe'nin").
 */
object TurkishSuffix {
    private val TR = Locale.forLanguageTag("tr-TR")
    private const val VOWELS = "aeıioöuü"

    private fun lastVowel(word: String): Char? = word.lowercase(TR).lastOrNull { it in VOWELS }
    private fun endsWithVowel(word: String) = word.lowercase(TR).lastOrNull()?.let { it in VOWELS } == true

    /** Yönelme hâli (-e/-a): "Elif'e", "Ulaş'a", "Ayşe'ye". */
    fun dative(name: String): String {
        val n = name.trim()
        if (n.isEmpty()) return n
        val v = lastVowel(n) ?: 'e'
        val e = if (v in "aıou") "a" else "e"
        return n + "'" + (if (endsWithVowel(n)) "y" else "") + e
    }

    /** İlgi hâli (-in/-ın/-un/-ün): "Elif'in", "Ulaş'ın", "Ayşe'nin", "Doğu'nun". */
    fun genitive(name: String): String {
        val n = name.trim()
        if (n.isEmpty()) return n
        val v = when (lastVowel(n) ?: 'i') {
            'a', 'ı' -> "ın"
            'o', 'u' -> "un"
            'ö', 'ü' -> "ün"
            else -> "in"
        }
        return n + "'" + (if (endsWithVowel(n)) "n" else "") + v
    }

    /** Ayrılma hâli (-den/-dan/-ten/-tan): "Perşembe'den", "Pazar'dan", "Ahmet'ten". */
    fun ablative(word: String): String {
        val n = word.trim()
        if (n.isEmpty()) return n
        val v = lastVowel(n) ?: 'e'
        val e = if (v in "aıou") "an" else "en"
        val last = n.lowercase(TR).last()
        val d = if (last in "çfhkpsşt") "t" else "d"
        return "$n'$d$e"
    }
}
