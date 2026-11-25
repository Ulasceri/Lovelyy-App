package com.lovelyy.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.lovelyy.app.R
import com.lovelyy.app.data.AppError
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.data.UserRepository
import com.lovelyy.app.ui.components.BeatingHeart
import com.lovelyy.app.ui.components.ErrorText
import com.lovelyy.app.ui.components.LovelyyTextField
import com.lovelyy.app.ui.components.PrimaryButton
import com.lovelyy.app.ui.lovelyyApp
import kotlinx.coroutines.launch

/** Shown once, when the account has no name yet (e.g. some email sign-ups). */
@Composable
fun NameScreen(me: UserProfile) {
    val users = lovelyyApp().userRepository
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<AppError?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.weight(1f))
        BeatingHeart(size = 56.dp)
        Text(stringResource(R.string.name_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.name_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        LovelyyTextField(
            value = name,
            onValueChange = { name = it.take(UserRepository.MAX_NAME) },
            label = stringResource(R.string.field_name),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        )
        ErrorText(error)
        PrimaryButton(
            text = stringResource(R.string.action_continue),
            enabled = name.isNotBlank(),
            loading = loading,
            onClick = {
                loading = true
                error = null
                scope.launch {
                    runCatching { users.updateName(me.uid, name) }
                        .onFailure { error = AppError.Unknown }
                    loading = false
                }
            },
        )
    }
}
