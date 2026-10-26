package com.darkempire78.opencalculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The app has one language: Brazilian Portuguese. The default resources ARE the Portuguese ones, no
 * other language folder exists, and the visible texts must not contain English.
 */
class StringResourcesTest {

    private val resDir: File = listOf("src/main/res", "app/src/main/res").map(::File).first { it.exists() }
    private val mainDir: File = resDir.parentFile

    private fun load(): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(resDir, "values/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) }
            .filter { it.attributes.getNamedItem("translatable")?.nodeValue != "false" }
            .associate { it.attributes.getNamedItem("name").nodeValue to it.textContent }
    }

    private val strings = load()

    @Test
    fun noOtherLanguageFolderExists() {
        val languageFolder = Regex("^values-[a-z]{2,3}(-r[A-Z]{2})?$")
        val extra = resDir.listFiles()!!.map { it.name }.filter { languageFolder.matches(it) }
        assertTrue("language folders must not exist: $extra", extra.isEmpty())
        assertFalse("locales_config must not exist", File(resDir, "xml/locales_config.xml").exists())
    }

    @Test
    fun theManifestHasNoLanguageListAndNoNetworkAccess() {
        val manifest = File(mainDir, "AndroidManifest.xml").readText()
        assertFalse(manifest.contains("localeConfig"))
        assertFalse("the app must work offline and ask for no network permission", manifest.contains("android.permission.INTERNET"))
        assertFalse(manifest.contains("ACCESS_NETWORK_STATE"))
    }

    @Test
    fun noLayoutOrMenuHasHardCodedVisibleText() {
        // technical tokens that are the same in every language
        val allowed = setOf("DEG", "RAD", "45", "00", "000", "")
        val attribute = Regex("(?:android|app):(?:text|contentDescription|hint|title|summary)=\"([^@\"][^\"]*)\"")
        val offenders = mutableListOf<String>()
        resDir.walkTopDown().filter { it.extension == "xml" && (it.parentFile.name.startsWith("layout") || it.parentFile.name == "menu" || it.parentFile.name == "xml") }
            .forEach { file ->
                attribute.findAll(file.readText()).map { it.groupValues[1] }.filter { it !in allowed }
                    .forEach { offenders.add("${file.parentFile.name}/${file.name}: $it") }
            }
        assertTrue("texts that should be in strings.xml: $offenders", offenders.isEmpty())
    }

    private val englishMarkers = Regex(
        "\\b(the|and|your|you|with|from|this|that|are|not|for|please|settings|cancel|delete|clear history|history|theme|language|" +
            "calculator|scientific|financial|payment|present value|future value|interest|enter a|invalid|close|history|copy|paste)\\b",
        RegexOption.IGNORE_CASE
    )

    // words that are also valid Portuguese or technical names
    private val allowedMatches = setOf("para", "financial rpn")

    @Test
    fun noVisibleStringContainsEnglishWords() {
        val offenders = strings.filter { (name, text) ->
            val clean = text.replace("%[0-9]*\\$?[sd]".toRegex(), "")
            // OpenCalc and technical formula names are proper nouns, not English text
            val withoutNames = clean.replace("OpenCalc", "").replace("GPL-3.0", "").replace("Gson", "")
            val hit = englishMarkers.find(withoutNames)?.value?.lowercase()
            hit != null && hit !in allowedMatches && name !in knownAcceptable
        }.keys
        assertTrue("strings that still look English: $offenders", offenders.isEmpty())
    }

    // names whose Portuguese text legitimately contains one of the markers (e.g. "Material You")
    private val knownAcceptable = setOf<String>()

    @Test
    fun brazilianTerminologyFollowsTheSpecification() {
        val expected = mapOf(
            "mode_basic" to "Calculadora básica",
            "mode_scientific" to "Calculadora científica",
            "mode_financial" to "Calculadora financeira RPN",
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
            "fin_reg_mode_end" to "final do período",
            "fin_menu_amortization" to "Tabela de amortização"
        )
        for ((name, text) in expected) assertEquals(name, text, strings[name])
    }

    @Test
    fun everyStringUsedByTheFinancialPanelIsDefined() {
        val panel = File(mainDir, "java/com/darkempire78/opencalculator/financial/FinancialPanel.kt").readText()
        val used = Regex("R\\.string\\.([a-zA-Z0-9_]+)").findAll(panel).map { it.groupValues[1] }.toSet()
        val allNames = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(resDir, "values/strings.xml"))
            .getElementsByTagName("string").let { n -> (0 until n.length).map { n.item(it).attributes.getNamedItem("name").nodeValue }.toSet() }
        val missing = used.filter { it !in allNames }
        assertTrue("strings used by FinancialPanel but not defined: $missing", missing.isEmpty())
    }

    @Test
    fun noStringIsEmpty() {
        val empty = strings.filter { it.value.isBlank() }.keys
        assertTrue("empty strings: $empty", empty.isEmpty())
    }
}
