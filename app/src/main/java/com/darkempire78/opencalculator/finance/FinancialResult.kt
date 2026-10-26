package com.darkempire78.opencalculator.finance

import java.math.BigDecimal

/** Why a financial computation could not produce a value. Mapped to localized text by the UI. */
enum class FinancialError {
    INVALID_INPUT,
    DIVISION_BY_ZERO,
    OUT_OF_RANGE,
    OVERFLOW,

    /** Cash in and cash out must have opposite signs for this computation. */
    INCONSISTENT_SIGNS,

    /** The equation has no solution for the given values. */
    NO_SOLUTION,

    /** The iterative method hit its iteration limit. */
    NO_CONVERGENCE,

    /** More than one valid solution exists; none is returned to avoid a misleading answer. */
    MULTIPLE_SOLUTIONS,
    EMPTY_CASH_FLOWS,

    /** IRR is undefined without a change of sign in the cash flows. */
    NO_SIGN_CHANGE,

    /** A register required by the computation has not been set. */
    MISSING_REGISTER
}

/** Non-fatal remark attached to a successful result. */
enum class FinancialWarning {
    /** Cash flows change sign more than once: more than one IRR is mathematically possible. */
    MULTIPLE_SIGN_CHANGES
}

class FinancialException(val error: FinancialError, detail: String) : RuntimeException(detail)

sealed class FinancialResult<out T> {
    data class Success<T>(val value: T, val warnings: List<FinancialWarning> = emptyList()) : FinancialResult<T>()
    data class Failure(
        val error: FinancialError,
        val detail: String = "",
        /** Candidate values for [FinancialError.MULTIPLE_SOLUTIONS]. */
        val candidates: List<BigDecimal> = emptyList()
    ) : FinancialResult<Nothing>()

    val valueOrNull: T? get() = (this as? Success)?.value
}

internal inline fun <T> financial(block: () -> T): FinancialResult<T> = try {
    FinancialResult.Success(block())
} catch (e: FinancialException) {
    FinancialResult.Failure(e.error, e.message.orEmpty())
} catch (e: ArithmeticException) {
    FinancialResult.Failure(FinancialError.DIVISION_BY_ZERO, e.message.orEmpty())
}

internal fun fail(error: FinancialError, detail: String): Nothing = throw FinancialException(error, detail)
