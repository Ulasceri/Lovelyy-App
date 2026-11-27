package com.lovelyy.app.ui.plans

import com.lovelyy.app.text.AppLocale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.LovelyyApplication
import com.lovelyy.app.R
import com.lovelyy.app.analytics.AnalyticsEvent
import com.lovelyy.app.data.CalEvent
import com.lovelyy.app.data.CalendarMath
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.DayCounter
import com.lovelyy.app.data.EventConflictException
import com.lovelyy.app.data.EventRepository
import com.lovelyy.app.data.ItemGoneException
import com.lovelyy.app.data.SignalKind
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.telemetry.Telemetry
import com.lovelyy.app.ui.components.LovelyyDatePicker
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachDeep
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.YearMonth

private val MintCell = Color(0xFFD6F0E8)
private val ButterCell = Color(0xFFFFF1C9)
private val CellLabel = Color(0xFF5F524C)
private val DotGreen = Color(0xFF3E9C82)
private val FieldBorder = Color(0xFFF0E2D8)

private const val PLUS_D = "M12 5v14M5 12h14"
private const val LEFT_D = "M15 5l-7 7 7 7"
private const val RIGHT_D = "M9 5l7 7-7 7"
private const val STAR_D = "M12 4l2.5 5 5.5.8-4 3.9.9 5.5-4.9-2.6-4.9 2.6.9-5.5-4-3.9 5.5-.8z"
private const val WHEEL_D = "M12 3.5a8.5 8.5 0 1 1 0 17a8.5 8.5 0 1 1 0-17zM12 3.5v17M3.5 12h17M6 6l12 12M18 6L6 18"
private const val BUILT_IN_ANNIVERSARY = "anniversary"

class PlansViewModel(private val app: LovelyyApplication, private val coupleId: String) : ViewModel() {
    private val month = kotlinx.coroutines.flow.MutableStateFlow(YearMonth.from(com.lovelyy.app.data.CoupleClock.today()))

    /** The month on screen; its plans are loaded for the calendar dots. */
    fun showMonth(m: YearMonth) { month.value = m }

    /**
     * What the tab shows, instead of every plan ever: all yearly special days (countdowns, dots in any month), the
     * next upcoming plans, and the plans of the month on screen.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val events: StateFlow<List<CalEvent>?> = kotlinx.coroutines.flow.combine(
        app.eventRepository.observeYearly(coupleId),
        app.eventRepository.observeUpcoming(coupleId, com.lovelyy.app.data.CoupleClock.today()),
        month.flatMapLatest { m -> app.eventRepository.observeRange(coupleId, m.atDay(1), m.atEndOfMonth()) },
    ) { yearly, upcoming, inMonth -> (yearly + upcoming + inMonth).distinctBy { it.id } }
        .catch { Timber.w(it, "Calendar unavailable") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Every plan, counted on the server (the free limit counts them all); re-asked when the shown ones change. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val total: StateFlow<Long?> = events
        .map { list -> list?.map { it.id }?.toSet() }
        .distinctUntilChanged()
        .mapLatest { ids -> if (ids == null) null else app.eventRepository.count(coupleId) }
        .catch { Timber.w(it, "Plan count unavailable") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** A plan open in the editor left the loaded set: only "deleted" if the server agrees. */
    suspend fun stillExists(id: String): Boolean = app.eventRepository.exists(coupleId, id)

    /**
     * [opened]: the plan as it was when the editor opened (null for a new one). [onGone]: the partner deleted
     * it meanwhile (it is not re-created). [onConflict]: the partner changed it meanwhile; gets the server
     * version. [force] overwrites.
     */
    fun save(
        me: UserProfile,
        partner: UserProfile?,
        opened: CalEvent?,
        title: String,
        date: LocalDate,
        yearly: Boolean,
        force: Boolean = false,
        onGone: () -> Unit,
        onConflict: (CalEvent) -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching { app.eventRepository.save(coupleId, me, opened, title, date, "", yearly, force) }
                .onSuccess {
                    if (opened == null) {
                        Telemetry.track(AnalyticsEvent.PlanAdded(yearly))
                        if (partner != null) {
                            runCatching { app.signalRepository.send(coupleId, me, partner.uid, SignalKind.Event, text = title.trim()) }
                        }
                    }
                }
                .onFailure {
                    when (it) {
                        is EventConflictException -> onConflict(it.current)
                        is ItemGoneException -> { Timber.w(it, "Saving the plan failed"); onGone() }
                        else -> Timber.w(it, "Saving the plan failed")
                    }
                }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { app.eventRepository.delete(coupleId, id) }.onFailure { Timber.w(it, "Deleting the plan failed") }
        }
    }
}

private data class Editing(val event: CalEvent?, val date: LocalDate)

/** An edit that hit a newer change by the partner: [current] is the server version, the rest is my unsaved edit. */
private data class PlanConflict(val current: CalEvent, val title: String, val date: LocalDate, val yearly: Boolean)

/** Planlar tab (approved design 11): countdowns to special days, the month, upcoming plans. */
@Composable
fun PlansScreen(me: UserProfile, couple: Couple?, partner: UserProfile?, onBucket: () -> Unit, onWheel: () -> Unit) {
    val app = lovelyyApp()
    val coupleId = couple?.id ?: me.coupleId ?: return
    val vm: PlansViewModel = viewModel(key = "plans-$coupleId") { PlansViewModel(app, coupleId) }
    val stored by vm.events.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val today = com.lovelyy.app.ui.coupleToday()
    val anniversaryTitle = stringResource(R.string.p_anniversary)

    // The day counter's start date is the built-in anniversary.
    val anniversary = couple?.startDate?.toDate()?.time?.let {
        CalEvent(BUILT_IN_ANNIVERSARY, anniversaryTitle, DayCounter.dateOf(it), "", yearly = true, by = "", byName = "", builtIn = true)
    }
    val all = listOfNotNull(anniversary) + stored.orEmpty()
    val special = all.filter { it.yearly }.mapNotNull { e -> CalendarMath.daysUntil(e, today)?.let { e to it } }.sortedBy { it.second }.take(3)
    val plans = all.filter { !it.yearly }.mapNotNull { e -> CalendarMath.daysUntil(e, today)?.let { e to it } }.sortedBy { it.second }.take(6)

    var monthKey by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = YearMonth.parse(monthKey)
    androidx.compose.runtime.LaunchedEffect(month) { vm.showMonth(month) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    // P9: a new plan or special day over the free limit opens the plans; existing ones stay editable.
    val gate = com.lovelyy.app.ui.plus.rememberLimitGate(com.lovelyy.app.data.PlusLimits.Limit.Events, maxOf(total?.toInt() ?: 0, stored?.size ?: 0))
    fun startNew(date: LocalDate) = gate.tryAdd { editing = Editing(null, date) }

    // "Emin misin?" before deleting.
    var askDelete by remember { mutableStateOf<(() -> Unit)?>(null) }
    askDelete?.let { act ->
        com.lovelyy.app.ui.components.ConfirmDialog(
            title = stringResource(R.string.del_plan_title),
            body = stringResource(R.string.del_plan_body),
            confirm = stringResource(R.string.del_confirm),
            onConfirm = { act(); askDelete = null },
            onDismiss = { askDelete = null; editing = null },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
                Initial(me.displayName, Peach)
                Initial(partnerName, Mint)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.p_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown)
                Text(stringResource(R.string.p_subtitle), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft)
            }
            RoundSvgButton(PLUS_D, stringResource(R.string.p_add), { startNew(today) })
        }
        com.lovelyy.app.ui.plus.PlusLimitBanner(gate)

        // Countdowns (same height, even when one title is shorter)
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val colors = listOf(PeachSoft, MintCell, ButterCell)
            if (special.isEmpty()) {
                Countdown(null, stringResource(R.string.p_no_special), "", PeachSoft, Modifier.weight(1f).fillMaxHeight()) { startNew(today) }
            }
            special.forEachIndexed { i, (e, left) ->
                val next = CalendarMath.nextOccurrence(e, today) ?: e.date
                Countdown(left, e.title, next.format(AppLocale.formatter("dMMMM")), colors[i], Modifier.weight(1f).fillMaxHeight()) {
                    if (!e.builtIn) editing = Editing(e, e.date)
                }
            }
        }

        // Month
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier.fillMaxWidth().softShadow(shape, 6.dp).clip(shape).background(CreamCard).padding(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    month.format(AppLocale.formatter("LLLLy")).replaceFirstChar { it.titlecase(AppLocale.current()) },
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown, modifier = Modifier.weight(1f),
                )
                ArrowButton(LEFT_D, stringResource(R.string.p_prev_month)) { monthKey = month.minusMonths(1).toString() }
                ArrowButton(RIGHT_D, stringResource(R.string.p_next_month)) { monthKey = month.plusMonths(1).toString() }
            }
            Row {
                stringArrayResource(R.array.cal_weekdays_short).forEach {
                    Text(it, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrownSoft, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            val offset = month.atDay(1).dayOfWeek.value - 1
            val cells = List(offset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
            cells.chunked(7).forEach { week ->
                Row(Modifier.padding(vertical = 1.dp)) {
                    week.forEach { day -> DayCell(day, today, day != null && all.any { CalendarMath.occursOn(it, day) }, Modifier.weight(1f)) { d -> startNew(d) } }
                    repeat(7 - week.size) { Box(Modifier.weight(1f)) }
                }
            }
        }

        // Upcoming plans
        Column(
            Modifier.fillMaxWidth().softShadow(shape, 6.dp).clip(shape).background(CreamCard).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (plans.isEmpty()) {
                Text(stringResource(R.string.p_no_plans), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrownSoft)
            }
            plans.forEachIndexed { i, (e, left) ->
                val who = if (e.by == me.uid) me.displayName else e.byName.ifBlank { partnerName }
                val whenText = when {
                    left == 0L -> stringResource(R.string.p_today)
                    left == 1L -> stringResource(R.string.p_tomorrow)
                    left < 7L -> e.date.format(AppLocale.formatter("EEEE"))
                    else -> pluralStringResource(R.plurals.p_left, left.toInt(), left.toInt())
                }
                Row(
                    Modifier.fillMaxWidth().bounceClick(pressedScale = 0.98f) { editing = Editing(e, e.date) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        Modifier.size(46.dp).clip(RoundedCornerShape(16.dp)).background(if (i % 2 == 0) MintCell else PeachSoft),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(e.date.dayOfMonth.toString(), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 18.sp, color = Brown)
                        Text(e.date.format(AppLocale.formatter("MMM")).replaceFirstChar { it.titlecase(AppLocale.current()) }, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 12.sp, color = Brown)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(e.title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(R.string.p_added_by, whenText, who), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft, maxLines = 1)
                    }
                    SvgStrokeIcon(RIGHT_D, BrownSoft, Modifier.size(18.dp))
                }
            }
        }

        // Shortcuts to the bucket list and the decision wheel.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LinkButton(STAR_D, stringResource(R.string.p_bucket), Modifier.weight(1f), onBucket)
            LinkButton(WHEEL_D, stringResource(R.string.p_wheel), Modifier.weight(1f), onWheel)
        }
    }

    // The partner deleted the plan that is open here: close the sheet and say so (never re-create it).
    val context = androidx.compose.ui.platform.LocalContext.current
    val goneText = stringResource(R.string.gone_plan)
    val showGone = { android.widget.Toast.makeText(context, goneText, android.widget.Toast.LENGTH_SHORT).show() }
    val editingId = editing?.event?.takeIf { !it.builtIn }?.id
    androidx.compose.runtime.LaunchedEffect(stored, editingId) {
        val list = stored
        if (editingId != null && list != null && list.none { it.id == editingId } && !vm.stillExists(editingId)) { editing = null; showGone() }
    }

    // The partner changed the plan while it was open here: ask before overwriting their version.
    var conflict by remember { mutableStateOf<PlanConflict?>(null) }
    conflict?.let { c ->
        com.lovelyy.app.ui.components.ConfirmDialog(
            title = stringResource(R.string.p_conflict_title),
            body = stringResource(R.string.p_conflict_body, partnerName),
            confirm = stringResource(R.string.n_conflict_overwrite),
            onConfirm = {
                conflict = null
                vm.save(me, partner, c.current, c.title, c.date, c.yearly, force = true, onGone = showGone)
            },
            // Keep the partner version: reopen the editor with it.
            onDismiss = { conflict = null; editing = Editing(c.current, c.current.date) },
        )
    }

    editing?.let { ed ->
        // key(): a reopened editor (after a conflict) starts from the new version, not the old draft.
        androidx.compose.runtime.key(ed.event?.id, ed.event?.updatedAt, ed.event?.title, ed.event?.date, ed.event?.yearly) {
            PlanEditor(
                event = ed.event,
                initialDate = ed.date,
                onDismiss = { editing = null },
                onSave = { title, date, yearly ->
                    vm.save(
                        me, partner, ed.event, title, date, yearly,
                        onGone = showGone,
                        onConflict = { current -> conflict = PlanConflict(current, title, date, yearly) },
                    )
                    editing = null
                },
                onDelete = ed.event?.let { e -> { askDelete = { vm.delete(e.id); editing = null } } },
            )
        }
    }
}

@Composable
private fun Initial(name: String, color: Color) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(color).border(3.dp, Cream, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text(name.trim().firstOrNull()?.uppercase(AppLocale.current()) ?: "?", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown) }
}

@Composable
private fun Countdown(left: Long?, title: String, date: String, bg: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(22.dp)).background(bg).bounceClick(pressedScale = 0.97f, onClick = onClick).padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (left != null) {
            Text(
                left.toString(), style = TextStyle(fontFeatureSettings = "tnum"),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, color = Brown,
            )
            Text(stringResource(if (left == 0L) R.string.p_today_word else R.string.p_days_left), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CellLabel)
        } else {
            SvgStrokeIcon(PLUS_D, Brown, Modifier.size(28.dp))
        }
        Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        if (date.isNotEmpty()) Text(date, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CellLabel)
    }
}

@Composable
private fun ArrowButton(d: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(width = 44.dp, height = 32.dp).semantics { contentDescription = label; role = Role.Button }.bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { SvgStrokeIcon(d, Brown, Modifier.size(18.dp)) }
}

@Composable
private fun DayCell(day: LocalDate?, today: LocalDate, hasEvent: Boolean, modifier: Modifier, onClick: (LocalDate) -> Unit) {
    Box(
        modifier.height(32.dp).then(if (day != null) Modifier.bounceClick { onClick(day) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (day == null) return@Box
        val isToday = day == today
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(if (isToday) Peach else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.dayOfMonth.toString(), style = TextStyle(fontFeatureSettings = "tnum"),
                fontFamily = Quicksand, fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold, fontSize = 14.sp, color = Brown,
            )
        }
        if (hasEvent) Box(Modifier.align(Alignment.BottomCenter).size(5.dp).clip(CircleShape).background(DotGreen))
    }
}

@Composable
private fun LinkButton(icon: String, text: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier.height(48.dp).softShadow(shape, 4.dp).clip(shape).background(CreamCard).semantics { role = Role.Button }.bounceClick(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SvgStrokeIcon(icon, Brown, Modifier.size(18.dp))
        Text(text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown, modifier = Modifier.padding(start = 8.dp), maxLines = 1)
    }
}

/** Adding or editing a plan or special day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanEditor(event: CalEvent?, initialDate: LocalDate, onDismiss: () -> Unit, onSave: (String, LocalDate, Boolean) -> Unit, onDelete: (() -> Unit)?) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var title by rememberSaveable { mutableStateOf(event?.title.orEmpty()) }
    var dateKey by rememberSaveable { mutableStateOf(initialDate.toString()) }
    val date = LocalDate.parse(dateKey)
    var yearly by rememberSaveable { mutableStateOf(event?.yearly == true) }
    var picking by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Cream) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(if (event == null) R.string.p_editor_new else R.string.p_editor_edit), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown)
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val fShape = RoundedCornerShape(18.dp)
            BasicTextField(
                value = title,
                onValueChange = { title = it.take(EventRepository.MAX_TITLE) },
                singleLine = true,
                interactionSource = interaction,
                cursorBrush = SolidColor(PeachDeep),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                textStyle = TextStyle(fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Brown),
                decorationBox = { inner ->
                    Box(
                        Modifier.fillMaxWidth().height(56.dp).clip(fShape).background(CreamCard)
                            .border(2.dp, if (focused) PeachDeep else FieldBorder, fShape).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (title.isEmpty()) Text(stringResource(R.string.p_editor_title_hint), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = BrownSoft)
                        inner()
                    }
                },
            )
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(fShape).background(CreamCard).bounceClick(pressedScale = 0.98f) { picking = true }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.p_editor_date, date.format(AppLocale.formatter("dMMMMyEEEE"))),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown, modifier = Modifier.weight(1f),
                )
                SvgStrokeIcon(RIGHT_D, BrownSoft, Modifier.size(18.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.p_editor_yearly), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown, modifier = Modifier.weight(1f))
                Switch(
                    checked = yearly, onCheckedChange = { yearly = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Peach, checkedThumbColor = CreamCard, uncheckedTrackColor = PeachSoft, uncheckedBorderColor = FieldBorder, uncheckedThumbColor = BrownSoft),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onDelete != null) {
                    Box(
                        Modifier.height(56.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFFF6EEE8))
                            .semantics { role = Role.Button }
                            .bounceClick { scope.launch { sheet.hide() }.invokeOnCompletion { onDelete() } }
                            .padding(horizontal = 22.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.n_delete), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown) }
                }
                Box(
                    Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(28.dp)).background(if (title.isNotBlank()) Peach else PeachSoft)
                        .semantics { role = Role.Button }
                        .bounceClick(enabled = title.isNotBlank(), pressedScale = 0.97f) {
                            scope.launch { sheet.hide() }.invokeOnCompletion { onSave(title, date, yearly) }
                        },
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.n_save), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown) }
            }
            Box(Modifier.width(1.dp).height(4.dp))
        }
    }

    if (picking) {
        LovelyyDatePicker(
            initial = date,
            title = stringResource(R.string.p_pick_date),
            allowFuture = true,
            onDismiss = { picking = false },
            onPick = { dateKey = it.toString(); picking = false },
        )
    }
}
