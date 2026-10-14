package com.pnd.android.loop.ui.home

import android.content.Intent
import android.os.SystemClock
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

    @Test fun shopPageNavigatesBackAndAppliesIndependentGardenEffects() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assertTrue(context.packageName.endsWith(".treepreview"))
        val store = TreeSkinStore(context)
        store.preferences.edit().clear().commit()
        val activity = instrumentation.startActivitySync(Intent(context, ComponentActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ComponentActivity
        val vitality = TreeVitality(earnedStage = 10, earnedReward = 5, growthPoints = 100)
        var nav: androidx.navigation.NavHostController? = null
        try {
            instrumentation.runOnMainSync {
                activity.setShowWhenLocked(true)
                activity.setTurnScreenOn(true)
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                activity.setContent {
                    AppTheme {
                        val controller = rememberNavController()
                        nav = controller
                        NavHost(controller, startDestination = "tree_test_home") {
                            composable("tree_test_home") {
                                Box(Modifier.systemBarsPadding()) {
                                    LoopTreeCard(LoopRates.Empty, vitality, false,
                                        onNavigateToTreeShop = { com.pnd.android.loop.common.NavigatePage.TreeShop.navigate(controller) })
                                }
                            }
                            composable(com.pnd.android.loop.common.NavigatePage.TreeShop.route) {
                                TreeShopPage(vitality) { controller.popBackStack() }
                            }
                        }
                    }
                }
            }
            SystemClock.sleep(1800)
            captureShop("tree-shop-navigation-home")
            clickLabel(context.getString(R.string.tree_skin_shop))
            SystemClock.sleep(1200)
            instrumentation.runOnMainSync {
                assertEquals(com.pnd.android.loop.common.NavigatePage.TreeShop.route, nav!!.currentDestination!!.route)
            }
            captureShop("tree-shop-page")
            clickLabel(context.getString(R.string.tree_shop_effects_tab))
            SystemClock.sleep(1000)
            val auroraLabel = context.getString(R.string.tree_shop_effect_toggle, context.getString(R.string.tree_reward_aurora))
            clickLabel(auroraLabel)
            SystemClock.sleep(800)
            assertEquals(TreeEffect.ALL, store.read().effectsFor(TreeSkin.GARDEN))
            clickLabel(context.getString(R.string.tree_skin_apply_button, context.getString(TreeSkin.GARDEN.title)))
            SystemClock.sleep(800)
            assertEquals(TreeEffect.ALL and TreeEffect.AURORA.bit.inv(), store.read().effectsFor(TreeSkin.GARDEN))
            captureShop("tree-shop-effects-applied")
            clickLabel(context.getString(R.string.navi_up))
            SystemClock.sleep(1500)
            instrumentation.runOnMainSync {
                assertEquals("tree_test_home", nav!!.currentDestination!!.route)
                val rive = requireNotNull(findRive(activity.window.decorView))
                val machine = rive.controller.stateMachines.first()
                assertFalse((machine.input(TreeEffect.AURORA.inputName) as app.rive.runtime.kotlin.core.SMIBoolean).value)
                assertTrue((machine.input(TreeEffect.BUTTERFLIES.inputName) as app.rive.runtime.kotlin.core.SMIBoolean).value)
            }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
            store.preferences.edit().clear().commit()
        }
    }

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
                        if (shop.value) TreeShopPage(vitality) { shop.value=false }
                        else LoopTreeCard(LoopRates.Empty,vitality,false)
                    }
                }
            }
            SystemClock.sleep(1800)
            captureShop("tree-shop-cards")
            val initialRoot = requireNotNull(instrumentation.uiAutomation.rootInActiveWindow)
            for (tree in TreeSkin.entries) {
                val label = context.getString(R.string.tree_skin_preview_button, context.getString(tree.title))
                assertNotNull("Every defined tree must have a preview card", findNode(initialRoot) {
                    it.contentDescription?.toString() == label
                })
            }
            assertFalse(skin in store.read().unlocked)
            store.select(skin)
            assertEquals(TreeSkin.GARDEN,store.read().selected)
            clickLabel(context.getString(R.string.tree_skin_preview_button,context.getString(skin.title)))
            SystemClock.sleep(1500)
            assertEquals("Preview must not equip a locked skin",TreeSkin.GARDEN,store.read().selected)
            val growthLabel = context.getString(R.string.tree_shop_growth)
            val growth = requireNotNull(findNode(requireNotNull(instrumentation.uiAutomation.rootInActiveWindow)) {
                it.contentDescription?.toString() == growthLabel && it.rangeInfo != null
            })
            assertEquals(10f, growth.rangeInfo.current, 0f)
            assertTrue(growth.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id, android.os.Bundle().apply {
                putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, 2f)
            }))
            SystemClock.sleep(1500)
            val changedGrowth = requireNotNull(findNode(requireNotNull(instrumentation.uiAutomation.rootInActiveWindow)) {
                it.contentDescription?.toString() == growthLabel && it.rangeInfo != null
            })
            assertEquals("Growth preview must reach the requested stage", 2f, changedGrowth.rangeInfo.current, 0f)
            assertEquals("Preview must not alter practice points", skin.unlockPoints - 1, points.value)
            assertEquals("Preview must not equip a locked tree", TreeSkin.GARDEN, store.read().selected)
            captureShop("tree-shop-preview-" + skin.packId)
            clickLabel(context.getString(R.string.tree_shop_effects_tab))
            SystemClock.sleep(1500)
            val effectLabel = context.getString(R.string.tree_shop_effect_toggle,
                context.getString(treeSkinRewardNames(skin)[TreeEffect.AURORA.level - 1]))
            clickLabel(effectLabel)
            SystemClock.sleep(800)
            instrumentation.runOnMainSync {
                val rive = requireNotNull(findRive(activity.window.decorView))
                val machine = rive.controller.stateMachines.first()
                assertFalse((machine.input(TreeEffect.AURORA.inputName) as app.rive.runtime.kotlin.core.SMIBoolean).value)
                assertTrue("Other effects must stay enabled", (machine.input(TreeEffect.BUTTERFLIES.inputName) as app.rive.runtime.kotlin.core.SMIBoolean).value)
            }
            assertEquals("Preview must not save effect preferences", TreeEffect.ALL, store.read().effectsFor(skin))
            captureShop("tree-shop-effects-" + skin.packId)
            instrumentation.runOnMainSync { points.value=skin.unlockPoints }
            SystemClock.sleep(800)
            assertTrue(skin in store.read().unlocked)
            clickLabel(context.getString(R.string.tree_skin_apply_button,context.getString(skin.title)))
            SystemClock.sleep(1000)
            assertEquals(skin,TreeSkinStore(context).read().selected)
            assertEquals(TreeEffect.ALL and TreeEffect.AURORA.bit.inv(), TreeSkinStore(context).read().effectsFor(skin))
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
            assertEquals(TreeEffect.ALL and TreeEffect.AURORA.bit.inv(), TreeSkinStore(context).read().effectsFor(skin))
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
            store.preferences.edit().clear().commit()
        }
    }

    private fun captureShop(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let { java.io.File(it) }
            ?: requireNotNull(instrumentation.targetContext.getExternalFilesDir(null))
        directory.mkdirs()
        val file = java.io.File(directory, "$name.png")
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun clickLabel(label: String) {
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        repeat(10) {
            val root=automation.rootInActiveWindow
            val target=root?.let { findNode(it) { node -> (node.contentDescription?.toString()==label || node.text?.toString()==label) && node.isEnabled && node.isVisibleToUser } }
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
