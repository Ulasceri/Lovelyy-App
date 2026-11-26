package com.lovelyy.app.text

import java.util.Locale

/**
 * Turkish case endings for names, so screens can say "Elif'e", "Ulaş'a", "Elif'in", "Ulaş'ın".
 * Only applies when the UI language is Turkish; in any other language the name is returned unchanged, so a
 * translation just writes "%1$s" and does its own grammar. Follows vowel harmony on the last vowel, adds a buffer letter after a vowel ("Ayşe'ye", "Ayşe'nin").
 */
object TurkishSuffix {
    private val TR = Locale.forLanguageTag("tr-TR")
    private const val VOWELS = "aeıioöuü"

    private fun lastVowel(word: String): Char? = word.lowercase(TR).lastOrNull { it in VOWELS }
    private fun endsWithVowel(word: String) = word.lowercase(TR).lastOrNull()?.let { it in VOWELS } == true

    /** Yönelme hâli (-e/-a): "Elif'e", "Ulaş'a", "Ayşe'ye". */
    fun dative(name: String): String {
        val n = name.trim()
        if (n.isEmpty() || !AppLocale.isTurkish()) return n
        val v = lastVowel(n) ?: 'e'
        val e = if (v in "aıou") "a" else "e"
        return n + "'" + (if (endsWithVowel(n)) "y" else "") + e
    }

    /** Belirtme hâli (-i/-ı/-u/-ü): "Elif'i", "Ulaş'ı", "Ayşe'yi", "Doğu'yu". */
    fun accusative(name: String): String {
        val n = name.trim()
        if (n.isEmpty() || !AppLocale.isTurkish()) return n
        val i = when (lastVowel(n) ?: 'e') {
            'a', 'ı' -> "ı"
            'o', 'u' -> "u"
            'ö', 'ü' -> "ü"
            else -> "i"
        }
        return n + "'" + (if (endsWithVowel(n)) "y" else "") + i
    }

    /** İlgi hâli (-in/-ın/-un/-ün): "Elif'in", "Ulaş'ın", "Ayşe'nin", "Doğu'nun". */
    fun genitive(name: String): String {
        val n = name.trim()
        if (n.isEmpty() || !AppLocale.isTurkish()) return n
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
        if (n.isEmpty() || !AppLocale.isTurkish()) return n
        if (n.last().isDigit()) return "$n'" + numberAblative(n.takeLastWhile { it.isDigit() })
        val v = lastVowel(n) ?: 'e'
        val e = if (v in "aıou") "an" else "en"
        val last = n.lowercase(TR).last()
        val d = if (last in "çfhkpsşt") "t" else "d"
        return "$n'$d$e"
    }

    /** Bulunma hâli (-de/-da/-te/-ta): "9 Ekim 2027'de", "İzmir'de", "Ahmet'te". */
    fun locative(word: String): String =
        if (!AppLocale.isTurkish()) word.trim() else ablative(word).let { if (it.endsWith("n")) it.dropLast(1) else it }

    /** "2025'ten", "2010'dan", "2000'den": the ending follows how the number is read aloud. */
    private fun numberAblative(digits: String): String {
        val n = digits.toLongOrNull() ?: return "den"
        if (n == 0L) return "dan" // sıfır
        if (n % 1000 == 0L) return "den" // bin, milyon
        if (n % 100 == 0L) return "den" // yüz
        if (n % 10 == 0L) return when ((n / 10 % 10).toInt()) {
            1, 3, 9 -> "dan" // on, otuz, doksan
            4, 6 -> "tan" // kırk, altmış
            7 -> "ten" // yetmiş
            else -> "den" // yirmi, elli, seksen
        }
        return when ((n % 10).toInt()) {
            3, 4, 5 -> "ten" // üç, dört, beş
            6, 9 -> "dan" // altı, dokuz
            else -> "den" // bir, iki, yedi, sekiz
        }
    }
}
