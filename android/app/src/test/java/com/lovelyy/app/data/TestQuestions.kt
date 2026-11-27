package com.lovelyy.app.data

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Unit tests cannot read Android resources, so the daily questions are parsed straight from
 * src/main/res/values/questions.xml (Android escapes resolved) and given to [QuestionBank.init].
 */
object TestQuestions {
    fun load(): List<String> {
        val file = listOf("src/main/res/values/questions.xml", "app/src/main/res/values/questions.xml")
            .map(::File).first { it.exists() }
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val array = (0 until doc.getElementsByTagName("string-array").length)
            .map { doc.getElementsByTagName("string-array").item(it) }
            .first { it.attributes.getNamedItem("name").nodeValue == "daily_questions" }
        val items = (array as org.w3c.dom.Element).getElementsByTagName("item")
        return (0 until items.length).map { unescape(items.item(it).textContent.trim()) }
    }

    /** Android resource escapes: \' \" \\ \n \@ \? (and \t). */
    fun unescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                i++
                sb.append(when (val n = s[i]) { 'n' -> '\n'; 't' -> '\t'; else -> n })
            } else sb.append(c)
            i++
        }
        return sb.toString()
    }

    /** Initialises [QuestionBank] from questions.xml; call before using it in a test. */
    fun init(): List<String> = load().also { QuestionBank.init(it) }
}
