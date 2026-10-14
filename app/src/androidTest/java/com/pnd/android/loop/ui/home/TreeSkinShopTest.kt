package com.pnd.android.loop.ui.home

import android.content.Intent
import android.os.SystemClock
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.home.viewmodel.LoopRates
import com.pnd.android.loop.ui.theme.AppTheme
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import app.rive.runtime.kotlin.RiveAnimationView
import android.view.View
import android.view.ViewGroup

@RunWith(AndroidJUnit4::class)
class TreeSkinShopTest {
    @Test fun previewUnlockAndPersistentSelectionWorkWithoutDebitingPracticePoints() = exerciseShop(TreeSkin.MAPLE)
    @Test fun pinePreviewUnlockAndSelectionWorkWithoutDebitingPracticePoints() = exerciseShop(TreeSkin.PINE)

    private fun exerciseShop(skin: TreeSkin) {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        assertTrue("This test changes only the isolated preview inventory",context.packageName.endsWith(".treepreview"))
        val store=TreeSkinStore(context)
        store.preferences.edit().clear().commit()
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").use {
            java.io.FileInputStream(it.fileDescriptor).use { stream -> stream.readBytes() }
        }
        val activity=instrumentation.startActivitySync(Intent(context,ComponentActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ComponentActivity
        val points=mutableStateOf(skin.unlockPoints-1)
        val shop=mutableStateOf(true)
        val condition=mutableStateOf(0)
        try {
            instrumentation.runOnMainSync {
                activity.setShowWhenLocked(true); activity.setTurnScreenOn(true)
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                activity.setContent {
                    AppTheme {
                        val vitality=TreeVitality(earnedStage=5,earnedReward=5,growthPoints=points.value,wiltLevel=condition.value)
                        if (shop.value) TreeSkinShop(vitality) { shop.value=false }
                        else LoopTreeCard(LoopRates.Empty,vitality,false)
                    }
                }
            }
            SystemClock.sleep(1800)
            assertFalse(skin in store.read().unlocked)
            store.select(skin)
            assertEquals(TreeSkin.GARDEN,store.read().selected)
            clickLabel(context.getString(R.string.tree_skin_preview_button,context.getString(skin.title)))
            SystemClock.sleep(1500)
            assertEquals("Preview must not equip a locked skin",TreeSkin.GARDEN,store.read().selected)
            instrumentation.runOnMainSync { points.value=skin.unlockPoints }
            SystemClock.sleep(800)
            assertTrue(skin in store.read().unlocked)
            clickLabel(context.getString(R.string.tree_skin_apply_button,context.getString(skin.title)))
            SystemClock.sleep(1000)
            assertEquals(skin,TreeSkinStore(context).read().selected)
            assertEquals("Cosmetics must not spend growth points",skin.unlockPoints,points.value)
            instrumentation.runOnMainSync { shop.value=false }
            SystemClock.sleep(1800)
            instrumentation.runOnMainSync {
                val rive=requireNotNull(findRive(activity.window.decorView))
                assertTrue(rive.isPlaying)
                assertTrue(rive.controller.activeArtboard!!.animationNames.contains(if(skin == TreeSkin.PINE) "Healthy fresh pine needles" else "Healthy rising maple leaves"))
            }
            if (skin == TreeSkin.PINE) for (wilt in 0..3) {
                instrumentation.runOnMainSync { condition.value=wilt }
                SystemClock.sleep(1500)
                val frames=mutableListOf<android.graphics.Bitmap>()
                repeat(2) {
                    instrumentation.runOnMainSync {
                        val rive=requireNotNull(findRive(activity.window.decorView))
                        assertTrue("Pine wilt $wilt must keep playing",rive.isPlaying)
                        assertEquals(wilt.toFloat(),(rive.controller.stateMachines.first().input("wiltLevel") as app.rive.runtime.kotlin.core.SMINumber).value)
                    }
                    frames.add(requireNotNull(instrumentation.uiAutomation.takeScreenshot()))
                    SystemClock.sleep(900)
                }
                val xy=IntArray(2)
                var width=0; var height=0
                instrumentation.runOnMainSync {
                    val rive=requireNotNull(findRive(activity.window.decorView))
                    rive.getLocationOnScreen(xy); width=rive.width; height=rive.height
                }
                var changed=0
                for(y in xy[1] until (xy[1]+height).coerceAtMost(frames[0].height) step 8)
                    for(x in xy[0] until (xy[0]+width).coerceAtMost(frames[0].width) step 8)
                        if(frames[0].getPixel(x,y)!=frames[1].getPixel(x,y)) changed++
                frames.forEach { it.recycle() }
                assertTrue("Pine wilt $wilt must visibly animate ($changed)",changed>15)
            }
            store.earn(0)
            assertEquals(skin,TreeSkinStore(context).read().selected)
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
            store.preferences.edit().clear().commit()
        }
    }

    private fun clickLabel(label: String) {
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        repeat(10) {
            val root=automation.rootInActiveWindow
            val target=root?.let { findNode(it) { node -> node.contentDescription?.toString()==label && node.isEnabled && node.isVisibleToUser } }
            if (target!=null) {
                val bounds=android.graphics.Rect()
                target.getBoundsInScreen(bounds)
                if (!bounds.isEmpty) {
                    automation.executeShellCommand("input tap ${bounds.centerX()} ${bounds.centerY()}").use {
                        java.io.FileInputStream(it.fileDescriptor).use { stream -> stream.readBytes() }
                    }
                    return
                }
            }
            if (root!=null) {
                val area=android.graphics.Rect()
                root.getBoundsInScreen(area)
                val x=area.left+(area.width()*0.85f).toInt()
                val from=area.top+(area.height()*0.72f).toInt()
                val to=area.top+(area.height()*0.35f).toInt()
                automation.executeShellCommand("input swipe $x $from $x $to 350").use {
                    java.io.FileInputStream(it.fileDescriptor).use { stream -> stream.readBytes() }
                }
            }
            SystemClock.sleep(450)
        }
        fail("Shop action not reachable: $label")
    }

    private fun findNode(node: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo)->Boolean): AccessibilityNodeInfo? {
        if (predicate(node)) return node
        for(i in 0 until node.childCount) node.getChild(i)?.let { findNode(it,predicate)?.let { result -> return result } }
        return null
    }

    private fun findRive(view: View): RiveAnimationView? {
        if(view is RiveAnimationView) return view
        if(view is ViewGroup) for(i in 0 until view.childCount) findRive(view.getChildAt(i))?.let { return it }
        return null
    }
}
