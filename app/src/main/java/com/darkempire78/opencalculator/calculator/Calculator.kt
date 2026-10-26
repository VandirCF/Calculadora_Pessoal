package com.darkempire78.opencalculator.calculator

import android.os.Build
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

var division_by_0 = false
var domain_error = false
var syntax_error = false
var is_infinity = false
var require_real_number = false

enum class CalcError { SYNTAX, DOMAIN, REQUIRE_REAL, DIVISION_BY_ZERO, INFINITY }

sealed class CalcResult {
    data class Value(val value: BigDecimal) : CalcResult()
    data class Error(val error: CalcError) : CalcResult()
}

class Calculator(
        private val numberPrecisionDecimal: Int
    ) {

    fun factorial(number: BigDecimal): BigDecimal {
        if (number >= BigDecimal(3000)) {
            is_infinity = true
            return BigDecimal.ZERO
        }
        return if (number < BigDecimal.ZERO) {
            domain_error = true
            BigDecimal.ZERO
        } else {
            val decimalPartOfNumber = number.toDouble() - number.toInt()
            if (decimalPartOfNumber == 0.0) {
                var factorial = BigInteger("1")
                for (i in 1..number.toInt()) {
                    factorial *= i.toBigInteger()
                }
                factorial.toBigDecimal()
            } else gammaLanczos(number + BigDecimal.ONE)
        }
    }

    private fun gammaLanczos(x: BigDecimal): BigDecimal {
        // Lanczos approximation parameters
        val p = arrayOf(
            676.5203681218851,
            -1259.1392167224028,
            771.3234287776531,
            -176.6150291621406,
            12.507343278686905,
            -0.13857109526572012,
            9.984369578019572e-6,
            1.5056327351493116e-7
        )
        val g = 7.0
        val z = x.toDouble() - 1.0

        var a = 0.9999999999998099
        for (i in p.indices) {
            a += p[i] / (z + i + 1)
        }

        val t = z + g + 0.5
        val sqrtTwoPi = sqrt(2.0 * PI)
        val firstPart = sqrtTwoPi * t.pow(z + 0.5) * exp(-t)
        val result = firstPart * a

        return BigDecimal(result, MathContext.DECIMAL64)
    }

    private fun exponentiation(x: BigDecimal, parseFactor: BigDecimal): BigDecimal {
        var value = x
        val intPart = parseFactor.toInt()
        val decimalPart = parseFactor.subtract(BigDecimal(intPart))

        // if the number is null
        if (value == BigDecimal.ZERO) {
            syntax_error = false
            value = BigDecimal.ZERO
        } else {
            if (parseFactor > BigDecimal(10000)) {
                is_infinity = true
                value = BigDecimal.ZERO
            } else {
                // If the number is negative and the factor is a float ( e.g : (-5)^0.5 )
                if (value < BigDecimal.ZERO && decimalPart != BigDecimal.ZERO) {
                    require_real_number = true
                } // the factor is NOT a float
                else if (parseFactor > BigDecimal.ZERO) {

                    // To support bigdecimal exponent (e.g: 3.5)
                    value = value.pow(intPart, MathContext.UNLIMITED)
                        .multiply(
                            BigDecimal.valueOf(
                                value.toDouble().pow(decimalPart.toDouble())
                            )
                        )

                    // To fix sqrt(2)^2 = 2
                    val decimal = value.toInt()
                    val fractional = value.toDouble() - decimal
                    if (fractional > 0 && fractional < 1.0E-30) {
                        value = decimal.toBigDecimal()
                    }
                } else {
                    // To support negative factor
                    value = value.pow(-intPart, MathContext.DECIMAL64)
                        .multiply(
                            BigDecimal.valueOf(
                                value.toDouble().pow(-decimalPart.toDouble())
                            )
                        )

                    value = try {
                        BigDecimal.ONE.divide(value)
                    } catch (e: ArithmeticException) {
                        // if the result is a non-terminating decimal expansion
                        BigDecimal.ONE.divide(value, numberPrecisionDecimal, RoundingMode.HALF_DOWN)
                    }
                }
            }
        }
        return value
    }

    /** Trigonometric results this close to zero are floating point noise (sin(180 deg) = 1.2e-16). */
    private fun snapNoise(value: BigDecimal): BigDecimal =
        if (value.abs() < NOISE_THRESHOLD) BigDecimal.ZERO else value

    private fun isTangentUndefined(angle: BigDecimal, degrees: Boolean): Boolean {
        val radians = if (degrees) Math.toRadians(angle.toDouble()) else angle.toDouble()
        return abs(cos(radians)) < TANGENT_UNDEFINED_THRESHOLD
    }

    /** n-th root of [radicand]. Odd integer indexes accept negative radicands. */
    private fun nthRoot(index: BigDecimal, radicand: BigDecimal): BigDecimal {
        if (index.signum() == 0) {
            domain_error = true
            return BigDecimal.ZERO
        }
        val isIntegerIndex = index.stripTrailingZeros().scale() <= 0
        if (radicand.signum() < 0) {
            val isOddIndex = isIntegerIndex && index.toBigInteger().testBit(0)
            if (!isOddIndex) {
                require_real_number = true
                return BigDecimal.ZERO
            }
            return nthRoot(index, radicand.negate()).negate()
        }
        if (radicand.signum() == 0) return BigDecimal.ZERO
        val root = radicand.toDouble().pow(1.0 / index.toDouble())
        if (root.isInfinite() || root.isNaN()) {
            is_infinity = true
            return BigDecimal.ZERO
        }
        // Return an exact integer when the root is exact (e.g. 3 root of 27 = 3)
        if (isIntegerIndex && index.abs() <= BigDecimal(1000)) {
            val candidate = BigDecimal(round(root))
            if (candidate.pow(index.toInt()).compareTo(radicand) == 0) return candidate
        }
        return BigDecimal(root, MathContext.DECIMAL64)
    }

    /**
     * Evaluates [equation] and reports the outcome as a value instead of through the global
     * error flags. Flags are reset before and after, and access is serialized, so results of
     * different evaluations cannot leak into each other.
     */
    fun evaluateSafe(equation: String, isDegreeModeActivated: Boolean): CalcResult = synchronized(FLAGS_LOCK) {
        resetErrorFlags()
        val value = try {
            evaluate(equation, isDegreeModeActivated)
        } catch (e: ArithmeticException) {
            resetErrorFlags()
            return CalcResult.Error(CalcError.DOMAIN)
        } catch (e: NumberFormatException) {
            resetErrorFlags()
            return CalcResult.Error(CalcError.SYNTAX)
        }
        val error = when {
            syntax_error -> CalcError.SYNTAX
            domain_error -> CalcError.DOMAIN
            require_real_number -> CalcError.REQUIRE_REAL
            division_by_0 -> CalcError.DIVISION_BY_ZERO
            is_infinity -> CalcError.INFINITY
            else -> null
        }
        resetErrorFlags()
        if (error != null) CalcResult.Error(error) else CalcResult.Value(value)
    }

    private fun resetErrorFlags() {
        division_by_0 = false
        domain_error = false
        syntax_error = false
        is_infinity = false
        require_real_number = false
    }

    companion object {
        private val FLAGS_LOCK = Any()
        private val NOISE_THRESHOLD = BigDecimal("1.0E-14")
        private const val TANGENT_UNDEFINED_THRESHOLD = 1.0E-12
    }

    fun bigDecimalSqrtFormerAndroidVersion(value: BigDecimal, mathContext: MathContext): BigDecimal {
        // Newton's method for square root calculation with Android versions prior to API 33
        var x0 = BigDecimal(0)
        var x1 = value.divide(BigDecimal(2), mathContext)

        // != evaluated true when comparing 0 and 0.0
        // This allowed the passing of 0.0 (or more trailing zeroes) to be divided.
        while (x0 < x1 || x0 > x1) {
            x0 = x1
            x1 = value.divide(x0, mathContext).add(x0).divide(BigDecimal(2), mathContext)
        }

        return x1
    }

    fun evaluate(equation: String, isDegreeModeActivated: Boolean): BigDecimal {
        println("Equation BigDecimal : $equation")
        return object : Any() {
            var pos = -1
            var ch = 0
            fun nextChar() {
                ch = if (++pos < equation.length) equation[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): BigDecimal {
                nextChar()
                val x = parseExpression()
                if (pos < equation.length) println("Unexpected: \"" + ch.toChar() + "\" in expression: " + equation)
                return x
            }

            fun parseExpression(): BigDecimal {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x = x.add(parseTerm()) // addition
                    else if (eat('-'.code)) x = x.subtract(parseTerm()) // subtraction
                    else return x
                }
            }

            fun parseTerm(): BigDecimal {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x = x.multiply(parseFactor()) // Multiplication
                    else if (eat('#'.code)) { // Modulo
                        val fractionDenominator = parseFactor()
                        if (fractionDenominator == BigDecimal.ZERO) {
                            division_by_0 = true
                            x = BigDecimal.ZERO
                        } else {
                            x = x.rem(fractionDenominator)
                        }
                    }
                    else if (eat('/'.code)) { // Division
                        val fractionDenominator = parseFactor()
                        // The Double value is the result of sin(2π) in Radian mode after conversions (0)
                        // This catches the error/crash during zero division in issue #499
                        if (fractionDenominator.toFloat() == 0f || fractionDenominator.toDouble() == -2.4492935982947064E-16) {
                            division_by_0 = true
                            x = BigDecimal.ZERO
                        } else {
                            try {
                                x = x.divide(fractionDenominator)
                            } catch (e: ArithmeticException) { // if the result is a non-terminating decimal expansion
                                x = x.divide(fractionDenominator, numberPrecisionDecimal, RoundingMode.HALF_DOWN)
                                println(x)
                            }
                        }
                    }
                    else return x
                }
            }

            fun parseFactor(): BigDecimal {
                if (eat('+'.code)) return parseFactor().plus() // unary plus
                if (eat('-'.code)) return parseFactor().unaryMinus() // unary minus
                var x: BigDecimal
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    if (!eat(')'.code)) {
                        println("Missing ')'")
                        x = BigDecimal.ZERO
                        syntax_error = true
                    }
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    val string = equation.substring(startPos, pos)
                    if (string.count { it == '.' } > 1) {
                        x = BigDecimal.ZERO
                        syntax_error = true
                    } else {
                        if ((string.length == 1) && (string[0] == '.')) {
                            x = BigDecimal.ZERO
                            syntax_error = true
                        } else {
                            x = BigDecimal(string)
                        }
                    }
                } else if (eat('e'.code)) {
                    x = BigDecimal(Math.E)
                } else if (eat('π'.code)) {
                    x = BigDecimal(PI)
                } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func: String = equation.substring(startPos, pos)
                    if (eat('('.code)) {
                        x = parseExpression()
                        if (!eat(')'.code)) x = parseFactor()
                    } else {
                        x = parseFactor()
                    }
                    println(x)
                    when (func) {
                        "sqrt" -> {
                            if (x >= BigDecimal.ZERO) {
                                // Set the precision for the square root calculation
                                val integerPartLength = x.toString().length
                                val maxPrecision = (integerPartLength + 50).coerceAtMost(1000) // Maximum precision is 1000
                                val precision = MathContext(maxPrecision, RoundingMode.HALF_DOWN)
                                x = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Use default BigDecimal sqrt function (API 33)
                                    x.sqrt(precision)
                                } else { // Use Newton's method for square root calculation with Android versions prior to API 33
                                    bigDecimalSqrtFormerAndroidVersion(x, precision)
                                }
                            } else {
                                require_real_number = true
                            }

                        }
                        "abs" -> {
                            x = x.abs()
                        }
                        "factorial" -> {
                            x = factorial(x)
                        }
                        "ln" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                is_infinity = true
                                x = BigDecimal.ZERO
                            } else if (x <= BigDecimal.ZERO) {
                                domain_error = true
                            } else {
                                x = BigDecimal(ln(x.toDouble()))
                            }
                        }
                        "logtwo" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                is_infinity = true
                                x = BigDecimal.ZERO
                            } else if (x <= BigDecimal.ZERO) {
                                domain_error = true
                            } else {
                                x = BigDecimal(log2(x.toDouble()))
                            }
                        }
                        "logten" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                is_infinity = true
                                x = BigDecimal.ZERO
                            } else if (x <= BigDecimal.ZERO) {
                                domain_error = true
                            } else {
                                x = BigDecimal(log10(x.toDouble()))
                            }
                        }
                        "xp" -> {
                            x = exponentiation(BigDecimal(Math.E), x)
                        }
                        "sin" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                is_infinity = true
                                x = BigDecimal.ZERO
                            } else if (isDegreeModeActivated) {
                                x = sin(Math.toRadians(x.toDouble())).toBigDecimal()
                                // https://stackoverflow.com/questions/29516222/how-to-get-exact-value-of-trigonometric-functions-in-java
                            } else {
                                x = sin(x.toDouble()).toBigDecimal()
                            }
                            x = snapNoise(x)
                        }
                        "cos" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                is_infinity = true
                                x = BigDecimal.ZERO
                            } else if (isDegreeModeActivated) {
                                x = cos(Math.toRadians(x.toDouble())).toBigDecimal()
                            } else {
                                x = cos(x.toDouble()).toBigDecimal()
                            }
                            x = snapNoise(x)
                        }
                        "tan" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                is_infinity = true
                                x = BigDecimal.ZERO
                            } else if (isTangentUndefined(x, isDegreeModeActivated)) {
                                // Tangent is undefined where cos = 0: (2k+1)*90 degrees, (2k+1)*pi/2 radians
                                domain_error = true
                                x = BigDecimal.ZERO
                            } else {
                                x = if (isDegreeModeActivated) {
                                    tan(Math.toRadians(x.toDouble())).toBigDecimal()
                                } else {
                                    tan(x.toDouble()).toBigDecimal()
                                }
                                x = snapNoise(x)
                            }
                        }
                        "arcsi" -> {
                            if (abs(x.toDouble()) > 1) {
                                x = BigDecimal.ZERO
                                domain_error = true
                            } else {
                                x = if (isDegreeModeActivated) {
                                    (asin(x.toDouble()) * 180 / Math.PI).toBigDecimal()
                                } else {
                                    asin(x.toDouble()).toBigDecimal()
                                }
                                x = snapNoise(x)
                            }
                        }
                        "arcco" -> {
                            if (abs(x.toDouble()) > 1) {
                                x = BigDecimal.ZERO
                                domain_error = true
                            } else {
                                x = if (isDegreeModeActivated) {
                                    (acos(x.toDouble())*180/Math.PI).toBigDecimal()
                                } else {
                                    acos(x.toDouble()).toBigDecimal()
                                }
                                x = snapNoise(x)
                            }

                        }
                        "arcta" -> {
                            if (x > Double.MAX_VALUE.toBigDecimal()) {
                                is_infinity = true
                                x = BigDecimal.ZERO
                            } else if  (isDegreeModeActivated) {
                                x = (atan(x.toDouble()) * 180 / Math.PI).toBigDecimal()
                            } else {
                                x =atan(x.toDouble()).toBigDecimal()
                            }
                            x = snapNoise(x)
                        }
                        else -> {
                            syntax_error = true
                        }
                    }
                } else {
                    x = BigDecimal.ZERO
                    syntax_error = true
                }
                if (eat('^'.code)) {
                    x = exponentiation(x, parseFactor())
                } else if (eat('@'.code)) { // nth root: index @ radicand (e.g. 3@27 = 3)
                    x = nthRoot(x, parseFactor())
                }
                return x
            }
        }.parse()
    }
}
