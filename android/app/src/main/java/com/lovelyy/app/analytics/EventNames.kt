package com.lovelyy.app.analytics

/** Checks an event against the naming rules in docs/analytics.md. Used by unit tests. */
object EventNames {
    private val SNAKE = Regex("^[a-z][a-z0-9_]*$")
    private val RESERVED_PREFIXES = listOf("firebase_", "google_", "ga_")
    const val MAX_NAME = 40
    const val MAX_PARAM_NAME = 40
    const val MAX_PARAMS = 25

    fun problems(event: AnalyticsEvent): List<String> = problems(event.name, event.params.keys)

    fun problems(name: String, paramKeys: Collection<String>): List<String> = buildList {
        if (!SNAKE.matches(name)) add("name '$name' is not snake_case")
        if (name.length > MAX_NAME) add("name '$name' is longer than $MAX_NAME")
        if (RESERVED_PREFIXES.any { name.startsWith(it) }) add("name '$name' uses a reserved prefix")
        if (paramKeys.size > MAX_PARAMS) add("more than $MAX_PARAMS params")
        for (key in paramKeys) {
            if (!SNAKE.matches(key)) add("param '$key' is not snake_case")
            if (key.length > MAX_PARAM_NAME) add("param '$key' is longer than $MAX_PARAM_NAME")
        }
    }
}
