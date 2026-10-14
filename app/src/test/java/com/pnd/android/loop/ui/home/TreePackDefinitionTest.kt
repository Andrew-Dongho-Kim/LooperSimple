package com.pnd.android.loop.ui.home

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TreePackDefinitionTest {
    @Test fun mapleUsesFortyImagesAndOnlyOneExternalRiveSlot() {
        val relative = "src/main/assets/tree-packs/maple/pack.json"
        val file = listOf(File(relative), File("app/$relative")).first { it.isFile }
        val maple = TreePackDefinition.parse(file.readText())
        assertEquals(40, maple.images.size)
        assertEquals(listOf("tree"), maple.rive.imageSlots)
        for (stage in 1..10) for (wilt in 0..3) {
            val name = "tree_%02d".format(stage) + if (wilt == 0) "" else "_wilt_$wilt"
            assertEquals("loop_maple_$name", maple.images.getValue(maple.treeKey(stage,wilt)).resource)
        }
    }

    @Test fun pineUsesFortyImagesAndOnlyOneExternalRiveSlot() {
        val relative = "src/main/assets/tree-packs/pine/pack.json"
        val file = listOf(File(relative), File("app/$relative")).first { it.isFile }
        val pine = TreePackDefinition.parse(file.readText())
        assertEquals(40, pine.images.size)
        assertEquals(listOf("tree"), pine.rive.imageSlots)
        for (stage in 1..10) for (wilt in 0..3) {
            val name = "tree_%02d".format(stage) + if (wilt == 0) "" else "_wilt_$wilt"
            assertEquals("loop_pine_$name", pine.images.getValue(pine.treeKey(stage,wilt)).resource)
        }
    }

    @Test fun rejectsUnsupportedAndDuplicateRiveSlots() {
        for (slots in listOf(listOf("tree","other"),listOf("tree","tree"),listOf("butterfly"),emptyList())) {
            val root = manifest()
            val array = (root["rive"] as ObjectNode).putArray("imageSlots")
            slots.forEach { array.add(it) }
            assertThrows(IllegalArgumentException::class.java) { TreePackDefinition.parse(root.toString()) }
        }
    }

    private fun manifest(): ObjectNode {
        val relative = "src/main/assets/tree-packs/default/pack.json"
        val file = listOf(File(relative), File("app/$relative")).first { it.isFile }
        return ObjectMapper().readTree(file) as ObjectNode
    }

    @Test fun selectsAllGrowthAndHealthCombinations() {
        val pack = TreePackDefinition.parse(manifest().toString())
        for (stage in 1..10) for (wilt in 0..3) {
            val key = "tree_%02d".format(stage) + if (wilt == 0) "" else "_wilt_$wilt"
            assertEquals(key, pack.treeKey(stage, wilt))
        }
        assertEquals(46, pack.images.size)
        assertEquals("tree_01", pack.treeKey(-1, -1))
        assertEquals("tree_10_wilt_3", pack.treeKey(11, 4))
    }

    @Test fun rejectsUnknownSchemas() {
        val root = manifest().put("schemaVersion", 2)
        assertThrows(IllegalArgumentException::class.java) { TreePackDefinition.parse(root.toString()) }
    }

    @Test fun selectsStageSpecificRiveAndPreservesSingleTemplateCompatibility() {
        val root = manifest()
        val pack = TreePackDefinition.parse(root.toString())
        for (stage in 1..10) assertEquals("tree_%02d.riv".format(stage), pack.rive.pathForStage(stage))
        (root["rive"] as ObjectNode).remove("stagePaths")
        val legacy = TreePackDefinition.parse(root.toString())
        assertEquals("tree.riv", legacy.rive.pathForStage(10))
    }

    @Test fun rejectsMissingImagesAndConditions() {
        val missingImage = manifest()
        (missingImage["images"] as ObjectNode).remove("butterfly")
        assertThrows(IllegalArgumentException::class.java) { TreePackDefinition.parse(missingImage.toString()) }
        val missingCondition = manifest()
        (missingCondition["trees"][0] as com.fasterxml.jackson.databind.node.ArrayNode).remove(3)
        assertThrows(IllegalArgumentException::class.java) { TreePackDefinition.parse(missingCondition.toString()) }
    }

    @Test fun rejectsPathsOutsidePack() {
        for (path in listOf("../tree.png", "/tree.png", "C:/tree.png", "images\\tree.png", "images//tree.png")) {
            val root = manifest()
            (root["images"]["tree_01"] as ObjectNode).put("path", path)
            assertThrows(IllegalArgumentException::class.java) { TreePackDefinition.parse(root.toString()) }
        }
        val root = manifest()
        (root["rive"] as ObjectNode).put("path", "../tree.riv")
        assertThrows(IllegalArgumentException::class.java) { TreePackDefinition.parse(root.toString()) }
        val invalidStage = manifest()
        (invalidStage["rive"]["stagePaths"] as com.fasterxml.jackson.databind.node.ArrayNode).set(0,
            com.fasterxml.jackson.databind.node.TextNode("../tree_01.riv"))
        assertThrows(IllegalArgumentException::class.java) { TreePackDefinition.parse(invalidStage.toString()) }
    }
}
