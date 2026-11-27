package com.lovelyy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.lovelyy.app.R
import com.lovelyy.app.data.ListLimits
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.CreamCard
import com.lovelyy.app.ui.theme.Quicksand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import timber.log.Timber

/** A growing window over a newest-first Firestore list: [items] (null while the first page loads). */
class Paged<T>(val items: List<T>?, val canLoadMore: Boolean, val loadMore: () -> Unit)

/**
 * Loads a list one page at a time instead of everything at once (each listed document is a billed read).
 * "Load more" grows the window by [page]; the list shown so far stays on screen while the bigger window loads.
 * Listens only while the screen is at least STARTED, like collectAsStateWithLifecycle.
 */
@Composable
fun <T> rememberPaged(key: Any, page: Int, source: (limit: Int) -> Flow<List<T>>): Paged<T> {
    var limit by rememberSaveable(key) { mutableIntStateOf(page) }
    var items by remember(key) { mutableStateOf<List<T>?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(key, limit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            source(limit)
                .catch { Timber.w(it, "Paged list unavailable"); emit(items ?: emptyList()) }
                .collect { items = it }
        }
    }
    val loaded = items
    return Paged(loaded, loaded != null && ListLimits.hasMore(loaded.size, limit)) { limit = ListLimits.nextLimit(limit, page) }
}

/** "Daha eskilerini göster": the same soft pill as Notlar's load-more button. */
@Composable
fun LoadMoreButton(onClick: () -> Unit, modifier: Modifier = Modifier, text: String = stringResource(R.string.list_load_more)) {
    Text(
        text,
        fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brown,
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(CreamCard)
            .semantics { role = Role.Button }
            .bounceClick(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    )
}
