package com.pnd.android.loop.ui.home

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.File
import app.rive.runtime.kotlin.core.FileAsset
import app.rive.runtime.kotlin.core.FileAssetLoader
import app.rive.runtime.kotlin.core.Rive
import app.rive.runtime.kotlin.core.SMIBoolean
import app.rive.runtime.kotlin.core.SMINumber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext

private data class TreeRiveInputs(val stage: Int, val reward: Int, val wilt: Int, val active: Boolean, val effects: Int)

/**
 * A parsed Rive scene with its image slots already decoded. Importing one means decoding seven
 * full-size PNGs, far too slow for the main thread, so it happens on IO and is shared by refcount.
 * The cache owns one reference; every scene showing it owns another.
 */
private class TreeRiveFile(val file: File, private val loader: FileAssetLoader) {
    private var refs = 1

    fun acquire() = synchronized(this) { refs++ }

    fun release() = synchronized(this) {
        if (--refs == 0) {
            file.release()
            loader.release()
        }
    }
}

/**
 * Imports run one at a time. Dragging the shop's growth slider requests a scene per step; queued
 * requests whose scene has already left composition are cancelled before they decode anything.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private val treeRiveDispatcher = Dispatchers.IO.limitedParallelism(1)

/** Keeps the most recent scenes, so returning to Home or scrolling the card back in is instant. */
private object TreeRiveFiles {
    // Each scene holds roughly 40 MB of decoded art, so keep just the Home card and one preview.
    private const val MAX_FILES = 2

    private val cache = object : LruCache<String, TreeRiveFile>(MAX_FILES) {
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: TreeRiveFile, newValue: TreeRiveFile?) =
            oldValue.release()
    }

    /** Returns a cached scene with a reference the caller must release, or null. */
    fun acquire(key: String): TreeRiveFile? = synchronized(cache) { cache.get(key)?.also { it.acquire() } }

    /** Imports the scene into the cache if absent. Call off the main thread. */
    fun load(key: String, importer: () -> TreeRiveFile) {
        if (synchronized(cache) { cache.get(key) } != null) return
        val imported = importer()
        synchronized(cache) { if (cache.get(key) == null) cache.put(key, imported) else imported.release() }
    }
}

private fun importTreeRive(pack: TreeVisualPack, stage: Int, selectedTree: String): TreeRiveFile {
    val scene = requireNotNull(pack.riveBytesOrNull(stage)) { "Missing Rive scene" }
    val slots = pack.definition.rive.imageSlots.associateWith { if (it == "tree") selectedTree else it }
    val images = slots.mapValues { (_, image) -> pack.openImage(image).use { it.readBytes() } }
    val loadedSlots = mutableSetOf<String>()
    val loader = object : FileAssetLoader() {
        override fun loadContents(asset: FileAsset, inBandBytes: ByteArray): Boolean {
            val bytes = images[asset.name] ?: return false
            check(asset.decode(bytes)) { "Cannot decode Rive image slot: ${asset.name}" }
            loadedSlots.add(asset.name)
            return true
        }
    }
    val file = try {
        File(scene, Rive.defaultRendererType, loader)
    } catch (e: Throwable) {
        loader.release()
        throw e
    }
    val imported = TreeRiveFile(file, loader)
    if (!loadedSlots.containsAll(images.keys)) {
        imported.release()
        error("Missing referenced Rive image slots")
    }
    return imported
}

/** One scene's hold on a [TreeRiveFile], released when the scene leaves composition. */
private class TreeRiveLease(initial: TreeRiveFile?) : RememberObserver {
    var file by mutableStateOf(initial)
        private set
    var unavailable by mutableStateOf(false)
        private set
    private var released = false

    fun attach(loaded: TreeRiveFile?) {
        if (released) {
            loaded?.release()
        } else if (loaded == null) {
            unavailable = true
        } else {
            file = loaded
        }
    }

    override fun onRemembered() {}
    override fun onForgotten() = release()
    override fun onAbandoned() = release()

    private fun release() {
        if (released) return
        released = true
        file?.release()
    }
}

/** Both the live card and guide use this renderer and the same image pack. */
@Composable
internal fun LoopTreeScene(stage: Int, rewardLevel: Int, description: String,
    modifier: Modifier, wiltLevel: Int = 0, skin: TreeSkin? = null,
    effects: Int = TreeEffect.ALL, animationsRunning: () -> Boolean = { true }) {
    val pack = rememberTreeVisualPack(skin)
    val selectedTree = pack.treeImageKey(stage, wiltLevel)
    val owner = LocalLifecycleOwner.current
    var active by remember(owner) {
        mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var animationsEnabled by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    // Observe only the scrolling/not-scrolling boundary, inside the scene's own scope.
    val running = animationsRunning()
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ ->
            active = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            animationsEnabled = ValueAnimator.areAnimatorsEnabled()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    if (!animationsEnabled) {
        NativeTreeScene(stage, rewardLevel, description, modifier, wiltLevel, pack, running, effects)
        return
    }
    // Recreate only when the selected image changes, not on clock or reward updates.
    val cacheKey = "${pack.skin.packId}/${pack.definition.rive.pathForStage(stage)}/$selectedTree"
    key(cacheKey) {
        val context = LocalContext.current
        val lease = remember { TreeRiveLease(TreeRiveFiles.acquire(cacheKey)) }
        LaunchedEffect(lease) {
            if (lease.file != null) return@LaunchedEffect
            val loaded = try {
                TreeRiveRuntime.initialize(context)
                withContext(treeRiveDispatcher) { TreeRiveFiles.load(cacheKey) { importTreeRive(pack, stage, selectedTree) } }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("TreePack", "Cannot prepare Rive pack; using native scene", e)
                false
            } catch (e: LinkageError) {
                Log.w("TreePack", "Rive runtime unavailable; using native scene", e)
                false
            }
            // Take the reference here on the main thread, so cancellation can never leak it.
            lease.attach(if (loaded) TreeRiveFiles.acquire(cacheKey) else null)
        }
        val riveFile = lease.file
        val view = remember(riveFile, context) {
            riveFile?.let { loaded ->
                var candidate: TreePackRiveView? = null
                try {
                    candidate = TreePackRiveView(context)
                    candidate.load(loaded.file, pack.definition.rive, stage, rewardLevel, wiltLevel, active, effects)
                    candidate
                } catch (e: Exception) {
                    candidate?.releasePack()
                    Log.w("TreePack", "Invalid Rive template; using native scene", e)
                    null
                }
            }
        }
        if (riveFile == null && !lease.unavailable) {
            // Reserve the card's space. The Canvas scene differs in scale/effect positions;
            // showing it while loading causes a visible jump followed by a blank TextureView.
            Box(modifier)
        } else if (view == null) {
            NativeTreeScene(stage, rewardLevel, description, modifier, wiltLevel, pack, running, effects)
        } else {
            BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
                AndroidView(
                    factory = { view },
                    modifier = Modifier.size(minOf(maxWidth, maxHeight)),
                    onRelease = { it.releasePack() },
                    update = {
                        it.contentDescription = description
                        it.updateState(stage, rewardLevel, wiltLevel, active, running, effects)
                    },
                )
            }
        }
        // Also covers a view prepared but never attached.
        DisposableEffect(view) { onDispose { view?.releasePack() } }
    }
}

private object TreeRiveRuntime {
    private var initialized = false

    @Synchronized
    fun initialize(context: Context) {
        if (!initialized) {
            Rive.init(context.applicationContext)
            initialized = true
        }
    }
}

private class TreePackRiveView(context: Context) : RiveAnimationView(context) {
    private var machine = ""
    private var released = false
    private var playing = false
    private var presentedFrame = false
    private var inputs: TreeRiveInputs? = null

    init { alpha = 0f }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        super.onSurfaceTextureUpdated(surface)
        if (!released && !presentedFrame) {
            presentedFrame = true
            // Surface availability does not mean a Rive frame has been drawn yet.
            animate().alpha(1f).setDuration(160L).start()
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        animate().cancel()
        presentedFrame = false
        alpha = 0f
        return super.onSurfaceTextureDestroyed(surface)
    }

    // Compose owns this view's native resources, including removal before Activity destruction.
    override fun createObserver(): LifecycleObserver = object : DefaultLifecycleObserver {}

    fun load(file: File, definition: TreeRiveDefinition,
        stage: Int, reward: Int, wilt: Int, active: Boolean, effects: Int) {
        machine = definition.stateMachine
        // The file arrives already imported, so this only instantiates the artboard.
        setRiveFile(file, artboardName = definition.artboard,
            stateMachineName = machine, autoplay = false)
        val instance = requireNotNull(controller.activeArtboard).stateMachine(machine)
        for (name in listOf("stage", "rewardLevel", "wiltLevel")) {
            require(instance.input(name) is SMINumber) { "Missing Number input: $name" }
        }
        require(instance.input("animationsEnabled") is SMIBoolean) { "Missing Boolean input: animationsEnabled" }
        for (effect in TreeEffect.entries) {
            require(instance.input(effect.inputName) is SMIBoolean) { "Missing effect toggle: " + effect.inputName }
        }
        // Input setters queue changes. Settle them before attaching a renderer or calling play(),
        // which otherwise settles Entry with the template's preview reward (5) for one frame.
        applyInputs(stage, reward, wilt, active, effects)
        controller.advance(0f)
        if (!active) pause()
    }

    fun updateState(stage: Int, reward: Int, wilt: Int, active: Boolean, running: Boolean, effects: Int) {
        if (released) return
        applyInputs(stage, reward, wilt, active, effects)
        // Pause the existing renderer without changing state-machine inputs or replacing its
        // TextureView. The last frame stays visible and playback resumes from the same point.
        val shouldPlay = active && running
        if (shouldPlay && (!playing || !controller.isAdvancing)) play(machine, isStateMachine = true)
        if (!shouldPlay && playing) pause()
        playing = shouldPlay
    }

    private fun applyInputs(stage: Int, reward: Int, wilt: Int, active: Boolean, effects: Int) {
        val next = TreeRiveInputs(stage.coerceIn(1, 10), reward.coerceIn(0, 5), wilt.coerceIn(0, 3), active, effects)
        if (inputs == next) return
        inputs = next
        setNumberState(machine, "stage", next.stage.toFloat())
        setNumberState(machine, "rewardLevel", next.reward.toFloat())
        setNumberState(machine, "wiltLevel", next.wilt.toFloat())
        setBooleanState(machine, "animationsEnabled", active)
        for (effect in TreeEffect.entries) setBooleanState(machine, effect.inputName, effect.enabled(effects))
    }

    fun releasePack() {
        if (released) return
        released = true
        animate().cancel()
        controller.isActive = false
        pause()
        findViewTreeLifecycleOwner()?.lifecycle?.removeObserver(lifecycleObserver)
        getContextAsType<LifecycleOwner>()?.lifecycle?.removeObserver(lifecycleObserver)
        controller.file = null
        controller.release()
    }
}
