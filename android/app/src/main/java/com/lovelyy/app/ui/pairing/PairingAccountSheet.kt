package com.lovelyy.app.ui.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.R
import com.lovelyy.app.ui.components.GhostButton
import com.lovelyy.app.ui.components.RoundSvgButton
import com.lovelyy.app.ui.components.SvgStrokeIcon
import com.lovelyy.app.ui.components.softShadow
import com.lovelyy.app.ui.history.Danger
import com.lovelyy.app.ui.history.SubText
import com.lovelyy.app.ui.profile.ActionRow
import com.lovelyy.app.ui.profile.LOGOUT_D
import com.lovelyy.app.ui.profile.MAIL_D
import com.lovelyy.app.ui.profile.TRASH_D
import com.lovelyy.app.ui.profile.USER_D
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.PeachSoft
import com.lovelyy.app.ui.theme.Quicksand
import com.lovelyy.app.ui.theme.Sand
import kotlinx.coroutines.launch

/** Drag handle and dashed lines on the pairing boards. */
private val HandleColor = Color(0xFFE3D5CB)

/**
 * Top-right account button on "Kodunu paylaş" (approved board 34). While the account is being deleted it shows a
 * spinner and can't be tapped.
 */
@Composable
internal fun PairingAccountButton(busy: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.pf_account)
    if (!busy) {
        RoundSvgButton(USER_D, label, onClick)
        return
    }
    Box(
        Modifier.size(44.dp).softShadow(CircleShape, 4.dp).clip(CircleShape).background(CreamCard)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Brown, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
    }
}

/**
 * "Hesabın" sheet before pairing (approved board 35): which e-mail I'm signed in with (never a name), sign out,
 * and delete the account. Google Play requires account deletion to be reachable in the app, also for someone who
 * never pairs. Deleting itself goes through the existing confirm dialog and [com.lovelyy.app.ui.profile.LeaveViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PairingAccountSheet(
    email: String,
    google: Boolean,
    busy: Boolean,
    onSignOut: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // Slide the sheet down first, then act (the caller removes it from composition).
    val hideThen: (() -> Unit) -> Unit = { action -> scope.launch { sheet.hide() }.invokeOnCompletion { action() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = Cream,
        dragHandle = {
            Box(Modifier.padding(top = 12.dp).size(width = 44.dp, height = 5.dp).clip(RoundedCornerShape(3.dp)).background(HandleColor))
        },
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.pair_acc_title), fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brown,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )

            // Who is signed in: e-mail and method only (no names in the pairing UI).
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(PeachSoft).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SvgStrokeIcon(MAIL_D, Brown, Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        email.ifBlank { "–" }, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Brown,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(if (google) R.string.pair_acc_google else R.string.pair_acc_email),
                        fontFamily = Quicksand, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 17.sp, color = SubText,
                    )
                }
            }

            val shape = RoundedCornerShape(24.dp)
            Column(
                Modifier.fillMaxWidth().softShadow(shape, 6.dp).clip(shape).background(CreamCard).padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                ActionRow(LOGOUT_D, stringResource(R.string.sign_out), stringResource(R.string.pair_acc_sign_out_sub), enabled = !busy, onClick = { hideThen(onSignOut) })
                Box(Modifier.fillMaxWidth().height(1.dp).background(Sand))
                ActionRow(TRASH_D, stringResource(R.string.acc_delete), stringResource(R.string.pair_acc_delete_sub), color = Danger, enabled = !busy, onClick = { hideThen(onDelete) })
            }

            GhostButton(stringResource(R.string.action_close), { hideThen(onDismiss) }, Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
