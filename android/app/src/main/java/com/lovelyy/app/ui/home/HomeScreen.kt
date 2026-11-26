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
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HourglassTop
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
import com.lovelyy.app.ui.cat.CatCareScreen
import com.lovelyy.app.ui.cat.CatSection
import com.lovelyy.app.ui.distance.DistanceScreen
import com.lovelyy.app.ui.profile.ProfileScreen
import com.lovelyy.app.ui.capsule.CapsuleScreen
import com.lovelyy.app.ui.drawing.DrawingScreen
import com.lovelyy.app.ui.dreams.DreamsScreen
import com.lovelyy.app.ui.memories.MemoriesScreen
import com.lovelyy.app.ui.wheel.WheelScreen
import com.lovelyy.app.ui.widgets.WidgetsScreen
import com.lovelyy.app.widget.WidgetData
import com.lovelyy.app.ui.components.CircleIconButton
import com.lovelyy.app.ui.components.IconCircle
import com.lovelyy.app.ui.components.Pill
import com.lovelyy.app.ui.components.SoftCard
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.analytics.InnerScreens
import com.lovelyy.app.ui.capsule.LettersScreen
import com.lovelyy.app.ui.cat.AccessoriesScreen
import com.lovelyy.app.ui.drawing.DrawingsScreen
import com.lovelyy.app.ui.history.HeartbeatHistoryScreen
import com.lovelyy.app.ui.history.MoodHistoryScreen
import com.lovelyy.app.ui.history.QuestionHistoryScreen
import com.lovelyy.app.ui.history.WheelHistoryScreen
import com.lovelyy.app.ui.inbox.InboxScreen
import com.lovelyy.app.ui.profile.AccountScreen
import com.lovelyy.app.ui.profile.UnpairScreen
import com.lovelyy.app.ui.search.SearchScope
import com.lovelyy.app.ui.search.SearchScreen
import com.lovelyy.app.telemetry.Telemetry
import com.lovelyy.app.ui.counter.CounterScreen
import com.lovelyy.app.ui.heartbeat.EmotionVisual
import com.lovelyy.app.ui.heartbeat.HeartbeatScreen
import com.lovelyy.app.ui.mood.MoodFace
import com.lovelyy.app.ui.mood.MoodScreen
import com.lovelyy.app.ui.notes.NotesScreen
import com.lovelyy.app.ui.plans.PlansScreen
import com.lovelyy.app.ui.question.QuestionScreen
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

/** Bottom-nav tabs. The center heart is not a tab: it opens the heartbeat screen. */
private enum class Tab { Yuva, Anilar, Planlar, Biz }

/** Inner screens, opened above the tabs (the bottom menu stays). Strings so they survive rotation. */
private object Routes {
    const val COUNTER = "counter"
    const val HEARTBEAT = "heartbeat" // "heartbeat" or "heartbeat:<emotion key>"
    const val MOOD = "mood"
    const val QUESTION = "question"
    const val NOTES = "notes"
    const val CAT = "cat"
    const val DISTANCE = "distance"
    const val DREAMS = "dreams"
    const val WHEEL = "wheel"
    const val CAPSULE = "capsule"
    const val DRAWING = "drawing"
    const val WIDGETS = "widgets"
    const val MOOD_HISTORY = "mood_history"
    const val QUESTION_HISTORY = "question_history"
    const val HEARTBEAT_HISTORY = "heartbeat_history"
    const val WHEEL_HISTORY = "wheel_history"
    const val SEARCH = "search" // "search:<scope>"
    const val DRAWINGS = "drawings"
    const val LETTERS = "letters" // "letters" or "letters:<capsule id>"
    const val ACCESSORIES = "accessories"
    const val INBOX = "inbox"
    const val ACCOUNT = "account"
    const val UNPAIR = "unpair"
}

/** Everything after pairing: the home tabs and the inner screens. */
@Composable
fun HomeScreen(me: UserProfile, couple: Couple?, partner: UserProfile?) {
    val app = lovelyyApp()
    val context = LocalContext.current
    val coupleId = me.coupleId ?: return

    var tab by rememberSaveable { mutableStateOf(Tab.Yuva) }
    val stack = rememberSaveable(
        saver = listSaver<SnapshotStateList<String>, String>({ it.toList() }, { mutableStateListOf<String>().apply { addAll(it) } }),
    ) { mutableStateListOf<String>() }
    fun push(route: String) {
        if (stack.lastOrNull()?.substringBefore(':') == route.substringBefore(':')) stack.removeAt(stack.lastIndex)
        stack.add(route)
    }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    /** Leaves the current screen for [route]: back to it when it is right underneath, else opens it. */
    fun returnTo(route: String) {
        pop()
        if (stack.lastOrNull()?.substringBefore(':') != route) push(route)
    }
    BackHandler(enabled = stack.isNotEmpty()) { pop() }

    // Widget and notification taps open their screen (DeepLink routes match Routes).
    val pendingRoute by app.pendingRoute.collectAsState()
    LaunchedEffect(pendingRoute) {
        val r = pendingRoute ?: return@LaunchedEffect
        app.pendingRoute.value = null
        stack.clear()
        if (r == PLANS_TAB) tab = Tab.Planlar else push(r)
    }
    BackHandler(enabled = stack.isEmpty() && tab != Tab.Yuva) { tab = Tab.Yuva }

    // screen_view for inner screens (the tabs themselves count as "home").
    val top = stack.lastOrNull()
    LaunchedEffect(top, tab) {
        Telemetry.onScreen(
            when (top?.substringBefore(':')) {
                Routes.COUNTER -> InnerScreens.DAY_COUNTER
                Routes.HEARTBEAT -> InnerScreens.HEARTBEAT
                Routes.MOOD -> InnerScreens.MOOD
                Routes.QUESTION -> InnerScreens.QUESTION
                Routes.NOTES -> InnerScreens.NOTES
                Routes.CAT -> InnerScreens.CAT_CARE
                Routes.DISTANCE -> InnerScreens.DISTANCE
                Routes.DREAMS -> InnerScreens.DREAMS
                Routes.WHEEL -> InnerScreens.WHEEL
                Routes.CAPSULE -> InnerScreens.CAPSULE
                Routes.DRAWING -> InnerScreens.DRAWING
                Routes.WIDGETS -> InnerScreens.WIDGETS
                Routes.MOOD_HISTORY -> InnerScreens.MOOD_HISTORY
                Routes.QUESTION_HISTORY -> InnerScreens.QUESTION_HISTORY
                Routes.HEARTBEAT_HISTORY -> InnerScreens.HEARTBEAT_HISTORY
                Routes.WHEEL_HISTORY -> InnerScreens.WHEEL_HISTORY
                Routes.SEARCH -> InnerScreens.SEARCH
                Routes.DRAWINGS -> InnerScreens.DRAWINGS
                Routes.LETTERS -> InnerScreens.LETTERS
                Routes.ACCESSORIES -> InnerScreens.ACCESSORIES
                Routes.INBOX -> InnerScreens.INBOX
                Routes.ACCOUNT -> InnerScreens.ACCOUNT
                Routes.UNPAIR -> InnerScreens.UNPAIR
                else -> when (tab) {
                    Tab.Anilar -> InnerScreens.MEMORIES
                    Tab.Planlar -> InnerScreens.PLANS
                    Tab.Biz -> InnerScreens.PROFILE
                    else -> "home"
                }
            },
        )
    }

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

    // The home-screen widgets can't read Firestore while the app is closed: keep who the couple is on the phone.
    LaunchedEffect(coupleId, me.uid, me.displayName, partner?.uid, partner?.displayName, couple?.startDate) {
        WidgetData.saveCouple(
            context,
            WidgetData.CoupleInfo(
                coupleId = coupleId,
                myUid = me.uid,
                myName = me.displayName,
                partnerUid = partner?.uid,
                partnerName = partner?.displayName.orEmpty(),
                startMillis = couple?.startDate?.toDate()?.time,
            ),
        )
    }

    // Signed out or offline: keep the last moods instead of crashing on PERMISSION_DENIED.
    val moods by remember(coupleId) { app.moodRepository.observe(coupleId).catch { } }.collectAsStateWithLifecycle(emptyMap())

    Box(Modifier.fillMaxSize().background(Cream)) {
        AnimatedContent(
            targetState = top,
            transitionSpec = {
                if (targetState != null && stack.size > 0) {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 6 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 6 } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
                }
            },
            label = "route",
        ) { route ->
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
                when (route?.substringBefore(':')) {
                    null -> AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { t ->
                        when (t) {
                            Tab.Yuva -> YuvaTab(me, couple, partner, moods, soon, open = ::push, openPlans = { tab = Tab.Planlar })
                            Tab.Anilar -> MemoriesScreen(
                                me, couple, partner,
                                onDrawing = { push(Routes.DRAWING) },
                                onNotes = { push(Routes.NOTES) },
                                onCapsule = { push(Routes.CAPSULE) },
                                onSearch = { push("${Routes.SEARCH}:memories") },
                            )
                            Tab.Planlar -> PlansScreen(me, couple, partner, onBucket = { push(Routes.DREAMS) }, onWheel = { push(Routes.WHEEL) })
                            Tab.Biz -> ProfileScreen(
                                me, couple, partner,
                                onMood = { push(Routes.MOOD) },
                                onQuestion = { push(Routes.QUESTION) },
                                onDistance = { push(Routes.DISTANCE) },
                                onCounter = { push(Routes.COUNTER) },
                                onWidgets = { push(Routes.WIDGETS) },
                                onInbox = { push(Routes.INBOX) },
                                onAccount = { push(Routes.ACCOUNT) },
                                onUnpair = { push(Routes.UNPAIR) },
                            )
                        }
                    }
                    Routes.COUNTER -> CounterScreen(me, couple, partner, onBack = ::pop)
                    Routes.MOOD -> MoodScreen(me, partner, moods, onBack = ::pop, onHistory = { push(Routes.MOOD_HISTORY) })
                    Routes.QUESTION -> QuestionScreen(me, couple, partner, onBack = ::pop, onHistory = { push(Routes.QUESTION_HISTORY) })
                    Routes.NOTES -> NotesScreen(me, partner, onBack = ::pop, onSearch = { push("${Routes.SEARCH}:notes") }, openId = route.substringAfter(':', "").ifEmpty { null })
                    Routes.CAT -> CatCareScreen(me, couple, partner, onBack = ::pop, onAccessories = { push(Routes.ACCESSORIES) })
                    Routes.DISTANCE -> DistanceScreen(me, partner, onBack = ::pop)
                    Routes.DREAMS -> DreamsScreen(me, partner, onBack = ::pop, onSearch = { push("${Routes.SEARCH}:dreams") }, openId = route.substringAfter(':', "").ifEmpty { null })
                    Routes.WHEEL -> WheelScreen(me, partner, onBack = ::pop, onHistory = { push(Routes.WHEEL_HISTORY) })
                    Routes.CAPSULE -> CapsuleScreen(me, partner, onBack = ::pop, onOpenedList = { push(Routes.LETTERS) })
                    Routes.DRAWING -> DrawingScreen(me, partner, onBack = ::pop, onOld = { push(Routes.DRAWINGS) })
                    Routes.WIDGETS -> WidgetsScreen(me, couple, partner)
                    Routes.HEARTBEAT -> HeartbeatScreen(me, partner, Emotion.of(route.substringAfter(':', "")), onHistory = { push(Routes.HEARTBEAT_HISTORY) })
                    Routes.MOOD_HISTORY -> MoodHistoryScreen(me, partner, onBack = ::pop)
                    Routes.QUESTION_HISTORY -> QuestionHistoryScreen(me, partner, onBack = ::pop, onAnswer = { returnTo(Routes.QUESTION) })
                    Routes.HEARTBEAT_HISTORY -> HeartbeatHistoryScreen(me, partner, onBack = ::pop)
                    Routes.WHEEL_HISTORY -> WheelHistoryScreen(me, partner, onBack = ::pop)
                    Routes.SEARCH -> SearchScreen(
                        me, partner, SearchScope.of(route.substringAfter(':', "")), onBack = ::pop,
                        onNote = { id -> pop(); push("${Routes.NOTES}:$id") },
                        onDream = { id -> pop(); push("${Routes.DREAMS}:$id") },
                        onLetter = { id -> push("${Routes.LETTERS}:$id") },
                    )
                    Routes.DRAWINGS -> DrawingsScreen(me, partner, onBack = ::pop, onNew = { returnTo(Routes.DRAWING) })
                    Routes.LETTERS -> LettersScreen(me, partner, route.substringAfter(':', "").ifEmpty { null }, onBack = ::pop)
                    Routes.ACCESSORIES -> AccessoriesScreen(me, partner, onBack = ::pop)
                    Routes.INBOX -> InboxScreen(me, partner, onBack = ::pop, onOpen = { r -> if (r == PLANS_TAB) { stack.clear(); tab = Tab.Planlar } else push(r) })
                    Routes.ACCOUNT -> AccountScreen(me, couple, partner, onBack = ::pop)
                    Routes.UNPAIR -> UnpairScreen(me, partner, onBack = ::pop)
                }
            }
        }
        Box(Modifier.align(Alignment.BottomCenter)) {
            // Inner screens light up the tab they belong to (design: counter → Yuva, mood → Biz, heartbeat → the heart).
            val routeKey = top?.substringBefore(':')
            val navTab = when (routeKey) {
                null -> tab
                Routes.COUNTER, Routes.CAT, Routes.ACCESSORIES, Routes.INBOX -> Tab.Yuva
                Routes.MOOD, Routes.QUESTION, Routes.DISTANCE, Routes.WIDGETS,
                Routes.MOOD_HISTORY, Routes.QUESTION_HISTORY, Routes.ACCOUNT, Routes.UNPAIR -> Tab.Biz
                Routes.NOTES, Routes.CAPSULE, Routes.DRAWING, Routes.DRAWINGS, Routes.LETTERS -> Tab.Anilar
                Routes.DREAMS, Routes.WHEEL, Routes.WHEEL_HISTORY -> Tab.Planlar
                Routes.SEARCH -> if (top?.substringAfter(':') == "dreams") Tab.Planlar else Tab.Anilar
                else -> null
            }
            BottomNav(
                navTab,
                heartActive = routeKey == Routes.HEARTBEAT || routeKey == Routes.HEARTBEAT_HISTORY,
                onTab = { stack.clear(); tab = it },
                onHeart = { push(Routes.HEARTBEAT) },
            )
        }
    }
}

// ---------- bottom navigation ----------

@Composable
private fun BottomNav(selected: Tab?, heartActive: Boolean, onTab: (Tab) -> Unit, onHeart: () -> Unit) {
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
                .scale(if (heartActive) 1f else beat)
                .softShadow(CircleShape, 14.dp, Peach)
                .clip(CircleShape)
                .background(Peach)
                .border(4.dp, if (heartActive) Brown else Cream, CircleShape)
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
    open: (String) -> Unit,
    openPlans: () -> Unit,
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
        Header(me, partnerName, startMillis, onDays = { open(Routes.COUNTER) }, onBell = { open(Routes.INBOX) })

        CatSection(me, couple, partner, Modifier.padding(top = 16.dp), onOpenCare = { open(Routes.CAT) })

        TogetherCard(startMillis, Modifier.padding(top = 18.dp)) { open(Routes.COUNTER) }

        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeartbeatCard(
                last = recent.firstOrNull { it.to == me.uid && it.kind == SignalKind.Heartbeat },
                partnerName = partnerName,
                modifier = Modifier.weight(1f),
                onClick = { e -> open(if (e == null) Routes.HEARTBEAT else "${Routes.HEARTBEAT}:${e.key}") },
            )
            NudgeCard(me, partner, Modifier.weight(1f), compact = true)
        }

        MoodCard(me, partnerName, moods[me.uid], partner?.uid?.let { moods[it] }, Modifier.padding(top = 14.dp)) { open(Routes.MOOD) }

        Text(
            stringResource(R.string.home_corner),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Brown,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
        )
        // Route = null means "not built yet": shows the "Yakında" pill and a toast.
        val corner = listOf(
            CornerItem(Icons.Rounded.QuestionAnswer, R.string.feature_question, PeachTint, Routes.QUESTION),
            CornerItem(Icons.Rounded.Brush, R.string.feature_drawing, MintTint, Routes.DRAWING),
            CornerItem(Icons.Rounded.EditNote, R.string.feature_notes, PeachTint, Routes.NOTES),
            CornerItem(Icons.Rounded.Event, R.string.feature_calendar, MintTint, PLANS_TAB),
            CornerItem(Icons.Rounded.Restaurant, R.string.feature_food, PeachTint, Routes.WHEEL),
            CornerItem(Icons.Rounded.Place, R.string.feature_distance, MintTint, Routes.DISTANCE),
            CornerItem(Icons.Rounded.HourglassTop, R.string.feature_capsule, PeachTint, Routes.CAPSULE),
            CornerItem(Icons.Rounded.Star, R.string.feature_bucket, MintTint, Routes.DREAMS),
            CornerItem(Icons.Rounded.Checkroom, R.string.feature_wardrobe, PeachTint, Routes.CAT),
        )
        corner.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                row.forEach { item ->
                    CornerTile(item.icon, stringResource(item.title), item.tint, Modifier.weight(1f), ready = item.route != null) {
                        when (item.route) {
                            null -> soon()
                            PLANS_TAB -> openPlans()
                            else -> item.route?.let(open)
                        }
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
        MoodFace(entry?.kind, Modifier.size(34.dp))
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

/** Corner tile target that switches to the Planlar tab instead of opening an inner screen. */
private const val PLANS_TAB = "tab:plans"

private class CornerItem(val icon: ImageVector, @androidx.annotation.StringRes val title: Int, val tint: Color, val route: String? = null)

@Composable
private fun CornerTile(icon: ImageVector, title: String, tint: Color, modifier: Modifier, ready: Boolean, onClick: () -> Unit) {
    SoftCard(modifier, radius = 20.dp, padding = 0.dp, onClick = onClick) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconCircle(icon, tint, size = 48.dp, iconSize = 24.dp)
            Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brown, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!ready) Pill(stringResource(R.string.home_soon), background = Color(0xFFF6EEE8), color = BrownSoft, fontSize = 11)
        }
    }
}
