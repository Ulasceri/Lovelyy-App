package com.lovelyy.app.ui.cat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.data.CatAction
import com.lovelyy.app.data.CatActionType
import com.lovelyy.app.data.CatDoc
import com.lovelyy.app.data.CatLevels
import com.lovelyy.app.data.CatRules
import com.lovelyy.app.data.CoupleClock
import com.lovelyy.app.data.OnboardingPrefs
import com.lovelyy.app.data.StreakExplainer
import com.lovelyy.app.data.StreakExplainerSource
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.telemetry.Telemetry
import com.lovelyy.app.widget.CatWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/** Things the cat screen reacts to once (an animation, a little banner). */
sealed interface CatEvent {
    /** The partner just did something: show it ("Deniz besledi"). */
    data class PartnerAction(val action: CatAction) : CatEvent
    data class Refused(val reason: CatRules.Refusal) : CatEvent
    data object Failed : CatEvent

    /** The couple's streak just reached 1 for the first time on this phone: open "Seri nedir?" by itself, once. */
    data object FirstStreak : CatEvent
}

data class CatUi(val doc: CatDoc, val levels: CatLevels)

class CatViewModel(private val app: LovelyyApplication, private val coupleId: String, private val myUid: String) : ViewModel() {

    private val repo = app.catRepository
    private val doc = MutableStateFlow<CatDoc?>(null)
    private val clock = MutableStateFlow(System.currentTimeMillis())
    private val events = Channel<CatEvent>(Channel.BUFFERED)
    val oneTimeEvents = events.receiveAsFlow()

    val ui: StateFlow<CatUi?> = combine(doc, clock) { d, now -> d?.let { CatUi(it, CatRules.levels(it, now)) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            var previous: CatAction? = null
            var first = true
            repo.observe(coupleId)
                .catch {
                    Timber.w(it, "Cat updates failed")
                    events.send(CatEvent.Failed)
                }
                .collect { d ->
                    doc.value = d
                    clock.value = System.currentTimeMillis()
                    val last = d.lastAction
                    if (!first && last != null && last != previous && last.uid != myUid &&
                        System.currentTimeMillis() - last.at < PARTNER_EVENT_MAX_AGE_MS
                    ) {
                        events.send(CatEvent.PartnerAction(last))
                    }
                    previous = last
                    first = false
                    CatWidget.save(app, coupleId, d)
                    maybeExplainStreak(d)
                }
        }
        // Needs decay with time: refresh the computed levels regularly.
        viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                clock.value = System.currentTimeMillis()
            }
        }
    }

    /**
     * Opens "Seri nedir?" (board S1) by itself the first time the shown streak is at least 1. Marked seen before the
     * event is sent so a second screen or a quick re-emit can't show it twice. The screens collect one shared
     * event channel, so only one of them gets it.
     */
    private suspend fun maybeExplainStreak(d: CatDoc) {
        val current = d.streak.current(CoupleClock.today())
        if (!StreakExplainer.shouldAutoShow(current, OnboardingPrefs.streakExplainerSeen(app, coupleId))) return
        OnboardingPrefs.markStreakExplainerSeen(app, coupleId)
        Telemetry.track(AnalyticsEvent.StreakExplainerShown(StreakExplainerSource.Auto.key))
        events.send(CatEvent.FirstStreak)
    }

    /** The streak chip (Yuva) or card (Kedi bakımı) opened "Seri nedir?". After that it never opens by itself. */
    fun streakExplainerOpened(source: StreakExplainerSource) {
        OnboardingPrefs.markStreakExplainerSeen(app, coupleId)
        Telemetry.track(AnalyticsEvent.StreakExplainerShown(source.key))
    }

    /** Checks locally first so the UI can answer instantly (e.g. "it's asleep"). */
    fun canDo(type: CatActionType): CatRules.Refusal? =
        doc.value?.let { CatRules.refusal(it, type, System.currentTimeMillis()) }

    fun act(me: UserProfile, members: List<String>, type: CatActionType, strokes: Int = 0, name: String? = null) {
        canDo(type)?.let { r -> viewModelScope.launch { events.send(CatEvent.Refused(r)) }; return }
        viewModelScope.launch {
            try {
                repo.act(coupleId, me, members, type, strokes, name)
                analyticsEvent(type, strokes)?.let(Telemetry::track)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Cat action %s failed", type)
                events.send(CatEvent.Failed)
            }
        }
    }

    /** Only care actions are tracked. Wake and Name aren't (the name is free text). */
    private fun analyticsEvent(type: CatActionType, strokes: Int): AnalyticsEvent? = when (type) {
        CatActionType.Feed -> AnalyticsEvent.CatFed
        CatActionType.Water -> AnalyticsEvent.CatWatered
        CatActionType.Play -> AnalyticsEvent.CatPlayed
        CatActionType.Pet -> AnalyticsEvent.CatPetted(strokes)
        CatActionType.Sleep -> AnalyticsEvent.CatSlept
        CatActionType.Wake, CatActionType.Name -> null
    }

    companion object {
        const val TICK_MS = 20_000L
        const val PARTNER_EVENT_MAX_AGE_MS = 60_000L
    }
}
