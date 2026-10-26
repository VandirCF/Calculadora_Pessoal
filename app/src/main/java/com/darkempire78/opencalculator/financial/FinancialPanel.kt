package com.darkempire78.opencalculator.financial

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.TextViewCompat
import com.darkempire78.opencalculator.R
import com.darkempire78.opencalculator.finance.Amortization
import com.darkempire78.opencalculator.finance.FinancialError
import com.darkempire78.opencalculator.finance.FinancialResult
import com.darkempire78.opencalculator.finance.FinancialWarning
import com.darkempire78.opencalculator.finance.Periodicity
import com.darkempire78.opencalculator.finance.RateConversion
import com.darkempire78.opencalculator.finance.RateConversions
import com.darkempire78.opencalculator.finance.RpnFormatter
import com.darkempire78.opencalculator.finance.rpn.RpnEngine
import com.darkempire78.opencalculator.finance.rpn.RpnEvent
import com.darkempire78.opencalculator.finance.rpn.RpnEventType
import com.darkempire78.opencalculator.finance.rpn.RpnKey
import com.darkempire78.opencalculator.history.HistoryType
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.math.BigDecimal
import java.math.RoundingMode

/** User display preferences for the financial panel (separators come from the app settings). */
data class FinancialDisplaySettings(
    val decimals: Int,
    val decimalSeparator: String,
    val groupingSeparator: String,
    val grouping: Boolean
)

/** A calculation to be recorded in the unified history. */
data class FinancialHistoryEvent(
    val type: HistoryType,
    val calculation: String,
    val result: String,
    val details: String?
)

/**
 * The financial RPN calculator screen, built in code so it adapts to portrait, landscape and
 * small screens with one definition. All arithmetic lives in [RpnEngine]; this class only maps
 * keys to engine calls and renders the engine state.
 */
class FinancialPanel(
    private val context: Context,
    private val viewModel: FinancialViewModel,
    private val settings: () -> FinancialDisplaySettings,
    private val onHistory: (FinancialHistoryEvent) -> Unit,
    private val onStateChanged: () -> Unit,
    private val onKeyFeedback: (View) -> Unit,
    private val onShowHistory: () -> Unit = {}
) {
    private val engine: RpnEngine get() = viewModel.engine

    // ---- key model -------------------------------------------------------------------------------
    private sealed class Action {
        data class Press(val key: RpnKey) : Action()
        data class Shift(val layer: ShiftLayer) : Action()
        object Store : Action()
        object Recall : Action()
        object ClearRegisters : Action()
    }

    private enum class Style { DIGIT, OPERATOR, FUNCTION, FINANCIAL, CLEAR, ENTER, SHIFT_F, SHIFT_G }

    /**
     * One key of the keypad: [main] is printed on the key, [fLabel] (gold) above it and [gLabel] (blue) on the key,
     * like the printed functions of a financial calculator.
     */
    private class KeySpec(
        val main: String,
        val mainAction: Action,
        val mainDescription: Int,
        val style: Style,
        val fLabel: String? = null,
        val fAction: Action? = null,
        val fDescription: Int? = null,
        val gLabel: String? = null,
        val gAction: Action? = null,
        val gDescription: Int? = null
    )

    /** Where a key sits in the grid; a null [spec] is the part of the grid covered by a taller key above. */
    private class Slot(val spec: KeySpec?, val colSpan: Int = 1, val rowSpan: Int = 1)

    private fun press(key: RpnKey) = Action.Press(key)

    private fun digit(d: Int, gLabel: String? = null, gAction: Action? = null, gDesc: Int? = null) =
        KeySpec(d.toString(), press(RpnKey.valueOf("D$d")), -1, Style.DIGIT, gLabel = gLabel, gAction = gAction, gDescription = gDesc)

    private val kN = KeySpec("n", press(RpnKey.N), R.string.fin_key_n, Style.FINANCIAL,
        fLabel = "AMORT", fAction = press(RpnKey.AMORT), fDescription = R.string.fin_key2_amort,
        gLabel = "12×", gAction = press(RpnKey.N_TIMES_PER_YEAR), gDescription = R.string.fin_key2_n)
    private val kI = KeySpec("i", press(RpnKey.I), R.string.fin_key_i, Style.FINANCIAL,
        gLabel = "12÷", gAction = press(RpnKey.I_DIV_PER_YEAR), gDescription = R.string.fin_key2_i)
    private val kPV = KeySpec("PV", press(RpnKey.PV), R.string.fin_key_pv, Style.FINANCIAL,
        fLabel = "NPV", fAction = press(RpnKey.NPV), fDescription = R.string.fin_key2_npv,
        gLabel = "CF0", gAction = press(RpnKey.CF0), gDescription = R.string.fin_key2_cf0)
    private val kPMT = KeySpec("PMT", press(RpnKey.PMT), R.string.fin_key_pmt, Style.FINANCIAL,
        gLabel = "CFj", gAction = press(RpnKey.CFJ), gDescription = R.string.fin_key2_cfj)
    private val kFV = KeySpec("FV", press(RpnKey.FV), R.string.fin_key_fv, Style.FINANCIAL,
        fLabel = "IRR", fAction = press(RpnKey.IRR), fDescription = R.string.fin_key2_irr,
        gLabel = "Nj", gAction = press(RpnKey.NJ), gDescription = R.string.fin_key2_nj)
    private val kChs = KeySpec("CHS", press(RpnKey.CHS), R.string.fin_key_chs, Style.FUNCTION)
    private val kPow = KeySpec("yˣ", press(RpnKey.POW), R.string.fin_key2_pow, Style.FUNCTION,
        gLabel = "√x", gAction = press(RpnKey.SQRT), gDescription = R.string.fin_key2_sqrt)
    private val kRecip = KeySpec("1/x", press(RpnKey.RECIPROCAL), R.string.fin_key2_recip, Style.FUNCTION)
    private val kPercentTotal = KeySpec("%T", press(RpnKey.PERCENT_TOTAL), R.string.fin_key_percent_total, Style.FUNCTION)
    private val kDelta = KeySpec("Δ%", press(RpnKey.DELTA_PERCENT), R.string.fin_key2_delta, Style.FUNCTION)
    private val kPercent = KeySpec("%", press(RpnKey.PERCENT), R.string.fin_key_percent, Style.FUNCTION)
    private val kSqrt = KeySpec("√x", press(RpnKey.SQRT), R.string.fin_key2_sqrt, Style.FUNCTION)
    private val kRollDown = KeySpec("R↓", press(RpnKey.ROLL_DOWN), R.string.fin_key_roll, Style.FUNCTION,
        fLabel = "CLR", fAction = press(RpnKey.CLEAR_STACK), fDescription = R.string.fin_key2_clrstack,
        gLabel = "R↑", gAction = press(RpnKey.ROLL_UP), gDescription = R.string.fin_key2_rollup)
    private val kSwap = KeySpec("x⇄y", press(RpnKey.SWAP), R.string.fin_key_swap, Style.FUNCTION)
    private val kClx = KeySpec("CLx", press(RpnKey.CLX), R.string.fin_key_clx, Style.CLEAR,
        fLabel = "CLR\u0192", fAction = Action.ClearRegisters, fDescription = R.string.fin_key2_clrfin)
    private val kBackspace = KeySpec("⌫", press(RpnKey.BACKSPACE), R.string.fin_key_backspace, Style.CLEAR)
    private val kEnter = KeySpec("ENTER", press(RpnKey.ENTER), R.string.fin_key_enter, Style.ENTER)
    private val kStore = KeySpec("STO", Action.Store, R.string.fin_key2_sto, Style.FUNCTION)
    private val kRecall = KeySpec("RCL", Action.Recall, R.string.fin_key2_rcl, Style.FUNCTION)
    private val kLastX = KeySpec("LSTx", press(RpnKey.LAST_X), R.string.fin_key2_lastx, Style.FUNCTION)
    private val kF = KeySpec("f", Action.Shift(ShiftLayer.F), R.string.fin_key_f, Style.SHIFT_F)
    private val kG = KeySpec("g", Action.Shift(ShiftLayer.G), R.string.fin_key_g, Style.SHIFT_G)
    private val kDiv = KeySpec("÷", press(RpnKey.DIV), R.string.fin_key_div, Style.OPERATOR)
    private val kMul = KeySpec("×", press(RpnKey.MUL), R.string.fin_key_mul, Style.OPERATOR)
    private val kSub = KeySpec("−", press(RpnKey.SUB), R.string.fin_key_sub, Style.OPERATOR)
    private val kAdd = KeySpec("+", press(RpnKey.ADD), R.string.fin_key_add, Style.OPERATOR)
    private val kDot = KeySpec(".", press(RpnKey.DOT), R.string.fin_key_dot, Style.DIGIT)

    private val kD7 = digit(7, "BEG", press(RpnKey.BEGIN), R.string.fin_key2_begin)
    private val kD8 = digit(8, "END", press(RpnKey.END), R.string.fin_key2_end)

    /** Landscape: 4 rows of 10 keys, with a tall ENTER (like the 10 x 4 layout of a financial calculator). */
    private val landscapeRows: List<List<Slot>> = listOf(
        listOf(kN, kI, kPV, kPMT, kFV, kChs, kD7, kD8, digit(9), kDiv).map { Slot(it) },
        listOf(kPow, kRecip, kPercentTotal, kDelta, kPercent, kSqrt, digit(4), digit(5), digit(6), kMul).map { Slot(it) },
        listOf(Slot(kRollDown), Slot(kSwap), Slot(kClx), Slot(kEnter, colSpan = 2, rowSpan = 2), Slot(kStore),
            Slot(digit(1)), Slot(digit(2)), Slot(digit(3)), Slot(kSub)),
        listOf(Slot(kF), Slot(kG), Slot(kBackspace), Slot(null, colSpan = 2), Slot(kRecall),
            Slot(digit(0)), Slot(kDot), Slot(kLastX), Slot(kAdd))
    )

    /** Portrait: the same keys in 8 rows of 5, with a wide ENTER at the bottom. */
    private val portraitRows: List<List<Slot>> = listOf(
        listOf(kN, kI, kPV, kPMT, kFV).map { Slot(it) },
        listOf(kChs, kD7, kD8, digit(9), kDiv).map { Slot(it) },
        listOf(kPow, digit(4), digit(5), digit(6), kMul).map { Slot(it) },
        listOf(kRecip, digit(1), digit(2), digit(3), kSub).map { Slot(it) },
        listOf(kPercentTotal, digit(0), kDot, kLastX, kAdd).map { Slot(it) },
        listOf(kDelta, kPercent, kSqrt, kRollDown, kSwap).map { Slot(it) },
        listOf(kF, kG, kStore, kRecall, kClx).map { Slot(it) },
        listOf(Slot(kBackspace), Slot(kEnter, colSpan = 4))
    )

    // ---- views -----------------------------------------------------------------------------------
    private val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    /** Text grows on tablets so the keypad and the display stay readable. */
    private val textScale = context.resources.configuration.smallestScreenWidthDp
        .let { if (it >= 720) 2.0f else if (it >= 600) 1.5f else 1f }
    private fun sp(value: Float) = value * textScale
    private fun spInt(value: Int) = (value * textScale).toInt()
    private val compactScreen = context.resources.configuration.screenHeightDp
        .let { it < COMPACT_HEIGHT_DP || (!isLandscape && it < COMPACT_PORTRAIT_HEIGHT_DP) }
    private val buttons = ArrayList<Pair<KeySpec, MaterialButton>>()
    private val fLabels = ArrayList<Pair<KeySpec, TextView>>()
    private lateinit var stackValues: List<TextView> // T, Z, Y, X
    private lateinit var beginBadge: TextView
    private lateinit var shiftBadge: TextView
    private lateinit var messageView: TextView
    private val chips = LinkedHashMap<RpnKey, TextView>()
    /** "PV - Valor presente": what the last technical key pressed means, so the notation is never a mystery. */
    private var lastHint: String? = null
    /** Text shown on a key: Portuguese labels for the English abbreviations, the decimal mark in use for ".". */
    private val labelResources = mapOf(
        "BEG" to R.string.fin_label_begin, "END" to R.string.fin_label_end,
        "STO" to R.string.fin_label_store, "RCL" to R.string.fin_label_recall, "LSTx" to R.string.fin_label_last_x,
        "CLR" to R.string.fin_label_clear_stack, "CLR\u0192" to R.string.fin_label_clear_registers
    )

    // NOTE: everything build() reads must be declared above this line (property initializers run in order)
    val root: View = build()

    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()

    private fun color(attr: Int): Int {
        val tv = TypedValue()
        context.theme.resolveAttribute(attr, tv, true)
        return tv.data
    }

    private fun roundedBackground(fill: Int, radiusDp: Int, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radiusDp).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun build(): View {
        val container = LinearLayout(context).apply {
            orientation = if (isLandscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(8))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val display = buildDisplay()
        val keypad = buildKeypad()
        if (isLandscape) {
            container.addView(display, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply { marginEnd = dp(8) })
            container.addView(keypad, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.3f))
        } else {
            container.addView(display, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, if (compactScreen) 1.15f else 0.85f).apply { bottomMargin = dp(6) })
            container.addView(keypad, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.6f))
        }
        return container
    }

    private fun badge(text: String): TextView = TextView(context).apply {
        this.text = text
        textSize = sp(11f)
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(color(R.attr.calc_text_primary))
        setPadding(dp(8), dp(2), dp(8), dp(2))
        background = roundedBackground(color(R.attr.calc_key_operator), 8, color(R.attr.calc_accent))
    }

    private fun buildDisplay(): View {
        val textColor = color(R.attr.calc_text_primary)
        val dim = color(R.attr.calc_text_secondary)
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBackground(color(R.attr.calc_display_background), 20)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        // header: RPN badge, mode badges and menu
        val header = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        val rpn = badge(context.getString(R.string.fin_badge_rpn))
        beginBadge = badge(context.getString(R.string.fin_badge_begin))
        shiftBadge = badge(context.getString(R.string.fin_badge_f))
        val spacer = View(context)
        val history = TextView(context).apply {
            text = context.getString(R.string.fin_history)
            textSize = sp(13f)
            gravity = Gravity.CENTER
            setTextColor(textColor)
            setPadding(dp(10), 0, dp(10), 0)
            minimumHeight = dp(48)
            contentDescription = context.getString(R.string.fin_history)
            setOnClickListener { onShowHistory() }
        }
        val menu = TextView(context).apply {
            text = "⋯"
            textSize = sp(24f)
            gravity = Gravity.CENTER
            setTextColor(textColor)
            minimumWidth = dp(48)
            minimumHeight = dp(48)
            contentDescription = context.getString(R.string.fin_menu_button)
            setOnClickListener { showMenu(it) }
        }
        header.addView(rpn)
        header.addView(beginBadge, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(6) })
        header.addView(shiftBadge, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(6) })
        header.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
        header.addView(history)
        header.addView(menu)
        box.addView(header)

        // stack: T, Z, Y small; X large
        val labels = listOf("T", "Z", "Y", "X")
        val descriptions = listOf(R.string.fin_stack_t, R.string.fin_stack_z, R.string.fin_stack_y, R.string.fin_stack_x)
        val values = ArrayList<TextView>()
        // Short screens (landscape phones): keep only Y and X so nothing is squeezed
        val compact = compactScreen
        for ((index, label) in labels.withIndex()) {
            val isX = index == 3
            val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
            val name = TextView(context).apply {
                text = label
                textSize = if (isX) sp(18f) else sp(13f)
                setTextColor(dim)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            val value = TextView(context).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setTextColor(if (isX) textColor else dim)
                setSingleLine()
                typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
                contentDescription = null
                ViewCompat.setAccessibilityLiveRegion(this, if (isX) ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE else ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE)
                if (isX) {
                    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, spInt(18), spInt(44), 2, TypedValue.COMPLEX_UNIT_SP)
                } else {
                    textSize = sp(15f)
                }
            }
            row.addView(name, LinearLayout.LayoutParams(dp(20), ViewGroup.LayoutParams.WRAP_CONTENT))
            row.addView(value, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
            val rowWeight = if (isX) 1.6f else 0.7f
            if (compact && index < 2) row.visibility = View.GONE
            box.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, rowWeight))
            value.tag = descriptions[index]
            values.add(value)
        }
        stackValues = values

        // registers strip
        val strip = LinearLayout(context).apply { gravity = Gravity.CENTER }
        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = true
            addView(strip)
        }
        for ((key, label) in listOf(RpnKey.N to "n", RpnKey.I to "i", RpnKey.PV to "PV", RpnKey.PMT to "PMT", RpnKey.FV to "FV")) {
            val chip = TextView(context).apply {
                tag = label
                TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, spInt(8), spInt(12), 1, TypedValue.COMPLEX_UNIT_SP)
                maxLines = 2
                gravity = Gravity.CENTER
                setTextColor(textColor)
                setPadding(dp(4), dp(2), dp(4), dp(2))
                background = roundedBackground(color(R.attr.calc_surface), 10, dim)
                minimumHeight = dp(40)
                isClickable = true
                setOnClickListener {
                    lastHint = label + " \u2014 " + context.getString(chipDescription(key))
                    runKey(rclKeyFor(key), it)
                }
            }
            chips[key] = chip
            strip.addView(chip, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(3) })
        }
        box.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })

        // contextual help / error line
        messageView = TextView(context).apply {
            textSize = sp(12f)
            setTextColor(dim)
            ViewCompat.setAccessibilityLiveRegion(this, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
            minLines = if (compactScreen) 1 else 2
        }
        box.addView(messageView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })
        return box
    }

    private fun chipDescription(key: RpnKey) = when (key) {
        RpnKey.N -> R.string.fin_key_n
        RpnKey.I -> R.string.fin_key_i
        RpnKey.PV -> R.string.fin_key_pv
        RpnKey.PMT -> R.string.fin_key_pmt
        else -> R.string.fin_key_fv
    }

    private fun rclKeyFor(key: RpnKey) = when (key) {
        RpnKey.N -> RpnKey.RCL_N
        RpnKey.I -> RpnKey.RCL_I
        RpnKey.PV -> RpnKey.RCL_PV
        RpnKey.PMT -> RpnKey.RCL_PMT
        else -> RpnKey.RCL_FV
    }

    private fun buildKeypad(): View {
        val grid = android.widget.GridLayout(context)
        val rows = if (isLandscape) landscapeRows else portraitRows
        grid.rowCount = rows.size
        grid.columnCount = rows.first().sumOf { it.colSpan }
        for ((rowIndex, row) in rows.withIndex()) {
            var column = 0
            for (slot in row) {
                val spec = slot.spec
                if (spec != null) {
                    val params = android.widget.GridLayout.LayoutParams(
                        android.widget.GridLayout.spec(rowIndex, slot.rowSpan, 1f),
                        android.widget.GridLayout.spec(column, slot.colSpan, 1f)
                    ).apply {
                        width = 0
                        height = 0
                        setMargins(dp(2), dp(1), dp(2), dp(1))
                    }
                    grid.addView(buildCell(spec), params)
                }
                column += slot.colSpan
            }
        }
        return grid
    }

    /** A key with its gold f function printed above it. */
    private fun buildCell(spec: KeySpec): View {
        val button = buildButton(spec)
        buttons.add(spec to button)
        if (spec.style == Style.ENTER) return button
        val cell = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val fText = TextView(context).apply {
            tag = "f:" + spec.main
            textSize = sp(if (compactScreen) 8f else 9.5f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(color(R.attr.calc_shift_f))
            maxLines = 1
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            text = spec.fLabel?.let(::shown) ?: "\u00A0"
        }
        fLabels.add(spec to fText)
        cell.addView(fText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        cell.addView(button, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return cell
    }

    private fun buildButton(spec: KeySpec) = MaterialButton(context).apply {
        isAllCaps = false
        insetTop = 0
        insetBottom = 0
        minWidth = 0
        minHeight = 0
        minimumHeight = 0
        setPadding(0, 0, 0, 0)
        cornerRadius = dp(12)
        backgroundTintList = ColorStateList.valueOf(buttonColor(spec.style))
        if (spec.style == Style.FUNCTION || spec.style == Style.FINANCIAL) {
            // outlined, so the key is visible on any theme background
            strokeWidth = dp(1)
            strokeColor = ColorStateList.valueOf(color(if (spec.style == Style.FINANCIAL) R.attr.calc_accent else R.attr.calc_outline))
        }
        setTextColor(textColorFor(spec.style))
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, spInt(9), spInt(if (spec.style == Style.ENTER) 20 else 18), 1, TypedValue.COMPLEX_UNIT_SP)
        maxLines = if (spec.gLabel != null) 2 else 1
        tag = "key:" + spec.main // stable handle for UI tests, independent of the language
        TooltipCompat.setTooltipText(this, listOfNotNull(describe(spec, ShiftLayer.NONE), spec.fDescription?.let { "f: " + context.getString(it) }, spec.gDescription?.let { "g: " + context.getString(it) }).joinToString(" / "))
        setOnClickListener { onKey(spec, it) }
    }

    private fun buttonColor(style: Style) = when (style) {
        Style.DIGIT -> color(R.attr.calc_key_number)
        Style.OPERATOR -> color(R.attr.calc_key_operator)
        Style.FUNCTION -> color(R.attr.calc_key_function)
        Style.FINANCIAL -> color(R.attr.calc_key_financial)
        Style.CLEAR -> color(R.attr.calc_key_danger)
        Style.ENTER -> color(R.attr.calc_key_enter)
        Style.SHIFT_F -> color(R.attr.calc_shift_f)
        Style.SHIFT_G -> color(R.attr.calc_shift_g)
    }

    private fun textColorFor(style: Style) = when (style) {
        Style.ENTER -> color(R.attr.calc_on_key_enter)
        Style.SHIFT_F, Style.SHIFT_G -> color(R.attr.calc_background)
        else -> color(R.attr.calc_text_primary)
    }

    // ---- key handling ---------------------------------------------------------------------------------
    private fun onKey(spec: KeySpec, view: View) {
        val layer = viewModel.shift
        val action: Action? = when (layer) {
            ShiftLayer.NONE -> spec.mainAction
            ShiftLayer.F -> spec.fAction ?: if (spec.mainAction is Action.Shift) spec.mainAction else null
            ShiftLayer.G -> spec.gAction ?: if (spec.mainAction is Action.Shift) spec.mainAction else null
        }
        viewModel.shift = ShiftLayer.NONE
        if (action == null) {
            // the key has no function on the active layer: the shift is just dropped
            lastHint = null
            onKeyFeedback(view)
            render()
            return
        }
        val label = when (layer) {
            ShiftLayer.F -> spec.fLabel
            ShiftLayer.G -> spec.gLabel
            ShiftLayer.NONE -> null
        } ?: spec.main
        lastHint = if (action is Action.Press && !label[0].isDigit() && label != "ENTER") "${shown(label)} \u2014 ${describe(spec, layer)}" else null
        when (action) {
            is Action.Shift -> {
                // pressing the same shift key again turns it off
                viewModel.shift = if (layer == action.layer) ShiftLayer.NONE else action.layer
                viewModel.pendingMemory = PendingMemory.NONE
                onKeyFeedback(view)
                render()
            }
            is Action.Store -> {
                viewModel.pendingMemory = PendingMemory.STORE
                onKeyFeedback(view)
                render()
            }
            is Action.Recall -> {
                viewModel.pendingMemory = PendingMemory.RECALL
                onKeyFeedback(view)
                render()
            }
            is Action.ClearRegisters -> {
                onKeyFeedback(view)
                confirmClearRegisters()
                render()
            }
            is Action.Press -> runKey(action.key, view)
        }
    }

    private fun runKey(key: RpnKey, view: View?) {
        view?.let(onKeyFeedback)
        val pending = viewModel.pendingMemory
        viewModel.pendingMemory = PendingMemory.NONE
        val digitValue = RpnKey.entries.indexOf(key) - RpnKey.D0.ordinal
        if (pending != PendingMemory.NONE && digitValue in 0..9) {
            if (pending == PendingMemory.STORE) engine.store(digitValue) else engine.recall(digitValue)
        } else {
            engine.press(key)
        }
        afterKey()
    }

    private fun afterKey() {
        engine.lastEvent?.let { onHistory(toHistory(it)) }
        onStateChanged()
        render()
    }

    // ---- rendering -------------------------------------------------------------------------------------
    fun render() {
        val s = settings()
        fun fixed(v: BigDecimal) = RpnFormatter.format(v, s.decimals, s.decimalSeparator, s.groupingSeparator, s.grouping)
        val e = engine
        stackValues[0].text = fixed(e.t)
        stackValues[1].text = fixed(e.z)
        stackValues[2].text = fixed(e.y)
        stackValues[3].text = e.entry?.let { RpnFormatter.formatEntry(it, s.decimalSeparator, s.groupingSeparator, s.grouping) } ?: fixed(e.x)
        for (v in stackValues) {
            val res = v.tag as Int
            v.contentDescription = context.getString(res) + ": " + v.text
        }

        beginBadge.visibility = if (e.tvm.beginMode) View.VISIBLE else View.GONE
        val layer = viewModel.shift
        shiftBadge.visibility = if (layer == ShiftLayer.NONE) View.GONE else View.VISIBLE
        if (layer != ShiftLayer.NONE) {
            val isF = layer == ShiftLayer.F
            shiftBadge.text = context.getString(if (isF) R.string.fin_badge_f else R.string.fin_badge_g)
            shiftBadge.setTextColor(color(R.attr.calc_background))
            shiftBadge.background = roundedBackground(color(if (isF) R.attr.calc_shift_f else R.attr.calc_shift_g), 8)
        }

        // registers: n and i are shown with their natural digits, money values with fixed decimals
        for ((key, chip) in chips) {
            val label = chip.tag as String
            val value = when (key) {
                RpnKey.N -> e.tvm.n
                RpnKey.I -> e.tvm.i
                RpnKey.PV -> e.tvm.pv
                RpnKey.PMT -> e.tvm.pmt
                else -> e.tvm.fv
            }
            val shown = value?.let { if (key == RpnKey.N || key == RpnKey.I) natural(it, s) else fixed(it) }
            chip.text = if (shown == null) label else "$label\n$shown"
            chip.alpha = if (shown == null) 0.55f else 1f
            chip.contentDescription = if (shown == null) context.getString(R.string.fin_reg_chip_unset, label)
            else context.getString(R.string.fin_reg_chip_set, label, shown)
        }

        // with f or g active, keys that have no function on that layer fade, so the usable ones stand out
        for ((spec, button) in buttons) {
            button.text = keyLabel(spec)
            button.contentDescription = describe(spec, layer)
            val usable = when (layer) {
                ShiftLayer.NONE -> true
                ShiftLayer.F -> spec.fAction != null || spec.mainAction is Action.Shift
                ShiftLayer.G -> spec.gAction != null || spec.mainAction is Action.Shift
            }
            button.alpha = if (usable) 1f else 0.4f
            if (spec.style == Style.SHIFT_F || spec.style == Style.SHIFT_G) {
                val active = (spec.style == Style.SHIFT_F && layer == ShiftLayer.F) || (spec.style == Style.SHIFT_G && layer == ShiftLayer.G)
                button.strokeWidth = if (active) dp(3) else 0
                button.strokeColor = ColorStateList.valueOf(color(R.attr.calc_text_primary))
            }
        }
        for ((spec, label) in fLabels) {
            val highlighted = layer == ShiftLayer.F && spec.fAction != null
            label.alpha = if (layer == ShiftLayer.G || (layer == ShiftLayer.F && !highlighted)) 0.4f else 1f
            label.scaleX = if (highlighted) 1.2f else 1f
            label.scaleY = if (highlighted) 1.2f else 1f
        }
        renderMessage()
    }

    private fun natural(v: BigDecimal, s: FinancialDisplaySettings): String {
        val trimmed = v.setScale(6, RoundingMode.HALF_EVEN).stripTrailingZeros()
        val scale = if (trimmed.scale() < 0) 0 else trimmed.scale()
        return RpnFormatter.format(trimmed, scale, s.decimalSeparator, s.groupingSeparator, s.grouping)
    }

    private fun shown(token: String): String =
        if (token == ".") settings().decimalSeparator else labelResources[token]?.let { context.getString(it) } ?: token

    /** The main label, with the blue g function in small type below it. */
    private fun keyLabel(spec: KeySpec): CharSequence {
        val g = spec.gLabel?.let(::shown) ?: return shown(spec.main)
        val text = SpannableStringBuilder()
        text.append(shown(spec.main))
        text.append("\n")
        val start = text.length
        text.append(g, RelativeSizeSpan(0.62f), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(ForegroundColorSpan(color(if (spec.style == Style.ENTER) R.attr.calc_on_key_enter else R.attr.calc_shift_g)), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return text
    }

    private fun describe(spec: KeySpec, layer: ShiftLayer): String {
        val res = when (layer) {
            ShiftLayer.F -> spec.fDescription
            ShiftLayer.G -> spec.gDescription
            ShiftLayer.NONE -> null
        } ?: spec.mainDescription
        return if (res > 0) context.getString(res) else shown(spec.main)
    }

    private fun renderMessage() {
        val e = engine
        val error = e.error
        val text: CharSequence
        var isError = false
        when {
            error != null -> {
                text = errorText(error, e.errorDetail, e.errorCandidates)
                isError = true
            }
            e.warnings.contains(FinancialWarning.MULTIPLE_SIGN_CHANGES) -> text = context.getString(R.string.fin_warn_multiple_sign_changes)
            viewModel.pendingMemory == PendingMemory.STORE -> text = context.getString(R.string.fin_help_sto)
            viewModel.pendingMemory == PendingMemory.RECALL -> text = context.getString(R.string.fin_help_rcl)
            viewModel.shift == ShiftLayer.F -> text = context.getString(R.string.fin_help_f)
            viewModel.shift == ShiftLayer.G -> text = context.getString(R.string.fin_help_g)
            lastHint != null -> text = lastHint!!
            e.tvm.pv == null && e.tvm.pmt == null && e.tvm.n == null && e.tvm.i == null && e.tvm.fv == null -> text = context.getString(R.string.fin_help_default)
            else -> text = context.getString(R.string.fin_help_tvm)
        }
        messageView.text = text
        messageView.setTextColor(if (isError) color(R.attr.calc_error) else color(R.attr.calc_text_secondary))
    }

    /**
     * Message for the user. The detail text produced by the domain is a developer message (English) and is
     * never shown, except the name of a missing register; several solutions are listed as formatted numbers.
     */
    private fun errorText(error: FinancialError, detail: String, candidates: List<BigDecimal> = emptyList()): String = when (error) {
        FinancialError.INVALID_INPUT -> context.getString(R.string.fin_err_invalid_input)
        FinancialError.DIVISION_BY_ZERO -> context.getString(R.string.fin_err_division_by_zero)
        FinancialError.OUT_OF_RANGE -> context.getString(R.string.fin_err_out_of_range)
        FinancialError.OVERFLOW -> context.getString(R.string.fin_err_overflow)
        FinancialError.INCONSISTENT_SIGNS -> context.getString(R.string.fin_err_inconsistent_signs)
        FinancialError.NO_SOLUTION -> context.getString(R.string.fin_err_no_solution)
        FinancialError.NO_CONVERGENCE -> context.getString(R.string.fin_err_no_convergence)
        FinancialError.MULTIPLE_SOLUTIONS -> context.getString(R.string.fin_err_multiple_solutions, candidates.joinToString("; ") { natural(it, settings()) + " %" })
        FinancialError.EMPTY_CASH_FLOWS -> context.getString(R.string.fin_err_empty_cash_flows)
        FinancialError.NO_SIGN_CHANGE -> context.getString(R.string.fin_err_no_sign_change)
        FinancialError.MISSING_REGISTER -> context.getString(R.string.fin_err_missing_register, detail)
    }

    /** Pushes a value (e.g. reused from the history) into X. */
    fun pushValue(value: BigDecimal) {
        engine.enterValue(value)
        onStateChanged()
        render()
    }

    // ---- history ----------------------------------------------------------------------------------------
    private fun toHistory(event: RpnEvent): FinancialHistoryEvent {
        val s = settings()
        fun f(v: BigDecimal) = RpnFormatter.format(v, s.decimals, s.decimalSeparator, s.groupingSeparator, s.grouping)
        fun n(v: BigDecimal) = natural(v, s)
        val mode = context.getString(if (event.beginMode) R.string.fin_reg_mode_begin else R.string.fin_reg_mode_end)
        return when (event.type) {
            RpnEventType.OPERATION -> {
                val symbol = when (event.label) { "x" -> "×"; "/" -> "÷"; "-" -> "−"; else -> event.label }
                val values = event.inputs.associate { it.first to it.second }
                val calc = when (event.label) {
                    "sqrt" -> "√${n(values.getValue("X"))}"
                    "1/x" -> "1/${n(values.getValue("X"))}"
                    "%" -> "${n(values.getValue("X"))}% × ${n(values.getValue("Y"))}"
                    "%T" -> "%T ${n(values.getValue("X"))} / ${n(values.getValue("Y"))}"
                    "delta%" -> "Δ% ${n(values.getValue("Y"))} → ${n(values.getValue("X"))}"
                    else -> "${n(values.getValue("Y"))} $symbol ${n(values.getValue("X"))}"
                }
                FinancialHistoryEvent(HistoryType.RPN_OPERATION, calc, n(event.result), null)
            }
            RpnEventType.TVM -> {
                val args = event.inputs.joinToString(", ") { (name, v) -> "$name=" + if (name == "n" || name == "i") n(v) else f(v) }
                FinancialHistoryEvent(HistoryType.TVM, "${event.label}: $args", if (event.label == "n" || event.label == "i") n(event.result) else f(event.result), mode)
            }
            RpnEventType.NPV, RpnEventType.IRR -> {
                val args = event.inputs.joinToString("; ") { (name, v) -> if (name == "i") "i=${n(v)}" else "$name=${f(v)}" }
                val type = if (event.type == RpnEventType.NPV) HistoryType.NPV else HistoryType.IRR
                FinancialHistoryEvent(type, "${event.label}: $args", if (event.type == RpnEventType.NPV) f(event.result) else n(event.result) + "%", null)
            }
            RpnEventType.AMORTIZATION -> {
                val values = event.inputs.associate { it.first to it.second }
                val calc = "AMORT ${values.getValue("periods").toPlainString()} (${values.getValue("from").toPlainString()}+): PV=${f(values.getValue("PV"))}, PMT=${f(values.getValue("PMT"))}, i=${n(values.getValue("i"))}"
                val result = context.getString(R.string.fin_hist_amort_result, f(event.result), f(event.extra[0].second), f(event.extra[1].second))
                FinancialHistoryEvent(HistoryType.AMORTIZATION, calc, result, mode)
            }
        }
    }

    // ---- menu and dialogs -----------------------------------------------------------------------------
    private fun showMenu(anchor: View) {
        val popup = PopupMenu(context, anchor)
        val items = listOf(
            R.string.fin_menu_registers, R.string.fin_menu_rates, R.string.fin_menu_amortization,
            R.string.fin_menu_ppy, R.string.fin_menu_clear_registers, R.string.fin_menu_clear_all, R.string.fin_menu_help
        )
        items.forEachIndexed { index, res -> popup.menu.add(0, index, index, res) }
        popup.setOnMenuItemClickListener { item ->
            when (items[item.itemId]) {
                R.string.fin_menu_registers -> showRegisters()
                R.string.fin_menu_rates -> showRateConversion()
                R.string.fin_menu_amortization -> showAmortization()
                R.string.fin_menu_amortization -> showAmortization()
                R.string.fin_menu_ppy -> showPaymentsPerYear()
                R.string.fin_menu_clear_registers -> confirmClearRegisters()
                R.string.fin_menu_clear_all -> confirmClearAll()
                R.string.fin_menu_help -> showHelp()
            }
            true
        }
        popup.show()
    }

    private fun dialogColumn(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(24), dp(8), dp(24), dp(0))
    }

    private fun showHelp() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.fin_help_title)
            .setMessage(R.string.fin_help_body)
            .setPositiveButton(R.string.fin_close, null)
            .show()
    }

    private fun confirmClearRegisters() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.fin_confirm_clear_title)
            .setMessage(R.string.fin_confirm_clear_message)
            .setNegativeButton(R.string.fin_cancel, null)
            .setPositiveButton(R.string.fin_clear) { _, _ ->
                engine.press(RpnKey.CLEAR_FINANCIAL)
                afterKey()
            }
            .show()
    }

    private fun confirmClearAll() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.fin_confirm_clear_all_title)
            .setMessage(R.string.fin_confirm_clear_all_message)
            .setNegativeButton(R.string.fin_cancel, null)
            .setPositiveButton(R.string.fin_clear) { _, _ ->
                engine.press(RpnKey.CLEAR_ALL)
                afterKey()
            }
            .show()
    }

    private fun showRegisters() {
        val s = settings()
        fun fixed(v: BigDecimal?) = v?.let { RpnFormatter.format(it, s.decimals, s.decimalSeparator, s.groupingSeparator, s.grouping) } ?: context.getString(R.string.fin_reg_not_set)
        fun nat(v: BigDecimal?) = v?.let { natural(it, s) } ?: context.getString(R.string.fin_reg_not_set)
        val e = engine
        val sb = StringBuilder()
        sb.append("n: ").append(nat(e.tvm.n)).append('\n')
        sb.append("i: ").append(nat(e.tvm.i)).append(" %\n")
        sb.append("PV: ").append(fixed(e.tvm.pv)).append('\n')
        sb.append("PMT: ").append(fixed(e.tvm.pmt)).append('\n')
        sb.append("FV: ").append(fixed(e.tvm.fv)).append("\n\n")
        sb.append(context.getString(R.string.fin_reg_mode)).append(": ")
            .append(context.getString(if (e.tvm.beginMode) R.string.fin_reg_mode_begin else R.string.fin_reg_mode_end)).append('\n')
        sb.append(context.getString(R.string.fin_reg_ppy)).append(": ").append(natural(e.periodsPerYear, s)).append("\n\n")
        sb.append(context.getString(R.string.fin_reg_flows)).append(":\n")
        val flows = e.cashFlows
        sb.append("CF0 = ").append(fixed(flows.cf0)).append('\n')
        flows.flows.forEachIndexed { index, f -> sb.append("CF").append(index + 1).append(" = ").append(fixed(f.amount)).append(" × ").append(f.count).append('\n') }
        sb.append('\n').append(context.getString(R.string.fin_reg_memory)).append(":\n")
        var anyMemory = false
        e.memory.forEachIndexed { index, v ->
            if (v.signum() != 0) {
                anyMemory = true
                sb.append("R").append(index).append(" = ").append(natural(v, s)).append('\n')
            }
        }
        if (!anyMemory) sb.append(context.getString(R.string.fin_reg_none)).append('\n')
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.fin_registers_title)
            .setMessage(sb.toString())
            .setNeutralButton(R.string.fin_menu_clear_registers) { _, _ -> confirmClearRegisters() }
            .setPositiveButton(R.string.fin_close, null)
            .show()
    }

    private fun showPaymentsPerYear() {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(engine.periodsPerYear.toBigInteger().toString())
            contentDescription = context.getString(R.string.fin_ppy_title)
        }
        val column = dialogColumn().apply {
            addView(TextView(context).apply { text = context.getString(R.string.fin_ppy_hint); textSize = 13f })
            addView(input)
        }
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.fin_ppy_title)
            .setView(column)
            .setNegativeButton(R.string.fin_cancel, null)
            .setPositiveButton(R.string.fin_ok, null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val value = input.text.toString().toIntOrNull()
            if (value == null || value !in 1..365) {
                input.error = context.getString(R.string.fin_ppy_invalid)
            } else {
                engine.periodsPerYear = BigDecimal(value)
                onStateChanged()
                render()
                dialog.dismiss()
            }
        }
    }

    private fun periodicityName(p: Periodicity): String = context.getString(
        when (p) {
            Periodicity.DAILY -> R.string.fin_per_daily
            Periodicity.WEEKLY -> R.string.fin_per_weekly
            Periodicity.BIWEEKLY -> R.string.fin_per_biweekly
            Periodicity.MONTHLY -> R.string.fin_per_monthly
            Periodicity.BIMONTHLY -> R.string.fin_per_bimonthly
            Periodicity.QUARTERLY -> R.string.fin_per_quarterly
            Periodicity.SEMIANNUAL -> R.string.fin_per_semiannual
            Periodicity.ANNUAL -> R.string.fin_per_annual
        }
    )

    private fun showRateConversion() {
        val s = settings()
        val rateInput = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            hint = context.getString(R.string.fin_rates_rate)
            contentDescription = context.getString(R.string.fin_rates_rate)
        }
        val types = listOf(R.string.fin_rates_type_nominal_to_effective, R.string.fin_rates_type_effective_to_nominal, R.string.fin_rates_type_equivalent)
        val periodicities = Periodicity.entries
        val periodNames = periodicities.map { periodicityName(it) }
        fun spinner(items: List<String>, selected: Int, description: String) = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items)
            setSelection(selected)
            contentDescription = description
        }
        val typeSpinner = spinner(types.map { context.getString(it) }, 0, context.getString(R.string.fin_menu_rates))
        val fromLabel = TextView(context).apply { text = context.getString(R.string.fin_rates_from) }
        val fromSpinner = spinner(periodNames, periodicities.indexOf(Periodicity.MONTHLY), context.getString(R.string.fin_rates_from))
        val toLabel = TextView(context).apply { text = context.getString(R.string.fin_rates_to) }
        val toSpinner = spinner(periodNames, periodicities.indexOf(Periodicity.ANNUAL), context.getString(R.string.fin_rates_to))
        val result = TextView(context).apply {
            textSize = 14f
            setPadding(0, dp(12), 0, 0)
            ViewCompat.setAccessibilityLiveRegion(this, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
        }
        fun updateVisibility() {
            val type = typeSpinner.selectedItemPosition
            val showFrom = type != 1
            val showTo = type != 0
            fromLabel.visibility = if (showFrom) View.VISIBLE else View.GONE
            fromSpinner.visibility = if (showFrom) View.VISIBLE else View.GONE
            toLabel.visibility = if (showTo) View.VISIBLE else View.GONE
            toSpinner.visibility = if (showTo) View.VISIBLE else View.GONE
        }
        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = updateVisibility()
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        updateVisibility()
        val column = dialogColumn().apply {
            addView(typeSpinner)
            addView(rateInput)
            addView(fromLabel); addView(fromSpinner)
            addView(toLabel); addView(toSpinner)
            addView(result)
        }
        val scroll = ScrollView(context).apply { addView(column) }
        var lastResult: RateConversion? = null
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.fin_rates_title)
            .setView(scroll)
            .setNegativeButton(R.string.fin_close, null)
            .setNeutralButton(R.string.fin_rates_send, null)
            .setPositiveButton(R.string.fin_rates_convert, null)
            .show()
        fun kindName(k: com.darkempire78.opencalculator.finance.RateKind) =
            context.getString(if (k == com.darkempire78.opencalculator.finance.RateKind.NOMINAL) R.string.fin_kind_nominal else R.string.fin_kind_effective)
        fun methodName(m: RateConversion.Method) = context.getString(
            when (m) {
                RateConversion.Method.NOMINAL_TO_EFFECTIVE -> R.string.fin_method_nominal_to_effective
                RateConversion.Method.EFFECTIVE_TO_NOMINAL -> R.string.fin_method_effective_to_nominal
                RateConversion.Method.COMPOUND_EQUIVALENT -> R.string.fin_method_compound
            }
        )
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val rate = rateInput.text.toString().replace(s.decimalSeparator, ".").toBigDecimalOrNull()
            if (rate == null) {
                rateInput.error = context.getString(R.string.fin_rates_invalid)
                return@setOnClickListener
            }
            val from = periodicities[fromSpinner.selectedItemPosition]
            val to = periodicities[toSpinner.selectedItemPosition]
            val outcome = when (typeSpinner.selectedItemPosition) {
                0 -> RateConversions.nominalToEffectiveAnnual(rate, from)
                1 -> RateConversions.effectiveAnnualToNominal(rate, to)
                else -> RateConversions.equivalent(rate, from, to)
            }
            when (outcome) {
                is FinancialResult.Success -> {
                    val c = outcome.value
                    lastResult = c
                    fun pct(v: BigDecimal) = natural(v.setScale(10, RoundingMode.HALF_EVEN), s)
                    result.text = context.getString(
                        R.string.fin_rates_result,
                        pct(c.sourceRatePercent), kindName(c.sourceKind), periodicityName(c.sourcePeriodicity),
                        pct(c.resultRatePercent), kindName(c.targetKind), periodicityName(c.targetPeriodicity),
                        methodName(c.method)
                    )
                    onHistory(
                        FinancialHistoryEvent(
                            HistoryType.RATE_CONVERSION,
                            "${pct(c.sourceRatePercent)}% ${kindName(c.sourceKind)}, ${periodicityName(c.sourcePeriodicity)} → ${kindName(c.targetKind)}, ${periodicityName(c.targetPeriodicity)}",
                            pct(c.resultRatePercent) + "%",
                            methodName(c.method)
                        )
                    )
                }
                is FinancialResult.Failure -> {
                    lastResult = null
                    result.text = errorText(outcome.error, outcome.detail, outcome.candidates)
                }
            }
        }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            lastResult?.let {
                engine.enterValue(it.resultRatePercent)
                onStateChanged()
                render()
                dialog.dismiss()
            }
        }
    }

    private fun showAmortization() {
        val s = settings()
        val e = engine
        val fromInput = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = context.getString(R.string.fin_amort_from)
            setText("1")
            contentDescription = context.getString(R.string.fin_amort_from)
        }
        val toInput = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = context.getString(R.string.fin_amort_to)
            e.tvm.n?.let { setText(it.setScale(0, RoundingMode.DOWN).toPlainString()) }
            contentDescription = context.getString(R.string.fin_amort_to)
        }
        val table = TextView(context).apply {
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setPadding(0, dp(12), 0, 0)
            setTextIsSelectable(true)
        }
        val column = dialogColumn().apply {
            addView(fromInput); addView(toInput); addView(table)
        }
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.fin_amort_title)
            .setView(ScrollView(context).apply { addView(column) })
            .setNegativeButton(R.string.fin_close, null)
            .setPositiveButton(R.string.fin_amort_show, null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val pv = e.tvm.pv
            val pmt = e.tvm.pmt
            val rate = e.tvm.i
            val from = fromInput.text.toString().toIntOrNull()
            val to = toInput.text.toString().toIntOrNull()
            if (pv == null || pmt == null || rate == null) {
                table.text = context.getString(R.string.fin_amort_need_registers)
                return@setOnClickListener
            }
            if (from == null || to == null) {
                table.text = context.getString(R.string.fin_amort_invalid)
                return@setOnClickListener
            }
            when (val outcome = Amortization.schedule(pv.abs(), pmt.abs(), rate, from, to, e.tvm.beginMode)) {
                is FinancialResult.Failure -> table.text = errorText(outcome.error, outcome.detail, outcome.candidates)
                is FinancialResult.Success -> {
                    val summary = outcome.value
                    fun money(v: BigDecimal) = RpnFormatter.format(v, s.decimals, s.decimalSeparator, s.groupingSeparator, s.grouping)
                    val sb = StringBuilder(context.getString(R.string.fin_amort_header)).append('\n')
                    for (row in summary.rows.take(MAX_TABLE_ROWS)) {
                        sb.append(row.period).append(" | ").append(money(row.interest)).append(" | ")
                            .append(money(row.principal)).append(" | ").append(money(row.balance)).append('\n')
                    }
                    if (summary.rows.size > MAX_TABLE_ROWS) sb.append("...\n")
                    sb.append('\n').append(context.getString(R.string.fin_amort_totals, money(summary.totalInterest), money(summary.totalPrincipal), money(summary.balance)))
                    table.text = sb.toString()
                    val mode = context.getString(if (e.tvm.beginMode) R.string.fin_reg_mode_begin else R.string.fin_reg_mode_end)
                    onHistory(
                        FinancialHistoryEvent(
                            HistoryType.AMORTIZATION,
                            "PV=${money(pv)}, PMT=${money(pmt)}, i=${natural(rate, s)}%, $from..$to",
                            context.getString(R.string.fin_hist_amort_result, money(summary.totalInterest), money(summary.totalPrincipal), money(summary.balance)),
                            mode
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val MAX_TABLE_ROWS = 120
        private const val COMPACT_HEIGHT_DP = 400
        private const val COMPACT_PORTRAIT_HEIGHT_DP = 700
    }
}
