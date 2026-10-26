package com.darkempire78.opencalculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * English (default) and Brazilian Portuguese must be complete for every string the multi-mode
 * work added, with matching format placeholders. Other languages fall back to English.
 */
class StringResourcesTest {

    private val resDir: File = listOf("src/main/res", "app/src/main/res").map(::File).first { it.exists() }

    private fun load(folder: String): Map<String, String> {
        val file = File(resDir, "$folder/strings.xml")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) }
            .filter { it.attributes.getNamedItem("translatable")?.nodeValue != "false" }
            .associate { it.attributes.getNamedItem("name").nodeValue to it.textContent }
    }

    private val english = load("values")
    private val portugueseBrazil = load("values-pt-rBR")
    private val portuguese = load("values-pt")

    private val ownedPrefixes = listOf(
        "fin_", "hist_", "mode_", "menu_memory", "menu_insert_previous", "memory_", "no_previous_result",
        "settings_financial", "settings_restore", "settings_formatting_decimal", "settings_formatting_grouping",
        "settings_formatting_note", "settings_category_financial", "sep_", "payment_mode_", "menu_filter_history",
        "paste_invalid_number", "toggle_sign_action", "nthRootDesc", "reciprocalDesc", "absoluteValueDesc"
    )

    private fun owned(map: Map<String, String>) = map.keys.filter { key -> ownedPrefixes.any { key.startsWith(it) } }

    private fun placeholders(text: String) = Regex("%(\\d+)\\$[sd]").findAll(text).map { it.groupValues[1] }.sorted().toList()

    @Test
    fun portugueseBrazilHasEveryOwnedString() {
        val missing = owned(english).filter { it !in portugueseBrazil }
        assertTrue("missing in values-pt-rBR: $missing", missing.isEmpty())
    }

    @Test
    fun portugueseHasEveryOwnedString() {
        val missing = owned(english).filter { it !in portuguese }
        assertTrue("missing in values-pt: $missing", missing.isEmpty())
    }

    @Test
    fun ownedStringsAreNotEmpty() {
        for ((name, text) in english.filterKeys { it in owned(english) }) assertTrue("$name is empty", text.isNotBlank())
        for ((name, text) in portugueseBrazil.filterKeys { it in owned(english) }) assertTrue("$name (pt-BR) is empty", text.isNotBlank())
    }

    @Test
    fun placeholdersMatchBetweenLanguages() {
        for (name in owned(english)) {
            val en = placeholders(english.getValue(name))
            val pt = placeholders(portugueseBrazil[name] ?: continue)
            assertEquals("placeholders of $name differ", en, pt)
        }
    }

    @Test
    fun brazilianTerminologyFollowsTheSpecification() {
        val expected = mapOf(
            "mode_basic" to "Calculadora básica",
            "mode_scientific" to "Calculadora científica",
            "fin_key_pv" to "Valor presente",
            "fin_key_fv" to "Valor futuro",
            "fin_key_pmt" to "Pagamento",
            "fin_key_n" to "Número de períodos",
            "fin_key_i" to "Taxa por período",
            "fin_key2_npv" to "Valor presente líquido",
            "fin_key2_irr" to "Taxa interna de retorno",
            "fin_reg_flows" to "Fluxos de caixa",
            "fin_menu_clear_registers" to "Limpar registradores",
            "fin_reg_mode_begin" to "início do período",
            "fin_reg_mode_end" to "final do período"
        )
        for ((name, text) in expected) assertEquals(name, text, portugueseBrazil[name])
        assertTrue(portugueseBrazil.getValue("mode_financial").startsWith("Calculadora financeira"))
        assertTrue(portugueseBrazil.getValue("fin_title").startsWith("Calculadora financeira"))
        assertTrue(portugueseBrazil.getValue("fin_key2_amort").contains("Amortizar"))
        assertEquals("Tabela de amortização", portugueseBrazil["fin_menu_amortization"])
    }

    @Test
    fun noHardCodedFinancialLabelsAreMissingFromTheDefaultLanguage() {
        // every fin_* string used by the panel code exists in the default resources
        val panel = File(resDir.parentFile, "java/com/darkempire78/opencalculator/financial/FinancialPanel.kt").readText()
        val used = Regex("R\\.string\\.(fin_[a-z0-9_]+)").findAll(panel).map { it.groupValues[1] }.toSet()
        val missing = used.filter { it !in english }
        assertTrue("strings used by FinancialPanel but not defined: $missing", missing.isEmpty())
    }
}
