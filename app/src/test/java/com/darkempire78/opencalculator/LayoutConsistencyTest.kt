package com.darkempire78.opencalculator

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The app has one layout per screen class. A view added to one variant but forgotten in another
 * crashes only on that screen class (a missing history_item_type crashed small screens and tablets),
 * so every variant of a layout must define the ids the code looks up.
 */
class LayoutConsistencyTest {

    private val resDir: File = listOf("src/main/res", "app/src/main/res").map(::File).first { it.exists() }

    private fun variants(fileName: String): List<File> =
        resDir.listFiles { f -> f.isDirectory && f.name.startsWith("layout") }!!
            .map { File(it, fileName) }
            .filter { it.exists() }

    private fun assertDefines(fileName: String, ids: List<String>) {
        val files = variants(fileName)
        assertTrue("no variants of $fileName found", files.isNotEmpty())
        for (file in files) {
            val text = file.readText()
            val missing = ids.filter { !text.contains("@+id/$it\"") }
            assertTrue("${file.parentFile.name}/$fileName is missing ids: $missing", missing.isEmpty())
        }
    }

    @Test
    fun everyHistoryItemLayoutDefinesTheViewsTheAdapterUses() = assertDefines(
        "history_item.xml",
        listOf(
            "history_item_calculation", "history_item_result", "history_item_type",
            "history_time", "history_separator", "history_same_date_separator"
        )
    )

    @Test
    fun everyMainLayoutDefinesTheModeAndFinancialViews() = assertDefines(
        "activity_main.xml",
        listOf(
            "modeSwitcherInclude", "financialPlaceholderInclude", "menuButton", "degreeTextView",
            "input", "resultDisplay", "equalsButton", "history_recylcle_view", "sliding_layout"
        )
    )

    @Test
    fun theOtherPartsOfTheMainScreenExistInEveryMainLayout() {
        val modeSwitcher = File(resDir, "layout/view_mode_switcher.xml").readText()
        for (id in listOf("modeBasicButton", "modeScientificButton", "modeFinancialButton")) {
            assertTrue("view_mode_switcher is missing $id", modeSwitcher.contains("@+id/$id\""))
        }
    }
}
