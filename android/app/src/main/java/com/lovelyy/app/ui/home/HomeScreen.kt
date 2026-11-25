package com.lovelyy.app.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush as GradientBrush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lovelyy.app.R
import com.lovelyy.app.data.Couple
import com.lovelyy.app.data.UserProfile
import com.lovelyy.app.ui.components.InitialAvatar
import com.lovelyy.app.ui.components.PrimaryButton
import com.lovelyy.app.ui.lovelyyApp
import com.lovelyy.app.ui.theme.LovelyyTheme
import kotlinx.coroutines.launch

private data class Feature(val title: Int, val icon: ImageVector)

private val upcoming = listOf(
    Feature(R.string.feature_days, Icons.Filled.CalendarMonth),
    Feature(R.string.feature_heartbeat, Icons.Filled.Favorite),
    Feature(R.string.feature_drawing, Icons.Filled.Brush),
    Feature(R.string.feature_question, Icons.Filled.QuestionAnswer),
    Feature(R.string.feature_mood, Icons.Filled.Mood),
    Feature(R.string.feature_piggy, Icons.Filled.Savings),
    Feature(R.string.feature_distance, Icons.Filled.Place),
    Feature(R.string.feature_notes, Icons.Filled.EditNote),
)

@Composable
fun HomeScreen(me: UserProfile, couple: Couple?, partner: UserProfile?) {
    val app = lovelyyApp()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    HomeContent(
        me = me,
        partner = partner,
        onSignOut = { scope.launch { app.authRepository.signOut(context) } },
    )
}

@Composable
private fun HomeContent(me: UserProfile, partner: UserProfile?, onSignOut: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val soonText = stringResource(R.string.home_main_action_soon)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { CoupleHeader(me, partner, onSignOut) },
        // The one main action of Home lives at the bottom, under the thumb.
        bottomBar = {
            Box(
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                PrimaryButton(
                    text = stringResource(R.string.home_main_action),
                    onClick = { scope.launch { snackbar.showSnackbar(soonText) } },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CatHero()
            Text(
                stringResource(R.string.home_soon),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
            upcoming.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { FeatureCard(it, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CoupleHeader(me: UserProfile, partner: UserProfile?, onSignOut: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val partnerName = partner?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_partner_fallback)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Two overlapping avatars = "us".
        Box {
            InitialAvatar(
                me.displayName, MaterialTheme.colorScheme.primary,
                Modifier.border(3.dp, MaterialTheme.colorScheme.background, CircleShape),
            )
            InitialAvatar(
                partnerName, MaterialTheme.colorScheme.secondary,
                Modifier
                    .offset(x = 32.dp)
                    .border(3.dp, MaterialTheme.colorScheme.background, CircleShape),
            )
        }
        Spacer(Modifier.width(44.dp))
        Text(
            stringResource(R.string.home_us, me.displayName, partnerName),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = null) }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sign_out)) },
                    onClick = { menuOpen = false; onSignOut() },
                )
            }
        }
    }
}

/** The heart of Home. The real animated cat (Lottie/Rive) replaces this in step 2. */
@Composable
private fun CatHero() {
    val breathe by rememberInfiniteTransition(label = "cat").animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                GradientBrush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.primaryContainer)
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(140.dp)
                .scale(breathe)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Pets, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.home_cat_soon), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.home_cat_soon_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FeatureCard(feature: Feature, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(feature.icon, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(22.dp))
        }
        Text(stringResource(feature.title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.home_soon),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFF8F2, heightDp = 900)
@Composable
private fun HomePreview() {
    LovelyyTheme {
        HomeContent(
            me = UserProfile(uid = "a", displayName = "Ulaş"),
            partner = UserProfile(uid = "b", displayName = "Deniz"),
            onSignOut = {},
        )
    }
}
