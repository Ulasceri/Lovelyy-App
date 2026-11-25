package com.lovelyy.app.ui.cat

import android.content.Context
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.lovelyy.app.data.CatMood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Plays the cat's clips back to back without a single visible jump.
 *
 * Two video layers (each an ExoPlayer on its own TextureView) take turns:
 *  - the FRONT layer plays the current clip;
 *  - the BACK layer already holds the next clip, decoded and paused on its first frame.
 * When the front clip ends, the back layer starts at once and comes to the top. Every clip
 * ends on the same frame the next one starts on (see [Pose]), so that is a seamless cut.
 * Pose changes (bowl appears) and interruptions (a touch) use a short cross-fade where the
 * new layer fades in ON TOP of the old one, so nothing behind them ever shows through.
 */
@Stable
@OptIn(UnstableApi::class)
class CatAnimator(private val context: Context, private val scope: CoroutineScope) {

    private inner class Layer(val id: Int) {
        val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = false
        }
        var clip: CatClip? = null
        var ready = false
        var alpha by mutableFloatStateOf(if (id == 0) 1f else 0f)
        var z by mutableFloatStateOf(if (id == 0) 1f else 0f)

        init {
            player.addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    ready = true
                    onLayerReady(this@Layer)
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED && this@Layer === front) onFrontEnded()
                }
            })
        }

        fun load(c: CatClip) {
            clip = c
            ready = false
            player.playWhenReady = false
            player.setMediaItem(MediaItem.fromUri(c.uri))
            player.prepare()
        }
    }

    private val layers = listOf(Layer(0), Layer(1))
    private var frontId by mutableIntStateOf(0)
    private val front get() = layers[frontId]
    private val back get() = layers[1 - frontId]

    /** True once the first clip is on screen (the poster image can stay behind it). */
    var started by mutableStateOf(false)
        private set

    /** The clip on screen, for the UI (e.g. hearts while petted). */
    var currentClip by mutableStateOf<CatClip?>(null)
        private set

    // ---------- what to play ----------
    private var mood: CatMood = CatMood.Idle
    private var moodLoops = 0
    private val queue = ArrayDeque<CatClip>()
    private var petting = false

    /** Waiting for the back layer to be ready, then switch. */
    private var pendingSwitch: Switch? = null
    private var fadeJob: Job? = null
    private var fading = false
    private var paused = false

    private enum class Switch { Cut, Fade }

    fun bind(layer: Int, view: TextureView) = layers[layer].player.setVideoTextureView(view)

    fun alpha(layer: Int) = layers[layer].alpha
    fun z(layer: Int) = layers[layer].z

    /** The pose the cat is in (or heading to) right now. */
    private val currentPose: Pose? get() = front.clip?.end

    /** Start playing. Call once the views are bound. */
    fun start(initialMood: CatMood) {
        if (front.clip != null) return
        mood = initialMood
        front.load(moodClip(mood, 0))
        front.player.playWhenReady = !paused
    }

    /** The needs changed. The cat finishes its current clip first, then shows the new mood. */
    fun setMood(newMood: CatMood) {
        if (newMood == mood) return
        mood = newMood
        moodLoops = 0
        // Falling asleep / waking up should start right away, not after a full idle loop.
        val f = front.clip
        if (f != null && !f.isBridge && (newMood == CatMood.Sleeping) != (f.end == Pose.Sleep)) {
            interrupt(bridgeTo(f.end, moodClip(newMood, 0)))
        } else {
            prepareNext()
        }
    }

    /**
     * Something happened (a tap, feeding...). Shows [clip] right away with a quick
     * cross-fade, then goes back to the mood loop.
     */
    fun play(clip: CatClip) {
        if (front.clip?.isBridge == true) {
            queue.addLast(clip); prepareNext(); return
        }
        val first = bridgeTo(currentPose, clip)
        if (first != clip) queue.addFirst(clip)
        interrupt(first)
    }

    /** While a finger strokes the cat, the "petted" clip keeps playing. */
    fun setPetting(on: Boolean) {
        if (on == petting) return
        petting = on
        if (on && currentPose != Pose.Sleep && front.clip != CatClip.Petted) play(CatClip.Petted) else prepareNext()
    }

    fun pause() {
        paused = true
        front.player.playWhenReady = false
    }

    fun resume() {
        paused = false
        if (pendingSwitch == null) front.player.playWhenReady = true
    }

    fun release() {
        fadeJob?.cancel()
        layers.forEach { it.player.release() }
    }

    // ---------- internals ----------

    /** What comes after the front clip, given the queue and the mood. */
    private fun nextClip(): CatClip {
        val after = front.clip?.end
        val target = when {
            queue.isNotEmpty() -> queue.first()
            petting && after != Pose.Sleep -> CatClip.Petted
            else -> moodClip(mood, moodLoops)
        }
        return bridgeTo(after, target)
    }

    /** Keep the back layer loaded with the right next clip. */
    private fun prepareNext() {
        // While fading, the back layer is still visible underneath: leave it alone.
        // The fade calls prepareNext() itself when it's done.
        if (fading) return
        if (pendingSwitch != null && back.clip != null) {
            // An interruption is loading; don't replace it.
            return
        }
        val next = nextClip()
        if (back.clip != next) back.load(next)
    }

    private fun interrupt(clip: CatClip) {
        finishFade()
        // A preloaded back layer sits paused on frame 0: reuse it when it holds this clip.
        if (back.clip != clip) back.load(clip)
        pendingSwitch = Switch.Fade
        if (back.ready) doSwitch()
    }

    private fun onFrontEnded() {
        if (pendingSwitch != null) return // an interruption is already on its way
        prepareNext()
        val f = front.clip
        val b = back.clip
        pendingSwitch = if (f != null && b != null && f.end == b.start) Switch.Cut else Switch.Fade
        if (back.ready) doSwitch()
    }

    private fun onLayerReady(layer: Layer) {
        if (layer === front && !started) {
            started = true
            currentClip = front.clip
            onClipStarted(front.clip)
            prepareNext()
        }
        if (layer === back && pendingSwitch != null) doSwitch()
    }

    private fun doSwitch() {
        val mode = pendingSwitch ?: return
        pendingSwitch = null
        finishFade()
        val old = front
        val new = back
        // New layer goes on top and starts playing.
        new.z = 1f
        old.z = 0f
        new.player.playWhenReady = !paused
        frontId = new.id
        currentClip = new.clip
        onClipStarted(new.clip)

        old.alpha = 1f
        if (mode == Switch.Cut) {
            new.alpha = 1f
            retire(old)
            return
        }
        new.alpha = 0f
        fading = true
        fadeJob = scope.launch {
            animate(0f, 1f, animationSpec = tween(FADE_MS, easing = FastOutSlowInEasing)) { v, _ -> new.alpha = v }
            fading = false
            retire(old)
        }
    }

    /** The old front layer is now fully covered: stop it and preload what comes next. */
    private fun retire(old: Layer) {
        old.player.playWhenReady = false
        old.alpha = 0f
        old.clip = null
        prepareNext()
    }

    /** Jumps a running cross-fade to its end, so the layers are in a clean state. */
    private fun finishFade() {
        if (!fading) return
        fadeJob?.cancel()
        fading = false
        front.alpha = 1f
        back.player.playWhenReady = false
        back.alpha = 0f
    }

    private fun onClipStarted(clip: CatClip?) {
        clip ?: return
        if (queue.firstOrNull() == clip) queue.removeFirst()
        if (clip == moodClip(mood, moodLoops)) moodLoops++
    }

    companion object {
        const val FADE_MS = 280
    }
}
