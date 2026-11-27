package com.lovelyy.app.ui.distance

import com.lovelyy.app.text.AppLocale
import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.lovelyy.app.data.DistanceMath
import com.lovelyy.app.data.SharedLocation
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.location.CoarseLocation
import com.lovelyy.app.telemetry.Telemetry
import com.lovelyy.app.ui.components.CenteredTopBar
import com.lovelyy.app.ui.components.HEART_D
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.components.svgPath
import com.lovelyy.app.ui.heartbeat.timeAgo
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate

private val MintCell = Color(0xFFD6F0E8)
private val MintText = Color(0xFF3F5F56)
private val SwitchOn = Color(0xFF3E9C82)
private val SwitchOff = Color(0xFFE3D8D0)
private val HeartOrange = Color(0xFFE07A45)

private const val PIN_D = "M12 21s-7-6-7-11a7 7 0 0 1 14 0c0 5-7 11-7 11z M14.5 10a2.5 2.5 0 1 1-5 0a2.5 2.5 0 1 1 5 0z"
private const val CAR_D = "M5 16l1.5-5a2 2 0 0 1 2-1.5h7a2 2 0 0 1 2 1.5L19 16v3H5z M8 16h.01M16 16h.01"
private const val PLANE_D = "M12 3c1 0 1.5 1 1.5 2.5V9l7 4v2l-7-2v4l2 1.5V20L12 19l-3.5 1v-1.5l2-1.5v-4l-7 2v-2l7-4V5.5C10.5 4 11 3 12 3z"
private const val CAL_D = "M6 5h12a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z M3 10h18M8 3v4M16 3v4"
private const val ARC_D = "M6 44C50 4 120 4 164 44"

class DistanceViewModel(private val app: LovelyyApplication, private val coupleId: String) : ViewModel() {
    val locations: StateFlow<Map<String, SharedLocation>?> = app.locationRepository.observe(coupleId)
        .catch { Timber.w(it, "Locations unavailable") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Only upcoming plans: this screen shows the next one-time meeting, not the calendar. */
    val events: StateFlow<List<CalEvent>?> = app.eventRepository.observeUpcoming(coupleId, com.lovelyy.app.data.CoupleClock.today())
        .catch { Timber.w(it, "Calendar unavailable") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    /** Finds the rough location and shares it. Returns a message res to show on failure. */
    fun share(context: Context, uid: String, cityOnly: Boolean, first: Boolean, onError: (Int) -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val fix = CoarseLocation.current(context)
                if (fix == null) {
                    onError(R.string.ds_no_fix)
                    return@launch
                }
                val city = CoarseLocation.city(context, fix.latitude, fix.longitude)
                runCatching { app.locationRepository.share(coupleId, uid, city, fix.latitude, fix.longitude, cityOnly) }
                    .onSuccess {
                        if (first) Telemetry.track(AnalyticsEvent.LocationSharingChanged(sharing = true))
                        Telemetry.track(AnalyticsEvent.LocationUpdated(cityOnly))
                    }
                    .onFailure {
                        Timber.w(it, "Sharing the location failed")
                        onError(R.string.cat_error)
                    }
            } finally {
                _busy.value = false
            }
        }
    }

    fun stop(uid: String) {
        viewModelScope.launch {
            runCatching { app.locationRepository.stop(coupleId, uid) }
                .onSuccess { Telemetry.track(AnalyticsEvent.LocationSharingChanged(sharing = false)) }
                .onFailure { Timber.w(it, "Stopping location sharing failed") }
        }
    }
}

/** Mesafe (approved design 9): how far apart you are, travel times, the next meeting and the privacy switches. */
@Composable
fun DistanceScreen(me: UserProfile, partner: UserProfile?, onBack: () -> Unit) {
    val coupleId = me.coupleId ?: return
    val app = lovelyyApp()
    val context = LocalContext.current
    val vm: DistanceViewModel = viewModel(key = "distance-$coupleId") { DistanceViewModel(app, coupleId) }
    val locations by vm.locations.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)

    val mine = locations?.get(me.uid)
    val theirs = partner?.uid?.let { locations?.get(it) }
    val sharing = mine != null
    var cityOnlyPref by rememberSaveable { mutableStateOf(true) }
    val cityOnly = mine?.cityOnly ?: cityOnlyPref

    val toast: (Int) -> Unit = { res -> Toast.makeText(context, context.getString(res), Toast.LENGTH_SHORT).show() }
    var pendingFirst by remember { mutableStateOf(true) }
    var pendingOnly by remember { mutableStateOf(true) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.share(context, me.uid, pendingOnly, pendingFirst, toast) else toast(R.string.ds_no_permission)
    }
    fun shareNow(first: Boolean, only: Boolean = cityOnly) {
        if (CoarseLocation.hasPermission(context)) {
            vm.share(context, me.uid, only, first, toast)
        } else {
            pendingFirst = first
            pendingOnly = only
            permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    val km = if (mine != null && theirs != null) DistanceMath.km(mine.lat, mine.lng, theirs.lat, theirs.lng) else null

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CenteredTopBar(stringResource(R.string.ds_title), onBack) {
            if (busy) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Brown, strokeWidth = 2.dp)
                }
            } else {
                RoundSvgButton(PIN_D, stringResource(R.string.ds_refresh), { shareNow(first = !sharing) })
            }
        }

        // Hero: both of you, a dotted arc with a beating heart, and the distance.
        val heroShape = RoundedCornerShape(28.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .softShadow(heroShape, 12.dp)
                .clip(heroShape)
                .background(MintCell)
                .padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Person(me.displayName, Peach, mine?.city)
                Arc(Modifier.weight(1f).aspectRatio(170f / 60f))
                Person(partnerName, Mint, theirs?.city)
            }
            Text(stringResource(R.string.ds_between), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MintText)
            if (km != null) {
                Text(
                    pluralStringResource(R.plurals.ds_km, km.toInt(), km.toInt()),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 64.sp, lineHeight = 66.sp, color = Brown,
                )
                val last = minOf(mine!!.at, theirs!!.at)
                Text(stringResource(R.string.ds_updated, timeAgo(last)), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MintText)
            } else {
                Text(
                    if (mine == null) stringResource(R.string.ds_waiting_me) else stringResource(R.string.ds_waiting_partner, partnerName),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, color = Brown,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 14.dp),
                )
            }
        }

        // Travel times (estimated from the straight-line distance).
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TravelCard(CAR_D, stringResource(R.string.ds_car), km?.let { duration(DistanceMath.driveMinutes(it)) }, Modifier.weight(1f))
            TravelCard(PLANE_D, stringResource(R.string.ds_plane), km?.let { k -> DistanceMath.flightMinutes(k)?.let { duration(it) } }, Modifier.weight(1f))
        }

        // Next meeting: the nearest one-time plan from the calendar.
        val today = com.lovelyy.app.ui.coupleToday()
        val next = events.orEmpty().filter { !it.yearly }
            .mapNotNull { e -> CalendarMath.nextOccurrence(e, today)?.let { e to it } }
            .minByOrNull { it.second }
        WhiteCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(CAL_D, 46.dp)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.ds_next_meet), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft)
                    Text(
                        next?.second?.format(AppLocale.formatter("dMMMMEEEE")) ?: stringResource(R.string.ds_no_meet),
                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = if (next != null) 16.sp else 14.sp, color = Brown,
                    )
                }
                if (next != null) {
                    val days = java.time.temporal.ChronoUnit.DAYS.between(today, next.second).toInt()
                    Text(
                        if (days == 0) stringResource(R.string.ds_meet_today) else pluralStringResource(R.plurals.ds_meet_days, days.toInt(), days),
                        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(PeachSoft).padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }

        // Privacy switches.
        WhiteCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SwitchRow(stringResource(R.string.ds_share), stringResource(R.string.ds_share_sub, partnerName), sharing || busy) { on ->
                    if (on) shareNow(first = true) else vm.stop(me.uid)
                }
                SwitchRow(stringResource(R.string.ds_city_only), stringResource(R.string.ds_city_only_sub), cityOnly) { on ->
                    cityOnlyPref = on
                    if (sharing) shareNow(first = false, only = on)
                }
            }
        }
    }
}

@Composable
private fun duration(minutes: Long): String {
    val h = (minutes / 60).toInt()
    val m = (minutes % 60).toInt()
    return if (h > 0) stringResource(R.string.ds_duration, h, m) else pluralStringResource(R.plurals.ds_duration_min, m.toInt(), m)
}

@Composable
private fun Person(name: String, color: Color, city: String?) {
    Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(color).border(3.dp, Cream, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.trim().firstOrNull()?.uppercase() ?: "?", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown)
        }
        Text(
            city?.takeIf { it.isNotBlank() } ?: if (city == null) "–" else stringResource(R.string.ds_no_city),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

/** The design's dotted arc (170×60) with the heart beating at its top. */
@Composable
private fun Arc(modifier: Modifier) {
    val arc = remember { svgPath(ARC_D) }
    val heart = remember { svgPath(HEART_D) }
    val beat by rememberInfiniteTransition(label = "arcBeat").animateFloat(
        1f, 1f,
        infiniteRepeatable(keyframes { durationMillis = 1800; 1f at 0; 1.12f at 270; 1f at 540; 1.08f at 810; 1f at 1080 }),
        label = "s",
    )
    Canvas(modifier) {
        val s = size.width / 170f
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            drawPath(
                arc, Brown,
                style = Stroke(2.5f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 8f))),
            )
            scale(beat, beat, pivot = Offset(85f, 14f)) {
                translate(73f, 2f) { drawPath(heart, HeartOrange) }
            }
        }
    }
}

@Composable
private fun WhiteCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxWidth().softShadow(shape, 6.dp).clip(shape).background(CreamCard).padding(16.dp)) { content() }
}

@Composable
private fun IconBadge(d: String, size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(PeachSoft), contentAlignment = Alignment.Center) {
        SvgStrokeIcon(d, Brown, Modifier.size(22.dp))
    }
}

@Composable
private fun TravelCard(icon: String, label: String, value: String?, modifier: Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.softShadow(shape, 6.dp).clip(shape).background(CreamCard).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconBadge(icon, 42.dp)
        Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft)
        Text(value ?: "–", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Brown, maxLines = 1)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown)
            Text(subtitle, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrownSoft)
        }
        DesignSwitch(checked)
    }
}

/** 52×30 switch from the design: green when on, white knob. */
@Composable
private fun DesignSwitch(checked: Boolean) {
    val track by animateColorAsState(if (checked) SwitchOn else SwitchOff, label = "track")
    val x by animateDpAsState(if (checked) 25.dp else 3.dp, label = "knob")
    Box(Modifier.size(52.dp, 30.dp).clip(RoundedCornerShape(15.dp)).background(track)) {
        Box(
            Modifier
                .offset(x = x, y = 3.dp)
                .size(24.dp)
                .softShadow(CircleShape, 2.dp)
                .clip(CircleShape)
                .background(CreamCard),
        )
    }
}
