package com.lovelyy.app.ui.pairing

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.R
import com.lovelyy.app.data.PairingRepository
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.components.BackButton
import com.lovelyy.app.ui.components.ErrorText
import com.lovelyy.app.ui.components.GhostButton
import com.lovelyy.app.ui.components.PeachButton
import com.lovelyy.app.ui.components.Pill
import com.lovelyy.app.ui.components.bounceClick
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.Mint
import com.lovelyy.app.ui.theme.MintInk
import com.lovelyy.app.ui.theme.MintTint
import com.lovelyy.app.ui.theme.Outline
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.PeachTint
import com.lovelyy.app.ui.theme.Quicksand
import com.lovelyy.app.ui.theme.Sand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PairingScreen(me: UserProfile) {
    val app = lovelyyApp()
    val vm: PairingViewModel = viewModel { PairingViewModel(app.pairingRepository) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler(enabled = vm.tab == PairingViewModel.Tab.Enter) { vm.selectTab(PairingViewModel.Tab.Share) }

    AnimatedContent(
        targetState = vm.tab,
        transitionSpec = {
            if (targetState == PairingViewModel.Tab.Enter) (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
            else (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
        },
        label = "pairTab",
    ) { tab ->
        when (tab) {
            PairingViewModel.Tab.Share -> {
                LaunchedEffect(me.pairCode) { vm.ensureCode(me) }
                SharePage(
                    me = me,
                    code = me.pairCode,
                    expiresAt = vm.expiresAt,
                    loading = vm.loading,
                    error = { ErrorText(vm.error, Modifier.fillMaxWidth()) },
                    onBack = { scope.launch { app.authRepository.signOut(context) } },
                    onExpired = { vm.newCode(me) },
                    onShare = { code -> shareCode(context, code) },
                    onNewCode = { vm.newCode(me) },
                    onEnter = { vm.selectTab(PairingViewModel.Tab.Enter) },
                )
            }
            PairingViewModel.Tab.Enter -> EnterPage(
                input = vm.input,
                loading = vm.loading,
                error = { ErrorText(vm.error, Modifier.fillMaxWidth().padding(top = 10.dp)) },
                onBack = { vm.selectTab(PairingViewModel.Tab.Share) },
                onDigit = vm::press,
                onBackspace = vm::backspace,
                onJoin = { vm.join(me) },
            )
        }
    }
}

// ---------- Kodunu paylaş ----------

private val ShareIcon: @Composable RowScope.() -> Unit = {
    Icon(Icons.Rounded.Share, null, tint = Brown, modifier = Modifier.size(20.dp))
}

@Composable
private fun SharePage(
    me: UserProfile,
    code: String?,
    expiresAt: Long?,
    loading: Boolean,
    error: @Composable () -> Unit,
    onBack: () -> Unit,
    onExpired: () -> Unit,
    onShare: (String) -> Unit,
    onNewCode: () -> Unit,
    onEnter: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Pill(stringResource(R.string.pair_last_step), color = BrownSoft)
                }
                Spacer(Modifier.width(44.dp))
            }

            CoupleAvatars(me.displayName, Modifier.padding(top = 28.dp))

            Text(
                stringResource(R.string.pair_title),
                fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Brown,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                stringResource(R.string.pair_subtitle),
                fontFamily = Quicksand, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp,
                color = BrownSoft, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp).widthIn(max = 300.dp),
            )

            CodeCard(code, expiresAt, onExpired, Modifier.padding(top = 28.dp))

            Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                PulseDot()
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.pair_waiting), fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrownSoft)
            }
            error()

            Spacer(Modifier.weight(1f).heightIn(min = 24.dp))

            PeachButton(
                stringResource(if (code == null) R.string.pair_create_code else R.string.pair_share),
                onClick = { if (code == null) onNewCode() else onShare(code) },
                loading = loading,
                leading = if (code != null) ShareIcon else null,
            )
            Spacer(Modifier.height(10.dp))
            GhostButton(stringResource(R.string.pair_have_code), onEnter, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CoupleAvatars(myName: String, modifier: Modifier = Modifier) {
    val beat by rememberInfiniteTransition(label = "beat").animateFloat(
        1f, 1f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 1600
                1f at 0
                1.18f at 240
                1f at 480
                1.12f at 720
                1f at 960
            },
        ),
        label = "s",
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(84.dp)
                .softShadow(CircleShape, 10.dp)
                .clip(CircleShape)
                .background(Peach)
                .border(4.dp, CreamCard, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(myName.trim().firstOrNull()?.uppercase() ?: "?", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = Brown)
        }
        Box(
            Modifier
                .width(64.dp)
                .height(36.dp)
                .drawBehind {
                    drawLine(
                        Color(0xFFE3D5CB), Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                        strokeWidth = 2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(36.dp).scale(beat).softShadow(CircleShape, 4.dp).clip(CircleShape).background(CreamCard),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Favorite, null, tint = Peach, modifier = Modifier.size(18.dp)) }
        }
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Sand)
                .drawBehind {
                    val w = 2.5.dp.toPx()
                    drawCircle(
                        Color(0xFFD9C9BE), radius = size.minDimension / 2 - w / 2,
                        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f))),
                    )
                },
            contentAlignment = Alignment.Center,
        ) { Text("?", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Color(0xFF9A8A83)) }
    }
}

@Composable
private fun CodeCard(code: String?, expiresAt: Long?, onExpired: () -> Unit, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1600); copied = false } }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(expiresAt) {
        while (expiresAt != null) {
            now = System.currentTimeMillis()
            if (now >= expiresAt) { onExpired(); break }
            delay(1000)
        }
    }
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .softShadow(shape, 14.dp)
            .clip(shape)
            .background(CreamCard)
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.pair_your_code), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.4.sp, color = BrownSoft)
        val described = stringResource(R.string.pair_code_a11y, code?.chunked(3)?.joinToString(" ") ?: "")
        Row(
            Modifier.padding(top = 14.dp).semantics { contentDescription = described },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (i in 0 until PairingRepository.CODE_LENGTH) {
                if (i == 3) Spacer(Modifier.width(2.dp))
                Box(
                    Modifier.size(width = 42.dp, height = 56.dp).clip(RoundedCornerShape(14.dp)).background(PeachTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(code?.getOrNull(i)?.toString() ?: "·", fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Brown)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Schedule, null, tint = BrownSoft, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            val left = expiresAt?.let { ((it - now) / 1000).coerceAtLeast(0) }
            Text(
                if (left == null) stringResource(R.string.pair_code_valid)
                else stringResource(R.string.pair_code_renews, "%02d:%02d".format(left / 60, left % 60)),
                fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft,
                modifier = Modifier.weight(1f),
            )
            Row(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MintTint)
                    .bounceClick(enabled = code != null) {
                        clipboard.setText(AnnotatedString(code.orEmpty()))
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        copied = true
                    }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (copied) Icons.Rounded.Done else Icons.Rounded.ContentCopy, null, tint = MintInk, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(if (copied) R.string.pair_copied else R.string.pair_copy), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MintInk)
            }
        }
    }
}

@Composable
private fun PulseDot() {
    val t by rememberInfiniteTransition(label = "pulse").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "t",
    )
    Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(10.dp + 8.dp * t)
                .clip(CircleShape)
                .background(Mint.copy(alpha = 0.9f * (1f - t)))
        )
        Box(Modifier.size(10.dp).clip(CircleShape).background(Mint))
    }
}

// ---------- Kodu gir ----------

@Composable
private fun EnterPage(
    input: String,
    loading: Boolean,
    error: @Composable () -> Unit,
    onBack: () -> Unit,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onJoin: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 20.dp),
    ) {
        BackButton(onBack)
        Text(
            stringResource(R.string.pair_enter_title),
            fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Brown,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            stringResource(R.string.pair_enter_body),
            fontFamily = Quicksand, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = BrownSoft,
            modifier = Modifier.padding(top = 10.dp),
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            for (i in 0 until PairingRepository.CODE_LENGTH) {
                if (i == 3) Spacer(Modifier.width(4.dp))
                CodeBox(input.getOrNull(i), active = i == input.length)
            }
        }
        Text(
            if (input.isNotEmpty() && input.length < PairingRepository.CODE_LENGTH)
                pluralStringResource(R.plurals.pair_enter_hint_short, (PairingRepository.CODE_LENGTH - input.length).toInt(), PairingRepository.CODE_LENGTH - input.length)
            else stringResource(R.string.pair_enter_hint_ttl),
            fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrownSoft,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        )
        error()

        Spacer(Modifier.weight(1f))

        NumberPad(onDigit, onBackspace)

        Spacer(Modifier.height(16.dp))
        PeachButton(
            stringResource(R.string.pair_join), onJoin,
            enabled = input.length == PairingRepository.CODE_LENGTH,
            loading = loading,
            leading = { Icon(Icons.Rounded.Favorite, null, tint = Brown, modifier = Modifier.size(20.dp)) },
        )
    }
}

@Composable
private fun CodeBox(ch: Char?, active: Boolean) {
    val border by animateColorAsState(
        when {
            active -> Peach
            ch != null -> Color(0xFFF3D9C8)
            else -> Outline
        },
        label = "border",
    )
    val pop by androidx.compose.animation.core.animateFloatAsState(if (ch != null) 1f else 0.6f, label = "pop")
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .size(width = 46.dp, height = 60.dp)
            .then(if (active) Modifier.border(6.dp, Peach.copy(alpha = 0.25f), RoundedCornerShape(19.dp)).padding(3.dp) else Modifier.padding(3.dp))
            .clip(shape)
            .background(if (ch != null) PeachTint else CreamCard)
            .border(if (active) 2.5.dp else 2.dp, border, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (ch != null) Text(ch.toString(), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Brown, modifier = Modifier.scale(pop))
    }
}

@Composable
private fun NumberPad(onDigit: (Char) -> Unit, onBackspace: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val rows = listOf("123", "456", "789")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { d -> Key(Modifier.weight(1f), filled = true, label = d.toString(), onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onDigit(d) }) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(Modifier.weight(1f))
            Key(Modifier.weight(1f), filled = true, label = "0", onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onDigit('0') })
            Key(Modifier.weight(1f), filled = false, label = null, onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onBackspace() })
        }
    }
}

@Composable
private fun Key(modifier: Modifier, filled: Boolean, label: String?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .height(60.dp)
            .then(if (filled) Modifier.softShadow(shape, 3.dp) else Modifier)
            .clip(shape)
            .background(if (filled) CreamCard else Cream)
            .bounceClick(pressedScale = 0.92f, haptic = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) Text(label, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Brown)
        else Icon(Icons.AutoMirrored.Rounded.Backspace, stringResource(R.string.pair_delete), tint = Brown, modifier = Modifier.size(24.dp))
    }
}

private fun shareCode(context: Context, code: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.pair_share_text, code))
    }
    context.startActivity(Intent.createChooser(send, null))
}
