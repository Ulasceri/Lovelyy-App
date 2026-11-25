package com.lovelyy.app.signals

import androidx.annotation.StringRes
import com.lovelyy.app.R
import com.lovelyy.app.data.Emotion
import com.lovelyy.app.data.MoodKind

@StringRes
fun Emotion.labelRes(): Int = when (this) {
    Emotion.Ozlem -> R.string.emotion_ozlem
    Emotion.Sevgi -> R.string.emotion_sevgi
    Emotion.Ask -> R.string.emotion_ask
    Emotion.Sarilma -> R.string.emotion_sarilma
    Emotion.Opucuk -> R.string.emotion_opucuk
    Emotion.Huzur -> R.string.emotion_huzur
}

/** "%1$s seni çok özledi…" */
@StringRes
fun Emotion.messageRes(): Int = when (this) {
    Emotion.Ozlem -> R.string.emotion_ozlem_msg
    Emotion.Sevgi -> R.string.emotion_sevgi_msg
    Emotion.Ask -> R.string.emotion_ask_msg
    Emotion.Sarilma -> R.string.emotion_sarilma_msg
    Emotion.Opucuk -> R.string.emotion_opucuk_msg
    Emotion.Huzur -> R.string.emotion_huzur_msg
}

@StringRes
fun MoodKind.labelRes(): Int = when (this) {
    MoodKind.Mutlu -> R.string.mood_kind_mutlu
    MoodKind.Asik -> R.string.mood_kind_asik
    MoodKind.Huzurlu -> R.string.mood_kind_huzurlu
    MoodKind.Heyecanli -> R.string.mood_kind_heyecanli
    MoodKind.Enerjik -> R.string.mood_kind_enerjik
    MoodKind.Ozlemli -> R.string.mood_kind_ozlemli
    MoodKind.Yorgun -> R.string.mood_kind_yorgun
    MoodKind.Uzgun -> R.string.mood_kind_uzgun
    MoodKind.Stresli -> R.string.mood_kind_stresli
    MoodKind.Sinirli -> R.string.mood_kind_sinirli
    MoodKind.Sikkin -> R.string.mood_kind_sikkin
    MoodKind.Hasta -> R.string.mood_kind_hasta
}
