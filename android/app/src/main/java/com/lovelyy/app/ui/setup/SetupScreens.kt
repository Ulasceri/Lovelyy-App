package com.lovelyy.app.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lovelyy.app.R
import com.lovelyy.app.ui.components.BeatingHeart
import com.lovelyy.app.ui.theme.LovelyyTheme

/** Shown when app/google-services.json is missing, instead of crashing. */
@Composable
fun FirebaseSetupScreen() = MessageScreen(
    title = stringResource(R.string.setup_title),
    body = stringResource(R.string.setup_body),
)

@Composable
fun ErrorScreen(message: String?) = MessageScreen(
    title = stringResource(R.string.error_title),
    body = listOfNotNull(stringResource(R.string.error_retry_hint), message).joinToString("\n\n"),
)

@Composable
private fun MessageScreen(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BeatingHeart(size = 64.dp)
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F2)
@Composable
private fun SetupPreview() {
    LovelyyTheme { FirebaseSetupScreen() }
}
