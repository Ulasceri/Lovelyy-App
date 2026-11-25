package com.lovelyy.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lovelyy.app.R
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.DayCounter
import com.lovelyy.app.data.Emotion
import com.lovelyy.app.data.MoodEntry
import com.lovelyy.app.data.Signal
import com.lovelyy.app.data.SignalKind
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.signals.labelRes
import com.lovelyy.app.ui.cat.CatSection
import com.lovelyy.app.ui.components.CircleIconButton
import com.lovelyy.app.ui.components.IconCircle
import com.lovelyy.app.ui.components.Pill
import com.lovelyy.app.ui.components.SoftCard
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.heartbeat.EmotionVisual
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.MintInkSoft
import com.lovelyy.app.ui.theme.MintSoft
import com.lovelyy.app.ui.theme.MintTint
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachCard
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.PeachTint
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Bottom-nav tabs. The center heart is not a tab: it opens the heartbeat screen. */
private enum class Tab { Yuva, Anilar, Planlar, Biz }

/** Everything after pairing: the home tabs. */
@Composable
fun HomeScreen(me: UserProfile, couple: Couple?, partner: UserProfile?) {
    val app = lovelyyApp()
    val context = LocalContext.current
    val coupleId = me.coupleId ?: return

    var tab by rememberSaveable { mutableStateOf(Tab.Yuva) }
    BackHandler(enabled = tab != Tab.Yuva) { tab = Tab.Yuva }

    // Features behind the home cards are designed and approved one by one; until then a tap says "Yakında".
    val soonText = stringResource(R.string.home_soon_toast)
    val soon: () -> Unit = { Toast.makeText(context, soonText, Toast.LENGTH_SHORT).show() }

    // Partner signals: listen while the app process lives, ask once for notification permission.
    LaunchedEffect(coupleId, me.uid) { app.signalCenter.start(coupleId, me.uid) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Signed out or offline: keep the last moods instead of crashing on PERMISSION_DENIED.
    val moods by remember(coupleId) { app.moodRepository.observe(coupleId).catch { } }.collectAsStateWithLifecycle(emptyMap())

    Box(Modifier.fillMaxSize().background(Cream)) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { t ->
                when (t) {
                    Tab.Yuva -> YuvaTab(me, couple, partner, moods, soon)
                    Tab.Anilar -> ComingSoon(stringResource(R.string.tab_memories), stringResource(R.string.tab_memories_soon), Icons.Rounded.Photo, Modifier.padding(top = 40.dp))
                    Tab.Planlar -> ComingSoon(stringResource(R.string.tab_plans), stringResource(R.string.tab_plans_soon), Icons.Rounded.CalendarMonth, Modifier.padding(top = 40.dp))
                    Tab.Biz -> BizTab(me, couple, partner)
                }
            }
        }
        Box(Modifier.align(Alignment.BottomCenter)) {
            BottomNav(tab, onTab = { tab = it }, onHeart = soon)
        }
    }
}

// ---------- bottom navigation ----------

@Composable
private fun BottomNav(selected: Tab, onTab: (Tab) -> Unit, onHeart: () -> Unit) {
    val shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(top = 28.dp)
                .fillMaxWidth()
                .softShadow(shape, 16.dp)
                .clip(shape)
                .background(CreamCard)
                .navigationBarsPadding()
                .height(78.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Rounded.Home, stringResource(R.string.tab_home), selected == Tab.Yuva, Modifier.weight(1f)) { onTab(Tab.Yuva) }
            NavItem(Icons.Rounded.Photo, stringResource(R.string.tab_memories), selected == Tab.Anilar, Modifier.weight(1f)) { onTab(Tab.Anilar) }
            Spacer(Modifier.weight(1f))
            NavItem(Icons.Rounded.CalendarMonth, stringResource(R.string.tab_plans), selected == Tab.Planlar, Modifier.weight(1f)) { onTab(Tab.Planlar) }
            NavItem(Icons.Rounded.Groups, stringResource(R.string.tab_us), selected == Tab.Biz, Modifier.weight(1f)) { onTab(Tab.Biz) }
        }
        // The raised, beating heart in the middle: send a heartbeat.
        val beat by rememberInfiniteTransition(label = "navBeat").animateFloat(
            1f, 1f,
            infiniteRepeatable(keyframes { durationMillis = 1800; 1f at 0; 1.12f at 270; 1f at 540; 1.08f at 810; 1f at 1080 }),
            label = "s",
        )
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = 2.dp)
                .size(64.dp)
                .scale(beat)
                .softShadow(CircleShape, 14.dp, Peach)
                .clip(CircleShape)
                .background(Peach)
                .border(4.dp, Cream, CircleShape)
                .bounceClick(onClick = onHeart),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Favorite, stringResource(R.string.nav_send_heartbeat), tint = Brown, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.bounceClick(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(width = 52.dp, height = 30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(if (selected) PeachSoft else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (selected) Brown else BrownSoft, modifier = Modifier.size(22.dp)) }
        Text(
            label, fontFamily = Quicksand, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) Brown else BrownSoft,
        )
    }
}

// ---------- Yuva (home tab) ----------

@Composable
private fun YuvaTab(
    me: UserProfile,
    couple: Couple?,
    partner: UserProfile?,
    moods: Map<String, MoodEntry>,
    soon: () -> Unit,
) {
    val app = lovelyyApp()
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val startMillis = couple?.startDate?.toDate()?.time
    val recent by app.signalCenter.recent.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 120.dp),
    ) {
        Header(me, partnerName, startMillis, onDays = soon, onBell = soon)

        CatSection(me, couple, partner, Modifier.padding(top = 16.dp))

        TogetherCard(startMillis, Modifier.padding(top = 18.dp), soon)

        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeartbeatCard(
                last = recent.firstOrNull { it.to == me.uid && it.kind == SignalKind.Heartbeat },
                partnerName = partnerName,
                modifier = Modifier.weight(1f),
                onClick = { soon() },
            )
            NudgeCard(me, partner, Modifier.weight(1f), compact = true, onSoon = soon)
        }

        MoodCard(me, partnerName, moods[me.uid], partner?.uid?.let { moods[it] }, Modifier.padding(top = 14.dp), soon)

        Text(
            stringResource(R.string.home_corner),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
        )
        // Route = null means "not built yet": shows the "Yakında" pill and a toast.
        val corner = listOf(
            CornerItem(Icons.Rounded.QuestionAnswer, R.string.feature_question, PeachTint),
            CornerItem(Icons.Rounded.Brush, R.string.feature_drawing, MintTint),
            CornerItem(Icons.Rounded.EditNote, R.string.feature_notes, PeachTint),
            CornerItem(Icons.Rounded.Event, R.string.feature_calendar, MintTint),
            CornerItem(Icons.Rounded.Restaurant, R.string.feature_food, PeachTint),
            CornerItem(Icons.Rounded.Place, R.string.feature_distance, MintTint),
            CornerItem(Icons.Rounded.HourglassTop, R.string.feature_capsule, PeachTint),
            CornerItem(Icons.Rounded.Star, R.string.feature_bucket, MintTint),
            CornerItem(Icons.Rounded.Checkroom, R.string.feature_wardrobe, PeachTint),
        )
        corner.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                row.forEach { item ->
                    CornerTile(item.icon, stringResource(item.title), item.tint, Modifier.weight(1f), ready = item.route != null) {
                        soon()
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(me: UserProfile, partnerName: String, startMillis: Long?, onDays: () -> Unit, onBell: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(70.dp)) {
            Avatar(me.displayName, Peach)
            Avatar(partnerName, Mint, Modifier.offset(x = 28.dp))
        }
        Column(Modifier.weight(1f).bounceClick(pressedScale = 0.98f, onClick = onDays)) {
            Text(
                stringResource(R.string.home_us, me.displayName, partnerName),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (startMillis != null) stringResource(R.string.home_day_number, DayCounter.together(startMillis, System.currentTimeMillis()).days)
                else stringResource(R.string.home_set_date),
                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft,
            )
        }
        CircleIconButton(Icons.Rounded.Notifications, stringResource(R.string.home_inbox), onBell)
    }
}

@Composable
private fun Avatar(name: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(42.dp).clip(CircleShape).background(color).border(3.dp, Cream, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.trim().firstOrNull()?.uppercase() ?: "?", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brown)
    }
}

/** "Birlikte 412 gün · 06:23:08", ticking every second. */
@Composable
private fun TogetherCard(startMillis: Long?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startMillis) {
        while (startMillis != null) { now = System.currentTimeMillis(); delay(1000) }
    }
    SoftCard(modifier.fillMaxWidth(), padding = 0.dp, onClick = onClick) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.counter_together), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrownSoft)
                if (startMillis != null) {
                    val t = DayCounter.together(startMillis, now)
                    Text(stringResource(R.string.counter_days, t.days), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Brown)
                } else {
                    Text(stringResource(R.string.counter_set_cta), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown)
                }
            }
            if (startMillis != null) {
                val t = DayCounter.together(startMillis, now)
                Text(
                    "%02d : %02d : %02d".format(t.hours, t.minutes, t.seconds),
                    fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BrownSoft,
                )
            } else {
                Pill(stringResource(R.string.counter_set_short), background = PeachCard)
            }
        }
    }
}

@Composable
private fun HeartbeatCard(last: Signal?, partnerName: String, modifier: Modifier, onClick: (Emotion?) -> Unit) {
    SoftCard(modifier, color = MintSoft, padding = 16.dp) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(CreamCard), contentAlignment = Alignment.Center) {
            EmotionVisual(last?.emotion ?: Emotion.Sevgi, Modifier.size(36.dp), heartScale = 0.5f)
        }
        Text(
            stringResource(R.string.feature_heartbeat),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            if (last != null) stringResource(R.string.home_hb_last, last.fromName.ifBlank { partnerName }, stringResource((last.emotion ?: Emotion.Sevgi).labelRes()), relativeTime(last.at))
            else stringResource(R.string.home_hb_none, partnerName),
            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, color = MintInkSoft,
            modifier = Modifier.padding(top = 6.dp).height(54.dp),
        )
        Box(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CreamCard)
                .bounceClick { onClick(last?.emotion) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(if (last != null) R.string.home_hb_reply else R.string.home_hb_send),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown,
            )
        }
    }
}

@Composable
private fun MoodCard(me: UserProfile, partnerName: String, mine: MoodEntry?, theirs: MoodEntry?, modifier: Modifier, onClick: () -> Unit) {
    SoftCard(modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_mood_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown, modifier = Modifier.weight(1f))
            Text(
                stringResource(if (mine == null) R.string.home_mood_share else R.string.home_mood_change),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown,
            )
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MoodTile(stringResource(R.string.cat_you), mine, PeachTint, BrownSoft, Modifier.weight(1f))
            MoodTile(partnerName, theirs, MintTint, MintInkSoft, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MoodTile(who: String, entry: MoodEntry?, bg: Color, labelColor: Color, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(18.dp)).background(bg).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(entry?.kind?.emoji ?: "·", fontSize = 26.sp, color = BrownSoft)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(who, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = labelColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                entry?.let { moodLabel(it.kind) } ?: stringResource(R.string.mood_unknown),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private class CornerItem(val icon: ImageVector, @androidx.annotation.StringRes val title: Int, val tint: Color, val route: String? = null)

@Composable
private fun CornerTile(icon: ImageVector, title: String, tint: Color, modifier: Modifier, ready: Boolean, onClick: () -> Unit) {
    SoftCard(modifier, radius = 20.dp, padding = 0.dp, onClick = onClick) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconCircle(icon, tint, size = 48.dp, iconSize = 24.dp)
            Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            // Built tiles keep an invisible pill so every tile in a row stays the same height.
            Box(Modifier.alpha(if (ready) 0f else 1f)) {
                Pill(stringResource(R.string.home_soon), background = Color(0xFFF6EEE8), color = BrownSoft, fontSize = 11)
            }
        }
    }
}

// ---------- Biz (us) tab ----------

@Composable
private fun BizTab(me: UserProfile, couple: Couple?, partner: UserProfile?) {
    val app = lovelyyApp()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    val startMillis = couple?.startDate?.toDate()?.time
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SoftCard(Modifier.fillMaxWidth(), color = PeachCard) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(92.dp)) {
                    Avatar(me.displayName, Peach, Modifier.scale(1.3f))
                    Avatar(partnerName, Mint, Modifier.offset(x = 40.dp).scale(1.3f))
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_us, me.displayName, partnerName), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown)
                    Text(
                        if (startMillis != null) stringResource(R.string.biz_since, formatDate(startMillis))
                        else stringResource(R.string.home_set_date),
                        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft,
                    )
                }
            }
        }
        BizRow(Icons.Rounded.Notifications, stringResource(R.string.biz_notifications)) {
            val i = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            runCatching { context.startActivity(i) }
        }
        Text(
            stringResource(R.string.biz_profile_soon),
            fontFamily = Quicksand, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = BrownSoft,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF6EEE8))
                .bounceClick { scope.launch { app.authRepository.signOut(context) } },
            contentAlignment = Alignment.Center,
        ) { Text(stringResource(R.string.sign_out), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown) }
    }
}

@Composable
private fun BizRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth(), padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconCircle(icon, PeachTint)
            Spacer(Modifier.width(14.dp))
            Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = BrownSoft)
        }
    }
}
