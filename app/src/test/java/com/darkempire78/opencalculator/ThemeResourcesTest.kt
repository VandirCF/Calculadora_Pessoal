package com.darkempire78.opencalculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.pow

/**
 * The two themes (Flamengo and Cleveland Cavaliers) are only data: colors.xml holds the palettes and
 * styles.xml maps them onto theme attributes. These tests keep both complete, readable and free of
 * colors scattered around the project.
 */
class ThemeResourcesTest {

    private val resDir: File = listOf("src/main/res", "app/src/main/res").map(::File).first { it.exists() }
    private val mainDir: File = resDir.parentFile
    private val themes = listOf("flamengo", "cavaliers")

    private fun xml(path: String) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(resDir, path))

    private val colors: Map<String, String> = xml("values/colors.xml").getElementsByTagName("color").let { n ->
        (0 until n.length).associate { n.item(it).attributes.getNamedItem("name").nodeValue to n.item(it).textContent.trim() }
    }

    private fun token(theme: String, name: String) = colors.getValue("${theme}_$name")

    private val tokens = listOf(
        "background", "surface", "surface_variant", "display", "primary", "secondary", "accent",
        "text_primary", "text_secondary", "key_number", "key_operator", "key_function", "key_danger",
        "key_financial", "key_enter", "on_key_enter", "shift_f", "shift_g", "ripple", "outline"
    )

    // ---- completeness -----------------------------------------------------------------------------------
    @Test
    fun bothThemesDefineEveryToken() {
        for (theme in themes) {
            val missing = tokens.filter { "${theme}_$it" !in colors }
            assertTrue("$theme is missing tokens: $missing", missing.isEmpty())
        }
    }

    @Test
    fun noOtherThemePaletteLingers() {
        val unexpected = colors.keys.filter { name -> themes.none { name.startsWith("${it}_") } && name != "calculation_error_color" }
        assertTrue("colors outside the two palettes: $unexpected", unexpected.isEmpty())
    }

    private fun styleItems(styleName: String): Map<String, String> {
        val styles = xml("values/styles.xml").getElementsByTagName("style")
        for (i in 0 until styles.length) {
            val style = styles.item(i)
            if (style.attributes.getNamedItem("name").nodeValue == styleName) {
                val items = style.childNodes
                return (0 until items.length).map { items.item(it) }.filter { it.nodeName == "item" }
                    .associate { it.attributes.getNamedItem("name").nodeValue to it.textContent.trim() }
            }
        }
        error("style $styleName not found")
    }

    private val attributes = listOf(
        "calc_background", "calc_surface", "calc_surface_variant", "calc_display_background", "calc_primary",
        "calc_secondary", "calc_accent", "calc_text_primary", "calc_text_secondary", "calc_key_number",
        "calc_key_operator", "calc_key_function", "calc_key_danger", "calc_key_financial", "calc_key_enter",
        "calc_outline", "calc_logo", "calc_error", "calc_on_key_enter", "calc_shift_f", "calc_shift_g",
        // inherited attributes that the original layouts still use
        "foreground_color", "background_color", "popup_background_color", "history_background_color", "text_color",
        "text_second_color", "text_third_color", "text_category_color", "button_color", "button_symbol_color",
        "button_clear_color", "button_equals_color", "button_ripple_color", "separator_color", "about_button_color"
    )

    @Test
    fun bothThemeStylesDefineEveryAttribute() {
        for (name in listOf("Theme.Calc.Flamengo", "Theme.Calc.Cavaliers")) {
            val items = styleItems(name)
            val missing = attributes.filter { it !in items }
            assertTrue("$name is missing attributes: $missing", missing.isEmpty())
        }
    }

    @Test
    fun everyAttributeIsDeclared() {
        val declared = File(resDir, "values/attrs.xml").readText()
        val undeclared = attributes.filter { !declared.contains("name=\"$it\"") }
        assertTrue("attributes used by the themes but not declared: $undeclared", undeclared.isEmpty())
    }

    @Test
    fun eachThemeUsesItsOwnPalette() {
        for ((name, theme) in listOf("Theme.Calc.Flamengo" to "flamengo", "Theme.Calc.Cavaliers" to "cavaliers")) {
            val colorReferences = styleItems(name).values.filter { it.startsWith("@color/") && !it.endsWith("calculation_error_color") }
            val foreign = colorReferences.filter { !it.startsWith("@color/${theme}_") }
            assertTrue("$name uses colors of another theme: $foreign", foreign.isEmpty())
        }
    }

    @Test
    fun oldThemesAreGone() {
        val styles = File(resDir, "values/styles.xml").readText()
        for (old in listOf("AmoledTheme", "MaterialYouTheme", "name=\"AppTheme\"")) assertTrue("$old still exists", !styles.contains(old))
        assertTrue("values-night must not exist", !File(resDir, "values-night").exists())
    }

    // ---- contrast (WCAG 2.1) ---------------------------------------------------------------------------------
    private fun luminance(hex: String): Double {
        val value = hex.removePrefix("#").takeLast(6)
        val channels = (0 until 3).map { Integer.parseInt(value.substring(it * 2, it * 2 + 2), 16) / 255.0 }
            .map { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
        return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]
    }

    private fun contrast(a: String, b: String): Double {
        val (high, low) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (high + 0.05) / (low + 0.05)
    }

    @Test
    fun textIsReadableOnEveryKeyAndSurface() {
        for (theme in themes) {
            val text = token(theme, "text_primary")
            for (surface in listOf("background", "surface", "surface_variant", "display", "key_number", "key_operator",
                "key_function", "key_danger", "key_financial")) {
                val ratio = contrast(text, token(theme, surface))
                assertTrue("$theme: text on $surface has contrast $ratio (< 4.5)", ratio >= 4.5)
            }
        }
    }

    @Test
    fun enterAndShiftKeysHaveReadableText() {
        for (theme in themes) {
            val ratio = contrast(token(theme, "on_key_enter"), token(theme, "key_enter"))
            assertTrue("$theme: text on enter key has contrast $ratio (< 4.5)", ratio >= 4.5)
            for (shift in listOf("shift_f", "shift_g")) {
                assertTrue("$theme: $shift key text", contrast(token(theme, "background"), token(theme, shift)) >= 4.5)
                // the printed f and g functions sit on the dark key faces and above them
                for (surface in listOf("background", "key_financial", "key_function", "key_number"))
                    assertTrue("$theme: $shift label on $surface", contrast(token(theme, shift), token(theme, surface)) >= 4.5)
            }
        }
    }

    @Test
    fun theTwoThemesDoNotShareSimilarReds() {
        // Flamengo's vivid red must not be mistaken for the Cavaliers' wine
        for (name in listOf("primary", "key_operator", "key_enter")) {
            val d = distance(token("flamengo", name), token("cavaliers", name))
            assertTrue("$name looks too similar in both themes (distance $d)", d >= 100)
        }
    }

    @Test
    fun secondaryTextAndAccentAreReadableOnTheDarkSurfaces() {
        for (theme in themes) {
            for (surface in listOf("background", "surface", "display")) {
                assertTrue("$theme: secondary text on $surface", contrast(token(theme, "text_secondary"), token(theme, surface)) >= 4.5)
                assertTrue("$theme: accent on $surface", contrast(token(theme, "accent"), token(theme, surface)) >= 4.5)
            }
            assertTrue("$theme: accent on number keys", contrast(token(theme, "accent"), token(theme, "key_number")) >= 4.5)
            assertTrue("$theme: error color on display", contrast(colors.getValue("calculation_error_color"), token(theme, "display")) >= 4.5)
        }
    }

    private fun distance(a: String, b: String): Double {
        fun rgb(hex: String) = hex.removePrefix("#").takeLast(6).let { v -> (0 until 3).map { Integer.parseInt(v.substring(it * 2, it * 2 + 2), 16) } }
        val (x, y) = rgb(a) to rgb(b)
        return Math.sqrt((0 until 3).sumOf { ((x[it] - y[it]).toDouble()).pow(2) })
    }

    @Test
    fun keyGroupsCanBeToldApart() {
        // number, operator, enter and clear keys differ by hue or brightness (RGB distance, not only luminance)
        for (theme in themes) {
            val groups = listOf("key_number", "key_operator", "key_enter", "key_danger")
            for (i in groups.indices) for (j in i + 1 until groups.size) {
                val d = distance(token(theme, groups[i]), token(theme, groups[j]))
                assertTrue("$theme: ${groups[i]} and ${groups[j]} look too similar (distance $d)", d >= 45)
            }
        }
    }

    @Test
    fun themesAreDarkAndDistinct() {
        for (theme in themes) assertTrue("$theme background must be dark", luminance(token(theme, "background")) < 0.02)
        assertTrue(token("flamengo", "primary") != token("cavaliers", "primary"))
        assertTrue(token("flamengo", "accent") != token("cavaliers", "accent"))
    }

    // ---- logos ---------------------------------------------------------------------------------------------------
    @Test
    fun logosExistAsTransparentPngs() {
        for (name in listOf("logo_flamengo.png", "logo_cavaliers.png")) {
            val file = File(resDir, "drawable-nodpi/$name")
            assertTrue("$name is missing: see docs/TEMAS.md", file.exists())
            val bytes = file.readBytes()
            assertTrue("$name must be a PNG", bytes.size > 1000 && bytes[1] == 'P'.code.toByte() && bytes[2] == 'N'.code.toByte())
            // IHDR color type 6 = RGBA (transparent background)
            assertEquals("$name must have an alpha channel", 6, bytes[25].toInt())
        }
    }

    // ---- no color outside the theme files ---------------------------------------------------------------------------
    @Test
    fun layoutsAndMenusHaveNoHardCodedColors() {
        val hex = Regex("=\"#[0-9a-fA-F]{3,8}\"")
        val offenders = resDir.walkTopDown()
            .filter { it.extension == "xml" && (it.parentFile.name.startsWith("layout") || it.parentFile.name == "menu" || it.parentFile.name == "xml") }
            .filter { hex.containsMatchIn(it.readText()) }.map { "${it.parentFile.name}/${it.name}" }.toList()
        assertTrue("hard-coded colors: $offenders", offenders.isEmpty())
    }

    @Test
    fun kotlinCodeDoesNotHardCodeColors() {
        val literal = Regex("0x[fF]{2}[0-9a-fA-F]{6}|Color\\.parseColor|Color\\.rgb")
        val offenders = File(mainDir, "java").walkTopDown().filter { it.extension == "kt" && literal.containsMatchIn(it.readText()) }
            .map { it.name }.toList()
        assertTrue("colors hard-coded in Kotlin: $offenders", offenders.isEmpty())
    }

    @Test
    fun unknownThemeIdsFallBackToFlamengo() {
        assertEquals(ThemeOption.FLAMENGO, ThemeOption.fromId(null))
        assertEquals(ThemeOption.FLAMENGO, ThemeOption.fromId("AMOLED"))
        assertEquals(ThemeOption.CAVALIERS, ThemeOption.fromId("CAVALIERS"))
        assertEquals(2, ThemeOption.entries.size)
    }
}
