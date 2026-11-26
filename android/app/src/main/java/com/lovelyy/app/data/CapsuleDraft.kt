package com.lovelyy.app.data

/** The unsealed letter, kept on this phone only (SharedPreferences). */
object CapsuleDraft {
    private const val PREFS = "capsule_draft"

    fun load(context: android.content.Context): Triple<String, String, String?> {
        val p = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        return Triple(p.getString("title", "").orEmpty(), p.getString("text", "").orEmpty(), p.getString("date", null))
    }

    fun save(context: android.content.Context, title: String, text: String, date: String) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).edit()
            .putString("title", title).putString("text", text).putString("date", date).apply()
    }

    fun clear(context: android.content.Context) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).edit().clear().apply()
    }
}
