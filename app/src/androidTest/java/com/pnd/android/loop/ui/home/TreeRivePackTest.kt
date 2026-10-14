package com.pnd.android.loop.ui.home

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.controllers.RiveFileController
import app.rive.runtime.kotlin.core.FileAsset
import app.rive.runtime.kotlin.core.FileAssetLoader
import app.rive.runtime.kotlin.core.Rive
import app.rive.runtime.kotlin.core.SMIBoolean
import app.rive.runtime.kotlin.core.SMINumber
import app.rive.runtime.kotlin.core.File as RiveFile
import com.pnd.android.loop.ui.theme.AppTheme
import com.pnd.android.loop.ui.home.viewmodel.LoopRates
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TreeRivePackTest {
    @Test fun allMapleStagesLoadAndKeepTheConditionAnimationsRunning() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Rive.init(context)
        val pack = TreeVisualPack.load(context, TreeSkin.MAPLE)
        assertArrayEquals(context.assets.open("tree-packs/maple/tree.riv").use { it.readBytes() },pack.riveBytesOrNull(1))
        for (stage in 1..10) {
            val loaded = mutableSetOf<String>()
            val loader = object : FileAssetLoader() {
                override fun loadContents(asset: FileAsset, inBandBytes: ByteArray): Boolean {
                    if (asset.name != "tree") return false
                    loaded.add(asset.name)
                    return pack.openImage(pack.treeImageKey(stage,0)).use { asset.decode(it.readBytes()) }
                }
            }
            val file = RiveFile(requireNotNull(pack.riveBytesOrNull(stage)),fileAssetLoader=loader)
            val controller = RiveFileController()
            try {
                val board = file.artboard("Tree")
                controller.file=file; controller.activeArtboard=board
                assertEquals(setOf("tree"),loaded)
                assertTrue(board.animationNames.contains("Healthy rising maple leaves"))
                assertTrue(board.animationNames.contains("Samaras riding the breeze"))
                for (wilt in 0..3) for (reward in 0..5) {
                    controller.setNumberState("TreeState","wiltLevel",wilt.toFloat())
                    controller.setNumberState("TreeState","rewardLevel",reward.toFloat())
                    controller.setBooleanState("TreeState","animationsEnabled",true)
                    repeat(20) { controller.advance(0.1f) }
                    assertTrue("Maple $stage wilt $wilt reward $reward stopped",controller.isAdvancing)
                }
                repeat(1200) { controller.advance(0.1f) }
                assertTrue(controller.isAdvancing)
            } finally { controller.release(); file.release(); loader.release() }
        }
    }
    @Test fun allPineStagesLoadAndKeepTheConditionAnimationsRunning() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Rive.init(context)
        val pack = TreeVisualPack.load(context, TreeSkin.PINE)
        assertArrayEquals(context.assets.open("tree-packs/pine/tree.riv").use { it.readBytes() },pack.riveBytesOrNull(1))
        for (stage in 1..10) {
            val loaded = mutableSetOf<String>()
            val loader = object : FileAssetLoader() {
                override fun loadContents(asset: FileAsset, inBandBytes: ByteArray): Boolean {
                    if (asset.name != "tree") return false
                    loaded.add(asset.name)
                    return pack.openImage(pack.treeImageKey(stage,0)).use { asset.decode(it.readBytes()) }
                }
            }
            val file = RiveFile(requireNotNull(pack.riveBytesOrNull(stage)),fileAssetLoader=loader)
            val controller = RiveFileController()
            try {
                val board = file.artboard("Tree")
                controller.file=file; controller.activeArtboard=board
                assertEquals(setOf("tree"),loaded)
                assertTrue(board.animationNames.contains("Healthy fresh pine needles"))
                assertTrue(board.animationNames.contains("Pine forest atmosphere"))
                for (wilt in 0..3) for (reward in 0..5) {
                    controller.setNumberState("TreeState","wiltLevel",wilt.toFloat())
                    controller.setNumberState("TreeState","rewardLevel",reward.toFloat())
                    controller.setBooleanState("TreeState","animationsEnabled",true)
                    repeat(20) { controller.advance(0.1f) }
                    assertTrue("Pine $stage wilt $wilt reward $reward stopped",controller.isAdvancing)
                }
                repeat(1200) { controller.advance(0.1f) }
                assertTrue(controller.isAdvancing)
            } finally { controller.release(); file.release(); loader.release() }
        }
    }
    @Test fun allTenFilesLoadOnThePinnedAndroidRuntime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Rive.init(context)
        val pack = TreeVisualPack.load(context)
        assertArrayEquals("Default template must match the first stage",
            context.assets.open("tree-packs/default/tree.riv").use { it.readBytes() },
            pack.riveBytesOrNull(1))
        for (stage in 1..10) {
            val slots = mutableSetOf<String>()
            val loader = object : FileAssetLoader() {
                override fun loadContents(asset: FileAsset, inBandBytes: ByteArray): Boolean {
                    val key = if (asset.name == "tree") pack.treeImageKey(stage, 0) else asset.name
                    if (key !in pack.definition.images) return false
                    slots.add(asset.name)
                    return pack.openImage(key).use { asset.decode(it.readBytes()) }
                }
            }
            val file = RiveFile(requireNotNull(pack.riveBytesOrNull(stage)), fileAssetLoader = loader)
            val controller = RiveFileController()
            try {
                assertEquals(setOf("tree") + TreePackDefinition.effectKeys, slots)
                val artboard = file.artboard("Tree")
                assertTrue("Stage $stage is missing motion timelines", artboard.animationCount >= 30)
                assertTrue(artboard.animationNames.contains("Healthy rising greenery"))
                for (wilt in 1..3) assertTrue(artboard.animationNames.contains("Wilt $wilt falling leaves"))
                for (wilt in 1..3) {
                    assertTrue("Stage $stage needs a distinct wilt $wilt animation",
                        artboard.animationNames.any { it.contains("Wilt $wilt") })
                }
                controller.file = file
                controller.activeArtboard = artboard
                controller.setNumberState("TreeState", "stage", stage.toFloat())
                controller.advance(0.1f)
                val machine = controller.stateMachines.first()
                for (wilt in 0..3) for (reward in 0..5) {
                    controller.setNumberState("TreeState", "wiltLevel", wilt.toFloat())
                    controller.setNumberState("TreeState", "rewardLevel", reward.toFloat())
                    controller.setBooleanState("TreeState", "animationsEnabled", true)
                    repeat(10) { controller.advance(0.1f) }
                    assertEquals(wilt.toFloat(), (machine.input("wiltLevel") as SMINumber).value)
                    assertEquals(reward.toFloat(), (machine.input("rewardLevel") as SMINumber).value)
                    assertTrue("Stage $stage wilt $wilt reward $reward stopped looping", controller.isAdvancing)
                }
                repeat(1200) { controller.advance(0.1f) }
                assertTrue("Stage $stage stopped after 120 seconds", controller.isAdvancing)
                controller.setBooleanState("TreeState", "animationsEnabled", false)
                controller.advance(0.1f)
                assertFalse((machine.input("animationsEnabled") as SMIBoolean).value)
            } finally {
                controller.release()
                file.release()
                loader.release()
            }
        }
    }

    @Test fun cardActuallyUsesRiveAndAnimates() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").use {
            java.io.FileInputStream(it.fileDescriptor).use { stream -> stream.readBytes() }
        }
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(Intent(context, ComponentActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ComponentActivity
        val stage = mutableStateOf(5)
        val reward = mutableStateOf(0)
        val wilt = mutableStateOf(0)
        val cardWidth = mutableStateOf(300)
        val directory = File(context.getExternalFilesDir(null), "tree-rive-renders").apply { mkdirs() }
        try {
            instrumentation.runOnMainSync {
                if (Build.VERSION.SDK_INT >= 27) {
                    activity.setShowWhenLocked(true)
                    activity.setTurnScreenOn(true)
                }
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                activity.setContent {
                    AppTheme {
                        Box(Modifier.padding(24.dp)) {
                            Box(Modifier.width(cardWidth.value.dp)) {
                                LoopTreeCard(
                                    overallRates = LoopRates.Empty,
                                    vitality = TreeVitality(earnedStage = stage.value, earnedReward = reward.value,
                                        wiltLevel = wilt.value),
                                    hasEstimatedHistory = false,
                                )
                            }
                        }
                    }
                }
            }
            assertStartupDoesNotFlash(instrumentation, activity)
            val motionNames = mapOf(1 to "Seed breathing", 5 to "Growing crown", 10 to "Grand tree")
            // Cover multiple full loops and TextureView surface recreation as Home relayouts.
            repeat(12) { iteration ->
                instrumentation.runOnMainSync { cardWidth.value = if (iteration % 2 == 0) 190 else 300 }
                SystemClock.sleep(4500)
                assertVisibleMotion(instrumentation, activity, "After ${iteration + 1} relayouts")
            }
            instrumentation.runOnMainSync { activity.moveTaskToBack(true) }
            SystemClock.sleep(1500)
            instrumentation.runOnMainSync {
                assertFalse("Activity did not stop", activity.lifecycle.currentState
                    .isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED))
                activity.startActivity(Intent(activity, ComponentActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            SystemClock.sleep(2000)
            assertVisibleMotion(instrumentation, activity, "After background and resume")
            for (level in listOf(0, 5)) for (value in listOf(1, 5, 10)) for (health in 0..3) {
                instrumentation.runOnMainSync { stage.value = value; reward.value = level; wilt.value = health }
                SystemClock.sleep(1500)
                assertStartupDoesNotFlash(instrumentation, activity)
                var view: RiveAnimationView? = null
                instrumentation.runOnMainSync { view = findRive(activity.window.decorView) }
                assertNotNull("Stage $value reward $level fell back to native Canvas", view)
                instrumentation.runOnMainSync {
                    val machine = view!!.controller.stateMachines.first()
                    assertEquals(level.toFloat(), (machine.input("rewardLevel") as SMINumber).value)
                    assertEquals(health.toFloat(), (machine.input("wiltLevel") as SMINumber).value)
                    assertTrue((machine.input("animationsEnabled") as SMIBoolean).value)
                }
                val first = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
                SystemClock.sleep(900)
                val second = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
                val xy = IntArray(2)
                var width = 0
                var height = 0
                instrumentation.runOnMainSync {
                    view!!.getLocationOnScreen(xy)
                    width = view!!.width
                    height = view!!.height
                    assertTrue(view!!.isPlaying)
                    assertTrue("Stage $value selected the wrong Rive file",
                        view!!.controller.activeArtboard!!.animationNames.any { it.startsWith(motionNames.getValue(value)) })
                }
                var changed = 0
                for (y in xy[1] until (xy[1]+height).coerceAtMost(first.height) step 8) {
                    for (x in xy[0] until (xy[0]+width).coerceAtMost(first.width) step 8) {
                        if (first.getPixel(x,y) != second.getPixel(x,y)) changed++
                    }
                }
                val minimum = if (level == 0 && value == 1) 2 else 20
                assertTrue("Stage $value wilt $health reward $level has no visible motion ($changed)", changed > minimum)
                val crop = Bitmap.createBitmap(second, xy[0],xy[1],width,height)
                File(directory,"tree_%02d_wilt_%d_reward_%d.png".format(value,health,level)).outputStream().use {
                    crop.compress(Bitmap.CompressFormat.PNG,100,it)
                }
                crop.recycle(); first.recycle(); second.recycle()
            }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    private fun assertStartupDoesNotFlash(instrumentation: android.app.Instrumentation,
        activity: ComponentActivity) {
        var visible = false
        var previousAlpha = 0f
        repeat(40) {
            instrumentation.runOnMainSync {
                val view = findRive(activity.window.decorView)
                if (view != null && view.alpha > 0f) {
                    assertTrue("First frame faded out again", view.alpha >= previousAlpha)
                    previousAlpha = view.alpha
                    val frame = requireNotNull(view.bitmap) { "Visible TextureView has no frame" }
                    var pixels = 0
                    for (y in 0 until frame.height step 8) for (x in 0 until frame.width step 8) {
                        if (android.graphics.Color.alpha(frame.getPixel(x, y)) > 20) pixels++
                    }
                    frame.recycle()
                    assertTrue("Visible TextureView exposed an empty frame", pixels > 10)
                    visible = true
                } else if (visible) {
                    fail("Tree vanished after its first visible frame")
                }
            }
            SystemClock.sleep(35)
        }
        assertTrue("Tree did not present its first frame", visible)
    }

    private fun assertVisibleMotion(instrumentation: android.app.Instrumentation,
        activity: ComponentActivity, label: String) {
        var view: RiveAnimationView? = null
        val xy = IntArray(2)
        var width = 0
        var height = 0
        instrumentation.runOnMainSync {
            view = findRive(activity.window.decorView)
            assertNotNull("$label: native fallback", view)
            assertTrue("$label: renderer stopped", view!!.isPlaying)
            assertTrue("$label: state machine stopped", view!!.controller.isAdvancing)
            view!!.getLocationOnScreen(xy)
            width = view!!.width; height = view!!.height
        }
        val first = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        SystemClock.sleep(900)
        val second = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        var changed = 0
        for (y in xy[1] until (xy[1] + height).coerceAtMost(first.height) step 8)
            for (x in xy[0] until (xy[0] + width).coerceAtMost(first.width) step 8)
                if (first.getPixel(x, y) != second.getPixel(x, y)) changed++
        first.recycle(); second.recycle()
        Log.i("TreePackTest", "$label: $changed changed pixels")
        assertTrue("$label: no visible motion ($changed)", changed > 20)
    }

    private fun findRive(view: View): RiveAnimationView? {
        if (view is RiveAnimationView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findRive(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}
