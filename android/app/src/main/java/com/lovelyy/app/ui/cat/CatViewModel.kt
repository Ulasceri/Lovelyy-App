package com.lovelyy.app.ui.cat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.data.CatAction
import com.lovelyy.app.data.CatActionType
import com.lovelyy.app.data.CatDoc
import com.lovelyy.app.data.CatLevels
import com.lovelyy.app.data.CatRules
import com.lovelyy.app.data.UserProfile
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

/** Things the cat screen reacts to once (an animation, a little banner). */
sealed interface CatEvent {
    /** The partner just did something: show it ("Deniz besledi"). */
    data class PartnerAction(val action: CatAction) : CatEvent
    data class Refused(val reason: CatRules.Refusal) : CatEvent
    data object Failed : CatEvent
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
                .catch { events.send(CatEvent.Failed) }
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

    /** Checks locally first so the UI can answer instantly (e.g. "it's asleep"). */
    fun canDo(type: CatActionType): CatRules.Refusal? =
        doc.value?.let { CatRules.refusal(it, type, System.currentTimeMillis()) }

    fun act(me: UserProfile, members: List<String>, type: CatActionType, strokes: Int = 0, name: String? = null) {
        canDo(type)?.let { r -> viewModelScope.launch { events.send(CatEvent.Refused(r)) }; return }
        viewModelScope.launch {
            try {
                repo.act(coupleId, me, members, type, strokes, name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                events.send(CatEvent.Failed)
            }
        }
    }

    companion object {
        const val TICK_MS = 20_000L
        const val PARTNER_EVENT_MAX_AGE_MS = 60_000L
    }
}
