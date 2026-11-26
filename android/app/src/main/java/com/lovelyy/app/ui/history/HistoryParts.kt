package com.lovelyy.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.R
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.Quicksand
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Shared pieces of the history, search and inbox boards (20-33). */
internal val Muted = Color(0xFF75655E)
internal val SubText = Color(0xFF5F524C)
internal val InChip = Color(0xFFD6F0E8)
internal val OutChip = Color(0xFFFFE6D6)
internal val Danger = Color(0xFFA8401C)
internal val DangerTint = Color(0xFFFBE3D9)
internal val PeachBubble = Color(0xFFFFF1E8)
internal val MintBubble = Color(0xFFE6F5F0)
internal val Highlight = Color(0xFFFFE2A8)
internal val TR = Locale.forLanguageTag("tr")
private val DAY_TITLE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM EEEE", TR)
internal val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", TR)
internal val DAY_MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", TR)
internal val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", TR)

internal fun Long.localDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
internal fun Long.clock(): String = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(TIME)

/** "Bugün", "Dün" or "23 Kasım Pazartesi". */
@Composable
internal fun dayTitle(day: LocalDate, today: LocalDate = LocalDate.now()): String = when (day) {
    today -> stringResource(R.string.hi_today)
    today.minusDays(1) -> stringResource(R.string.hi_yesterday)
    else -> day.format(DAY_TITLE)
}

@Composable
internal fun DayHeader(text: String, modifier: Modifier = Modifier) {
    Text(text, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Muted, modifier = modifier.padding(start = 4.dp, top = 4.dp))
}

/** Dark-selected filter chips (Tümü / Gönderdiklerim / Gelenler...). Scrolls sideways when long. */
@Composable
internal fun FilterChips(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(if (sel) Brown else Color.White)
                    .semantics { role = Role.Tab; this.selected = sel }
                    .bounceClick { onSelect(i) }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (sel) Cream else Brown, maxLines = 1, softWrap = false)
            }
        }
    }
}

/** White rounded row with a leading icon circle, two lines of text and a trailing slot. */
@Composable
internal fun HistoryRow(
    icon: @Composable () -> Unit,
    iconBg: Color,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    extra: @Composable (ColumnScope.() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier.fillMaxWidth().softShadow(shape, 6.dp).clip(shape).background(Color.White)
            .then(if (onClick != null) Modifier.bounceClick(pressedScale = 0.98f, onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) { icon() }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            extra?.invoke(this)
        }
        trailing?.invoke()
    }
}

/** "Gelen" / "Giden" chip with an arrow. */
@Composable
internal fun DirectionChip(incoming: Boolean) {
    Row(
        Modifier.clip(CircleShape).background(if (incoming) InChip else OutChip).padding(start = 7.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        com.lovelyy.app.ui.components.SvgStrokeIcon(if (incoming) "M17 7L7 17M7 9v8h8" else "M7 17L17 7M9 7h8v8", Brown, Modifier.size(14.dp), strokeWidth = 2.4f)
        Text(stringResource(if (incoming) R.string.hi_in else R.string.hi_out), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Brown)
    }
}

/** Small initial circle (U / E) used next to names. */
@Composable
internal fun MiniAvatar(name: String, color: Color, size: Int = 18, modifier: Modifier = Modifier) {
    Box(modifier.size(size.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(name.trim().firstOrNull()?.uppercase() ?: "?", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = (size * 0.55f).sp, color = Brown)
    }
}

/** Friendly empty state for lists that have nothing yet. */
@Composable
internal fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Text(
        text, fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = SubText,
        modifier = modifier.fillMaxWidth().clip(shape).background(Color.White).padding(18.dp),
    )
}

/** Standard scrolling column of the inner screens (top bar + content above the bottom nav). */
@Composable
internal fun InnerColumn(spacing: Int = 8, scroll: androidx.compose.foundation.ScrollState = rememberScrollState(), content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(scroll).padding(horizontal = 20.dp).navigationBarsPadding().padding(top = 20.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
        content = content,
    )
}
