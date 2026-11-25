package com.lovelyy.app.ui.pairing

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelyy.app.R
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.components.BeatingHeart
import com.lovelyy.app.ui.components.ErrorText
import com.lovelyy.app.ui.components.LovelyyTextField
import com.lovelyy.app.ui.components.PrimaryButton
import com.lovelyy.app.ui.components.SecondaryButton
import com.lovelyy.app.ui.lovelyyApp
import kotlinx.coroutines.launch

@Composable
fun PairingScreen(me: UserProfile) {
    val app = lovelyyApp()
    val vm: PairingViewModel = viewModel { PairingViewModel(app.pairingRepository) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { scope.launch { app.authRepository.signOut(context) } }) {
                Text(stringResource(R.string.sign_out), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        BeatingHeart(size = 56.dp)
        Text(stringResource(R.string.pair_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.pair_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PillTabs(selected = vm.tab, onSelect = vm::selectTab)

        AnimatedContent(targetState = vm.tab, label = "pairTab") { tab ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (tab) {
                    PairingViewModel.Tab.Share -> {
                        LaunchedEffect(me.pairCode) { vm.ensureCode(me) }
                        ShareCode(
                            code = me.pairCode,
                            loading = vm.loading,
                            onShare = { code -> shareCode(context, code) },
                            onNewCode = { vm.newCode(me) },
                        )
                    }
                    PairingViewModel.Tab.Enter -> {
                        LovelyyTextField(
                            value = vm.input,
                            onValueChange = vm::onInput,
                            label = stringResource(R.string.pair_enter_hint),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done,
                            ),
                        )
                        ErrorText(vm.error)
                        PrimaryButton(
                            text = stringResource(R.string.pair_join),
                            onClick = { vm.join(me) },
                            enabled = vm.input.length == 6,
                            loading = vm.loading,
                        )
                    }
                }
            }
        }
        if (vm.tab == PairingViewModel.Tab.Share) ErrorText(vm.error)
    }
}

@Composable
private fun ShareCode(code: String?, loading: Boolean, onShare: (String) -> Unit, onNewCode: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.pair_your_code), style = MaterialTheme.typography.titleSmall)
        Text(
            text = code?.chunked(3)?.joinToString(" ") ?: "··· ···",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            stringResource(R.string.pair_code_valid),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (code != null) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BeatingHeart(size = 16.dp, color = MaterialTheme.colorScheme.tertiary)
                Text(stringResource(R.string.pair_waiting), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    PrimaryButton(
        text = stringResource(if (code == null) R.string.pair_create_code else R.string.pair_share),
        onClick = { if (code == null) onNewCode() else onShare(code) },
        loading = loading,
    )
    if (code != null) {
        SecondaryButton(text = stringResource(R.string.pair_new_code), onClick = onNewCode, enabled = !loading)
    }
}

@Composable
private fun PillTabs(selected: PairingViewModel.Tab, onSelect: (PairingViewModel.Tab) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(4.dp),
    ) {
        PairingViewModel.Tab.entries.forEach { tab ->
            val isSelected = tab == selected
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.secondaryContainer,
                label = "tabBg",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(bg)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(tab)
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(if (tab == PairingViewModel.Tab.Share) R.string.pair_tab_share else R.string.pair_tab_enter),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun shareCode(context: Context, code: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.pair_share_text, code))
    }
    context.startActivity(Intent.createChooser(send, null))
}
