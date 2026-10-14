package com.pnd.android.loop.ui.home

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import app.rive.runtime.kotlin.core.FileAsset
import app.rive.runtime.kotlin.core.FileAssetLoader
import app.rive.runtime.kotlin.core.Rive
import app.rive.runtime.kotlin.core.SMIBoolean
import app.rive.runtime.kotlin.core.SMINumber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class PreparedTreeRive(val scene: ByteArray, val images: Map<String, ByteArray>)

private sealed interface TreeRivePreparation {
    data object Loading : TreeRivePreparation
    data object Unavailable : TreeRivePreparation
    data class Ready(val data: PreparedTreeRive) : TreeRivePreparation
}

/** Both the live card and guide use this renderer and the same image pack. */
@Composable
internal fun LoopTreeScene(stage: Int, rewardLevel: Int, description: String,
    modifier: Modifier, wiltLevel: Int = 0, skin: TreeSkin? = null) {
    val pack = rememberTreeVisualPack(skin)
    val selectedTree = pack.treeImageKey(stage, wiltLevel)
    val owner = LocalLifecycleOwner.current
    var active by remember(owner) {
        mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var animationsEnabled by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ ->
            active = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            animationsEnabled = ValueAnimator.areAnimatorsEnabled()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    if (!animationsEnabled) {
        NativeTreeScene(stage, rewardLevel, description, modifier, wiltLevel, pack)
        return
    }
    // Recreate only when the selected image changes, not on clock or reward updates.
    key(pack, selectedTree) {
        val prepared by produceState<TreeRivePreparation>(TreeRivePreparation.Loading, pack, selectedTree) {
            val data = withContext(Dispatchers.IO) {
                try {
                    val scene = pack.riveBytesOrNull(stage) ?: return@withContext null
                    val slots = pack.definition.rive.imageSlots.associateWith {
                        if (it == "tree") selectedTree else it
                    }
                    PreparedTreeRive(scene, slots.mapValues { (_, image) ->
                        pack.openImage(image).use { it.readBytes() }
                    })
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("TreePack", "Cannot prepare Rive pack; using native scene", e)
                    null
                }
            }
            value = data?.let { TreeRivePreparation.Ready(it) } ?: TreeRivePreparation.Unavailable
        }
        val context = LocalContext.current
        val view = remember(prepared, context) {
            (prepared as? TreeRivePreparation.Ready)?.data?.let { data ->
                var candidate: TreePackRiveView? = null
                try {
                    TreeRiveRuntime.initialize(context)
                    candidate = TreePackRiveView(context)
                    candidate.load(data, pack.definition.rive, stage, rewardLevel, wiltLevel, active)
                    candidate
                } catch (e: Exception) {
                    candidate?.releasePack()
                    Log.w("TreePack", "Invalid Rive template; using native scene", e)
                    null
                } catch (e: LinkageError) {
                    candidate?.releasePack()
                    Log.w("TreePack", "Rive runtime unavailable; using native scene", e)
                    null
                }
            }
        }
        if (prepared == TreeRivePreparation.Loading) {
            // Reserve the card's space. The Canvas scene differs in scale/effect positions;
            // showing it while loading causes a visible jump followed by a blank TextureView.
            Box(modifier)
        } else if (view == null) {
            NativeTreeScene(stage, rewardLevel, description, modifier, wiltLevel, pack)
        } else {
            BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
                AndroidView(
                    factory = { view },
                    modifier = Modifier.size(minOf(maxWidth, maxHeight)),
                    onRelease = { it.releasePack() },
                    update = {
                        it.contentDescription = description
                        it.updateState(stage, rewardLevel, wiltLevel, active)
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

    fun load(data: PreparedTreeRive, definition: TreeRiveDefinition,
        stage: Int, reward: Int, wilt: Int, active: Boolean) {
        machine = definition.stateMachine
        val loadedSlots = mutableSetOf<String>()
        setAssetLoader(object : FileAssetLoader() {
            override fun loadContents(asset: FileAsset, inBandBytes: ByteArray): Boolean {
                val bytes = data.images[asset.name] ?: return false
                check(asset.decode(bytes)) { "Cannot decode Rive image slot: ${asset.name}" }
                loadedSlots.add(asset.name)
                return true
            }
        })
        setRiveBytes(data.scene, artboardName = definition.artboard,
            stateMachineName = machine, autoplay = false)
        require(loadedSlots.containsAll(data.images.keys)) { "Missing referenced Rive image slots" }
        val instance = requireNotNull(controller.activeArtboard).stateMachine(machine)
        for (name in listOf("stage", "rewardLevel", "wiltLevel")) {
            require(instance.input(name) is SMINumber) { "Missing Number input: $name" }
        }
        require(instance.input("animationsEnabled") is SMIBoolean) { "Missing Boolean input: animationsEnabled" }
        // Input setters queue changes. Settle them before attaching a renderer or calling play(),
        // which otherwise settles Entry with the template's preview reward (5) for one frame.
        applyInputs(stage, reward, wilt, active)
        controller.advance(0f)
        if (!active) pause()
    }

    fun updateState(stage: Int, reward: Int, wilt: Int, active: Boolean) {
        if (released) return
        applyInputs(stage, reward, wilt, active)
        if (active && (!playing || !controller.isAdvancing)) play(machine, isStateMachine = true)
        if (!active && playing) pause()
        playing = active
    }

    private fun applyInputs(stage: Int, reward: Int, wilt: Int, active: Boolean) {
        setNumberState(machine, "stage", stage.coerceIn(1, 10).toFloat())
        setNumberState(machine, "rewardLevel", reward.coerceIn(0, 5).toFloat())
        setNumberState(machine, "wiltLevel", wilt.coerceIn(0, 3).toFloat())
        setBooleanState(machine, "animationsEnabled", active)
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
        setAssetLoader(null)
    }
}
