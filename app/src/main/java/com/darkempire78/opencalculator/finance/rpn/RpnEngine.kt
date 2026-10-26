package com.darkempire78.opencalculator.finance.rpn

import com.darkempire78.opencalculator.finance.Amortization
import com.darkempire78.opencalculator.finance.BigMath
import com.darkempire78.opencalculator.finance.CashFlow
import com.darkempire78.opencalculator.finance.CashFlows
import com.darkempire78.opencalculator.finance.FinancialError
import com.darkempire78.opencalculator.finance.FinancialResult
import com.darkempire78.opencalculator.finance.Irr
import com.darkempire78.opencalculator.finance.MC
import com.darkempire78.opencalculator.finance.Npv
import com.darkempire78.opencalculator.finance.Tvm
import com.darkempire78.opencalculator.finance.TvmRegisters
import com.darkempire78.opencalculator.finance.WORK_MC
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/** Keys of the RPN financial calculator. UI buttons map one-to-one to these. */
enum class RpnKey {
    D0, D1, D2, D3, D4, D5, D6, D7, D8, D9, DOT, CHS, BACKSPACE,
    ENTER, ADD, SUB, MUL, DIV, POW, RECIPROCAL, SQRT, PERCENT, DELTA_PERCENT,
    SWAP, ROLL_DOWN, ROLL_UP, LAST_X, CLX, CLEAR_STACK,
    N, I, PV, PMT, FV, N_TIMES_PER_YEAR, I_DIV_PER_YEAR, BEGIN, END, CLEAR_FINANCIAL, CLEAR_ALL,
    RCL_N, RCL_I, RCL_PV, RCL_PMT, RCL_FV,
    CF0, CFJ, NJ, NPV, IRR, AMORT
}

enum class RpnEventType { OPERATION, TVM, NPV, IRR, AMORTIZATION }

/**
 * Describes the last successful computation so the UI can record it in the history.
 * [label] is the operator symbol or register name; amounts are full precision values.
 */
data class RpnEvent(
    val type: RpnEventType,
    val label: String,
    val inputs: List<Pair<String, BigDecimal>>,
    val result: BigDecimal,
    val extra: List<Pair<String, BigDecimal>> = emptyList(),
    val beginMode: Boolean = false
)

/**
 * RPN (reverse Polish notation) engine with a four level stack X, Y, Z, T.
 *
 * Rules (documented in docs/RPN_GUIDE.md):
 * - Digits build a number in X. If stack lift is enabled when a new number starts, the stack
 *   rises first (T<-Z, Z<-Y, Y<-X, old T is lost); otherwise X is overwritten.
 * - ENTER copies X into Y (stack rises) and disables stack lift, so the next number replaces X.
 * - Binary operations compute Y op X, store LAST X, put the result in X, drop the stack
 *   (Y<-Z, Z<-T, T unchanged) and enable stack lift.
 * - CLX clears X and disables stack lift. CLEAR_STACK clears X, Y, Z, T (not registers).
 * - After an error the stack and registers are unchanged, [error] is set, and the next key
 *   clears the error and executes normally.
 * - PERCENT: X <- Y * X / 100 and Y is kept (so 100 ENTER 10 % gives 10, then + gives 110).
 * - Financial keys N, I, PV, PMT, FV: if a number was typed, or the previous key was not a
 *   financial key, the value in X is STORED in the register; if the previous key was also a
 *   financial key, the register is COMPUTED from the other four (the result goes to X).
 */
class RpnEngine {
    var x: BigDecimal = BigDecimal.ZERO; private set
    var y: BigDecimal = BigDecimal.ZERO; private set
    var z: BigDecimal = BigDecimal.ZERO; private set
    var t: BigDecimal = BigDecimal.ZERO; private set
    var lastX: BigDecimal = BigDecimal.ZERO; private set

    /** Text being typed, or null when X holds a computed/entered value. */
    var entry: String? = null; private set
    private var liftEnabled = false
    private var previousKeyWasFinancial = false

    var error: FinancialError? = null; private set
    var errorDetail: String = ""; private set
    var warnings: List<com.darkempire78.opencalculator.finance.FinancialWarning> = emptyList(); private set

    /** Last successful computation, cleared at the start of every key press. */
    var lastEvent: RpnEvent? = null; private set

    val memory: Array<BigDecimal> = Array(MEMORY_SIZE) { BigDecimal.ZERO }

    var tvm: TvmRegisters = TvmRegisters(); private set
    var periodsPerYear: BigDecimal = BigDecimal(12)
    private var cashFlow0: BigDecimal = BigDecimal.ZERO
    private val cashFlowEntries = ArrayList<CashFlow>()
    /** Number of periods already amortized by AMORT. */
    private var amortizedPeriods = 0

    /** Value shown in the display: the typed text or X. */
    val displayValue: BigDecimal get() = entry?.toBigDecimalOrNull() ?: x

    val cashFlows: CashFlows get() = CashFlows(cashFlow0, cashFlowEntries.toList())

    // ---- public key entry ----------------------------------------------------------------

    fun press(key: RpnKey) {
        error = null
        errorDetail = ""
        warnings = emptyList()
        lastEvent = null
        try {
            execute(key)
        } catch (e: com.darkempire78.opencalculator.finance.FinancialException) {
            setError(e.error, e.message.orEmpty())
        } catch (e: ArithmeticException) {
            setError(FinancialError.INVALID_INPUT, e.message.orEmpty())
        }
        previousKeyWasFinancial = key in FINANCIAL_KEYS
    }

    /** Test helper: "2 ENTER 3 +" or "1000 CHS PV". Numbers are typed digit by digit. */
    fun keys(sequence: String) {
        for (token in sequence.trim().split(Regex("\\s+"))) {
            val symbol = SYMBOLS[token]
            when {
                symbol != null -> press(symbol)
                token.matches(Regex("-?[0-9]+(\\.[0-9]+)?")) -> typeNumber(token)
                token.startsWith("STO") -> store(token.removePrefix("STO").toInt())
                token.startsWith("RCL") && token.removePrefix("RCL").toIntOrNull() != null -> recall(token.removePrefix("RCL").toInt())
                else -> press(RpnKey.valueOf(token))
            }
        }
    }

    private fun typeNumber(text: String) {
        val negative = text.startsWith("-")
        for (c in text.removePrefix("-")) press(if (c == '.') RpnKey.DOT else RpnKey.valueOf("D$c"))
        if (negative) press(RpnKey.CHS)
    }

    fun store(register: Int) {
        error = null
        try {
            require(register in 0 until MEMORY_SIZE)
            commitEntry()
            memory[register] = x
            liftEnabled = true
        } catch (e: com.darkempire78.opencalculator.finance.FinancialException) {
            setError(e.error, e.message.orEmpty())
        }
        previousKeyWasFinancial = false
    }

    fun recall(register: Int) {
        error = null
        require(register in 0 until MEMORY_SIZE)
        commitEntry()
        liftIfEnabled()
        x = memory[register]
        liftEnabled = true
        previousKeyWasFinancial = false
    }

    // ---- execution -------------------------------------------------------------------------

    private fun execute(key: RpnKey) {
        when (key) {
            RpnKey.D0, RpnKey.D1, RpnKey.D2, RpnKey.D3, RpnKey.D4,
            RpnKey.D5, RpnKey.D6, RpnKey.D7, RpnKey.D8, RpnKey.D9 -> typeDigit(key.name.substring(1))
            RpnKey.DOT -> typeDot()
            RpnKey.CHS -> changeSign()
            RpnKey.BACKSPACE -> backspace()
            RpnKey.ENTER -> {
                commitEntry()
                t = z; z = y; y = x
                liftEnabled = false
            }
            RpnKey.ADD -> binary("+") { a, b -> a.add(b, WORK_MC) }
            RpnKey.SUB -> binary("-") { a, b -> a.subtract(b, WORK_MC) }
            RpnKey.MUL -> binary("x") { a, b -> a.multiply(b, WORK_MC) }
            RpnKey.DIV -> binary("/") { a, b ->
                if (b.signum() == 0) com.darkempire78.opencalculator.finance.fail(FinancialError.DIVISION_BY_ZERO, "division by zero")
                a.divide(b, WORK_MC)
            }
            RpnKey.POW -> binary("^") { a, b -> BigMath.pow(a, b) }
            RpnKey.RECIPROCAL -> unary("1/x") { v ->
                if (v.signum() == 0) com.darkempire78.opencalculator.finance.fail(FinancialError.DIVISION_BY_ZERO, "1/0")
                BigDecimal.ONE.divide(v, WORK_MC)
            }
            RpnKey.SQRT -> unary("sqrt") { v -> BigMath.sqrt(v) }
            RpnKey.PERCENT -> unary("%") { v -> y.multiply(v, WORK_MC).divide(HUNDRED, WORK_MC) }
            RpnKey.DELTA_PERCENT -> unary("delta%") { v ->
                if (y.signum() == 0) com.darkempire78.opencalculator.finance.fail(FinancialError.DIVISION_BY_ZERO, "base is zero")
                v.subtract(y, WORK_MC).divide(y, WORK_MC).multiply(HUNDRED, WORK_MC)
            }
            RpnKey.SWAP -> { commitEntry(); val tmp = x; x = y; y = tmp; liftEnabled = true }
            RpnKey.ROLL_DOWN -> { commitEntry(); val tmp = x; x = y; y = z; z = t; t = tmp; liftEnabled = true }
            RpnKey.ROLL_UP -> { commitEntry(); val tmp = x; x = t; t = z; z = y; y = tmp; liftEnabled = true }
            RpnKey.LAST_X -> {
                commitEntry()
                liftIfEnabled()
                x = lastX
                liftEnabled = true
            }
            RpnKey.CLX -> { entry = null; x = BigDecimal.ZERO; liftEnabled = false }
            RpnKey.CLEAR_STACK -> { entry = null; x = BigDecimal.ZERO; y = x; z = x; t = x; liftEnabled = false }
            RpnKey.N, RpnKey.I, RpnKey.PV, RpnKey.PMT, RpnKey.FV -> financialKey(key)
            RpnKey.N_TIMES_PER_YEAR -> { commitEntry(); x = x.multiply(periodsPerYear, WORK_MC).round(MC); storeRegister(RpnKey.N, x); liftEnabled = true }
            RpnKey.I_DIV_PER_YEAR -> { commitEntry(); x = x.divide(periodsPerYear, WORK_MC).round(MC); storeRegister(RpnKey.I, x); liftEnabled = true }
            RpnKey.BEGIN -> tvm = tvm.copy(beginMode = true)
            RpnKey.END -> tvm = tvm.copy(beginMode = false)
            RpnKey.CLEAR_FINANCIAL -> {
                tvm = TvmRegisters(beginMode = tvm.beginMode)
                cashFlow0 = BigDecimal.ZERO
                cashFlowEntries.clear()
                amortizedPeriods = 0
            }
            RpnKey.CLEAR_ALL -> reset()
            RpnKey.RCL_N -> recallRegister(tvm.n)
            RpnKey.RCL_I -> recallRegister(tvm.i)
            RpnKey.RCL_PV -> recallRegister(tvm.pv)
            RpnKey.RCL_PMT -> recallRegister(tvm.pmt)
            RpnKey.RCL_FV -> recallRegister(tvm.fv)
            RpnKey.CF0 -> { commitEntry(); cashFlow0 = x; cashFlowEntries.clear(); liftEnabled = true }
            RpnKey.CFJ -> {
                commitEntry()
                if (cashFlowEntries.size >= CashFlows.MAX_ENTRIES) com.darkempire78.opencalculator.finance.fail(FinancialError.OUT_OF_RANGE, "cash flow list is full")
                cashFlowEntries.add(CashFlow(x, 1))
                liftEnabled = true
            }
            RpnKey.NJ -> {
                commitEntry()
                if (cashFlowEntries.isEmpty()) com.darkempire78.opencalculator.finance.fail(FinancialError.MISSING_REGISTER, "enter a CFj first")
                val count = x.toBigIntegerExact().toInt()
                cashFlowEntries[cashFlowEntries.size - 1] = cashFlowEntries.last().copy(count = count)
                CashFlows(cashFlow0, cashFlowEntries.toList()).validate()
                liftEnabled = true
            }
            RpnKey.NPV -> {
                commitEntry()
                val rate = tvm.i ?: com.darkempire78.opencalculator.finance.fail(FinancialError.MISSING_REGISTER, "i")
                val flows = cashFlows
                publish(Npv.compute(flows, rate))
                if (error == null) lastEvent = RpnEvent(RpnEventType.NPV, "NPV", flowInputs(flows) + ("i" to rate), x)
            }
            RpnKey.IRR -> {
                commitEntry()
                val flows = cashFlows
                publish(Irr.compute(flows))
                if (error == null) lastEvent = RpnEvent(RpnEventType.IRR, "IRR", flowInputs(flows), x)
            }
            RpnKey.AMORT -> amortize()
        }
    }

    // ---- number entry ----------------------------------------------------------------------

    private fun typeDigit(digit: String) {
        val current = entry
        if (current == null) {
            liftIfEnabled()
            entry = digit
            liftEnabled = true
        } else if (digitCount(current) < MAX_DIGITS) {
            entry = if (current == "0") digit else if (current == "-0") "-$digit" else current + digit
        }
        x = entry!!.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    private fun typeDot() {
        val current = entry
        if (current == null) {
            liftIfEnabled()
            entry = "0."
            liftEnabled = true
        } else if (!current.contains('.')) {
            entry = "$current."
        }
        x = entry!!.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    private fun digitCount(text: String) = text.count { it.isDigit() }

    private fun changeSign() {
        val current = entry
        if (current != null) {
            entry = if (current.startsWith("-")) current.substring(1) else "-$current"
            x = entry!!.toBigDecimalOrNull() ?: BigDecimal.ZERO
        } else {
            x = x.negate()
        }
    }

    private fun backspace() {
        val current = entry
        if (current == null) {
            x = BigDecimal.ZERO
            liftEnabled = false
            return
        }
        val shortened = current.dropLast(1)
        if (shortened.isEmpty() || shortened == "-") {
            entry = null
            x = BigDecimal.ZERO
            liftEnabled = false
        } else {
            entry = shortened
            x = shortened.toBigDecimalOrNull() ?: BigDecimal.ZERO
        }
    }

    private fun commitEntry() {
        entry?.let {
            x = it.toBigDecimalOrNull() ?: BigDecimal.ZERO
            entry = null
        }
    }

    private fun liftIfEnabled() {
        if (liftEnabled) {
            t = z; z = y; y = x
        }
    }

    // ---- arithmetic ------------------------------------------------------------------------

    private fun binary(symbol: String, operation: (BigDecimal, BigDecimal) -> BigDecimal) {
        commitEntry()
        val result = operation(y, x)
        lastEvent = RpnEvent(RpnEventType.OPERATION, symbol, listOf("Y" to y, "X" to x), result.round(MC))
        lastX = x
        x = result.round(MC)
        y = z
        z = t
        liftEnabled = true
    }

    private fun unary(symbol: String, operation: (BigDecimal) -> BigDecimal) {
        commitEntry()
        val result = operation(x)
        val inputs = if (symbol == "%" || symbol == "delta%") listOf("Y" to y, "X" to x) else listOf("X" to x)
        lastEvent = RpnEvent(RpnEventType.OPERATION, symbol, inputs, result.round(MC))
        lastX = x
        x = result.round(MC)
        liftEnabled = true
    }

    // ---- financial registers ---------------------------------------------------------------

    private fun financialKey(key: RpnKey) {
        val typed = entry != null
        commitEntry()
        if (typed || !previousKeyWasFinancial) {
            storeRegister(key, x)
            liftEnabled = true
        } else {
            val result = when (key) {
                RpnKey.N -> Tvm.solveN(tvm)
                RpnKey.I -> Tvm.solveRate(tvm)
                RpnKey.PV -> Tvm.solvePv(tvm)
                RpnKey.PMT -> Tvm.solvePmt(tvm)
                else -> Tvm.solveFv(tvm)
            }
            if (result is FinancialResult.Success) {
                val inputs = registerInputs(excluding = key)
                storeRegister(key, result.value)
                liftIfEnabled()
                x = result.value
                liftEnabled = true
                lastEvent = RpnEvent(RpnEventType.TVM, registerName(key), inputs, result.value, beginMode = tvm.beginMode)
            } else {
                publish(result)
            }
        }
    }

    private fun registerName(key: RpnKey) = when (key) {
        RpnKey.N -> "n"; RpnKey.I -> "i"; RpnKey.PV -> "PV"; RpnKey.PMT -> "PMT"; else -> "FV"
    }

    private fun registerInputs(excluding: RpnKey): List<Pair<String, BigDecimal>> =
        listOf(RpnKey.N to tvm.n, RpnKey.I to tvm.i, RpnKey.PV to tvm.pv, RpnKey.PMT to tvm.pmt, RpnKey.FV to tvm.fv)
            .filter { it.first != excluding }
            .mapNotNull { (k, v) -> v?.let { registerName(k) to it } }

    private fun storeRegister(key: RpnKey, value: BigDecimal) {
        tvm = when (key) {
            RpnKey.N -> tvm.copy(n = value)
            RpnKey.I -> tvm.copy(i = value)
            RpnKey.PV -> tvm.copy(pv = value)
            RpnKey.PMT -> tvm.copy(pmt = value)
            else -> tvm.copy(fv = value)
        }
    }

    private fun recallRegister(value: BigDecimal?) {
        commitEntry()
        liftIfEnabled()
        x = value ?: BigDecimal.ZERO
        liftEnabled = true
    }

    private fun flowInputs(flows: CashFlows): List<Pair<String, BigDecimal>> =
        listOf("CF0" to flows.cf0) + flows.flows.mapIndexed { index, flow -> "CF${index + 1}x${flow.count}" to flow.amount }

    /** Puts a computed value in X (with stack lift) or raises the error. */
    private fun publish(result: FinancialResult<BigDecimal>) {
        when (result) {
            is FinancialResult.Success -> {
                liftIfEnabled()
                x = result.value
                liftEnabled = true
                warnings = result.warnings
            }
            is FinancialResult.Failure -> setError(result.error, result.detail)
        }
    }

    /** AMORT: amortizes the next X periods. Result: X = interest, Y = principal, Z = balance. */
    private fun amortize() {
        commitEntry()
        val count = x.toBigIntegerExact().toInt()
        val pv = tvm.pv ?: com.darkempire78.opencalculator.finance.fail(FinancialError.MISSING_REGISTER, "PV")
        val pmt = tvm.pmt ?: com.darkempire78.opencalculator.finance.fail(FinancialError.MISSING_REGISTER, "PMT")
        val rate = tvm.i ?: com.darkempire78.opencalculator.finance.fail(FinancialError.MISSING_REGISTER, "i")
        val from = amortizedPeriods + 1
        val to = amortizedPeriods + count
        // Loan magnitude and payment magnitude; sign convention of the registers is preserved on output.
        val summary = Amortization.schedule(pv.abs(), pmt.abs(), rate, from, to, tvm.beginMode)
        if (summary is FinancialResult.Failure) {
            setError(summary.error, summary.detail)
            return
        }
        val s = (summary as FinancialResult.Success).value
        // Interest and principal carry the sign of the payment, the balance the sign of PV
        val paymentSign = if (pmt.signum() >= 0) BigDecimal.ONE else BigDecimal.ONE.negate()
        val balanceSign = if (pv.signum() >= 0) BigDecimal.ONE else BigDecimal.ONE.negate()
        amortizedPeriods = to
        liftIfEnabled()
        // Stack after AMORT: Z = balance, Y = principal, X = interest
        t = y
        z = s.balance.multiply(balanceSign)
        y = s.totalPrincipal.multiply(paymentSign)
        x = s.totalInterest.multiply(paymentSign)
        lastEvent = RpnEvent(
            RpnEventType.AMORTIZATION, "AMORT",
            listOf("PV" to pv, "PMT" to pmt, "i" to rate, "periods" to BigDecimal(count), "from" to BigDecimal(from)),
            x, extra = listOf("principal" to y, "balance" to z), beginMode = tvm.beginMode
        )
        liftEnabled = true
    }

    /** Pushes an externally computed value (e.g. a converted rate) into X, like recalling a register. */
    fun enterValue(value: BigDecimal) {
        error = null
        errorDetail = ""
        commitEntry()
        liftIfEnabled()
        x = value.round(MC)
        liftEnabled = true
        previousKeyWasFinancial = false
    }

    // ---- state -----------------------------------------------------------------------------

    private fun setError(error: FinancialError, detail: String) {
        this.error = error
        this.errorDetail = detail
    }

    fun reset() {
        x = BigDecimal.ZERO; y = BigDecimal.ZERO; z = BigDecimal.ZERO; t = BigDecimal.ZERO
        lastX = BigDecimal.ZERO
        entry = null
        liftEnabled = false
        error = null
        errorDetail = ""
        tvm = TvmRegisters()
        cashFlow0 = BigDecimal.ZERO
        cashFlowEntries.clear()
        amortizedPeriods = 0
        for (i in memory.indices) memory[i] = BigDecimal.ZERO
    }

    /** Immutable copy of everything needed to restore the engine (rotation, process death). */
    fun snapshot(): RpnSnapshot = RpnSnapshot(
        x, y, z, t, lastX, entry, liftEnabled, memory.toList(), tvm, periodsPerYear,
        cashFlowEntries.toList(), cashFlow0, amortizedPeriods
    )

    fun restore(s: RpnSnapshot) {
        x = s.x; y = s.y; z = s.z; t = s.t; lastX = s.lastX
        entry = s.entry
        liftEnabled = s.liftEnabled
        for (i in memory.indices) memory[i] = s.memory.getOrElse(i) { BigDecimal.ZERO }
        tvm = s.tvm
        periodsPerYear = s.periodsPerYear
        cashFlow0 = s.cashFlow0
        cashFlowEntries.clear(); cashFlowEntries.addAll(s.cashFlowEntries)
        amortizedPeriods = s.amortizedPeriods
        error = null; errorDetail = ""
        previousKeyWasFinancial = false
    }

    companion object {
        const val MEMORY_SIZE = 10
        const val MAX_DIGITS = 20
        private val HUNDRED = BigDecimal(100)
        private val FINANCIAL_KEYS = setOf(RpnKey.N, RpnKey.I, RpnKey.PV, RpnKey.PMT, RpnKey.FV)
        private val SYMBOLS = mapOf(
            "+" to RpnKey.ADD, "-" to RpnKey.SUB, "*" to RpnKey.MUL, "/" to RpnKey.DIV,
            "%" to RpnKey.PERCENT, "n" to RpnKey.N, "i" to RpnKey.I, "." to RpnKey.DOT
        )
    }
}

data class RpnSnapshot(
    @SerializedName("x") val x: BigDecimal,
    @SerializedName("y") val y: BigDecimal,
    @SerializedName("z") val z: BigDecimal,
    @SerializedName("t") val t: BigDecimal,
    @SerializedName("lastX") val lastX: BigDecimal,
    @SerializedName("entry") val entry: String?,
    @SerializedName("liftEnabled") val liftEnabled: Boolean,
    @SerializedName("memory") val memory: List<BigDecimal>,
    @SerializedName("tvm") val tvm: TvmRegisters,
    @SerializedName("periodsPerYear") val periodsPerYear: BigDecimal,
    @SerializedName("cashFlowEntries") val cashFlowEntries: List<CashFlow>,
    @SerializedName("cashFlow0") val cashFlow0: BigDecimal,
    @SerializedName("amortizedPeriods") val amortizedPeriods: Int
)
