package org.ksmt.solver.kbva

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.ksmt.KContext
import org.ksmt.expr.KBitVec64Value
import org.ksmt.expr.KExpr
import org.ksmt.sort.KBv64Sort
import org.ksmt.utils.mkConst
import org.ksmt.utils.toBinary
import kotlin.random.Random
import kotlin.test.assertEquals

typealias PositiveLong = Long
typealias NegativeLong = Long

class BitVecTest {
    private var context = KContext()
    private var solver = KBVASolver(context)

    @BeforeEach
    fun createNewEnvironment() {
        context = KContext()
        solver = KBVASolver(context)
    }

    @AfterEach
    fun clearResources() {
        solver.close()
    }

    private fun createTwoRandomLongValues(): Pair<NegativeLong, PositiveLong> {
        val negativeValue = Random.nextLong(from = Int.MIN_VALUE.toLong(), until = 0L)
        val positiveValue = Random.nextLong(from = 1L, until = Int.MAX_VALUE.toLong())

        return negativeValue to positiveValue
    }

    @Test
    fun testNotExpr(): Unit = with(context) {
        val (negativeValue, positiveValue) = createTwoRandomLongValues().let {
            it.first.toBinary() to it.second.toBinary()
        }
        val negativeSizeBits = negativeValue.length.toUInt()
        val positiveSizeBits = positiveValue.length.toUInt()

        val negativeBv = mkBv(negativeValue, negativeSizeBits)
        val positiveBv = mkBv(positiveValue, positiveSizeBits)

        val negativeSymbolicValue = negativeBv.sort().mkConst("negative_symbolic_variable")
        val positiveSymbolicValue = positiveBv.sort().mkConst("positive_symbolic_variable")

        solver.assert(mkBvNotExpr(mkBvNotExpr(negativeBv)) eq negativeBv)
        solver.assert(mkBvNotExpr(negativeBv) eq negativeSymbolicValue)

        solver.assert(mkBvNotExpr(mkBvNotExpr(positiveBv)) eq positiveBv)
        solver.assert(mkBvNotExpr(positiveBv) eq positiveSymbolicValue)

        solver.check()

        val actualNegativeValue = (solver.model().eval(negativeSymbolicValue) as KBitVec64Value).numberValue.toBinary()
        val actualPositiveValue = (solver.model().eval(positiveSymbolicValue) as KBitVec64Value).numberValue.toBinary()
        val sizeBits = negativeBv.sort().sizeBits

        val expectedValueTransformation = { stringValue: String ->
            stringValue
                .padStart(sizeBits.toInt(), if (stringValue[0] == '1') '1' else '0')
                .map { if (it == '1') '0' else '1' }
                .joinToString("")
        }

        val expectedNegativeValue = expectedValueTransformation(negativeValue)
        val expectedPositiveValue = expectedValueTransformation(positiveValue)

        assertEquals(
            expectedNegativeValue,
            actualNegativeValue,
            message = "Size bits: $sizeBits, negativeValue: $negativeValue"
        )
        assertEquals(
            expectedPositiveValue,
            actualPositiveValue,
            message = "Size bits: $sizeBits, positiveValue: $positiveValue"
        )
    }

    @Test
    fun testAndExpr(): Unit = with(context) {
        val value = Random.nextLong()

        val bv = value.toBv()
        val anotherBv = Random.nextLong().toBv()
        val zero = 0L.toBv()

        val conjunctionWithItself = mkBvAndExpr(bv, bv)
        val conjunctionWithZero = mkBvAndExpr(bv, zero)
        val conjunctionWithOnes = mkBvAndExpr(bv, mkBvNotExpr(zero))

        val conjunctionResult = mkBv64Sort().mkConst("symbolicVariable")

        solver.assert(conjunctionWithItself eq bv)
        solver.assert(conjunctionWithZero eq zero)
        solver.assert(conjunctionWithOnes eq bv)
        solver.assert(mkBvAndExpr(bv, anotherBv) eq conjunctionResult)

        solver.check()

        val actualValue = (solver.model().eval(conjunctionResult) as KBitVec64Value).numberValue
        val expectedValue = value and anotherBv.numberValue

        assertEquals(expectedValue, actualValue)
    }

    @Test
    fun testOrExpr(): Unit = with(context) {
        val value = Random.nextLong()

        val bv = value.toBv()
        val anotherBv = Random.nextLong().toBv()
        val zero = 0L.toBv()

        val disjunctionWithItself = mkBvOrExpr(bv, bv)
        val disjunctionWithZero = mkBvOrExpr(bv, zero)
        val disjunctionWithOnes = mkBvOrExpr(bv, mkBvNotExpr(zero))

        val disjunctionResult = mkBv64Sort().mkConst("symbolicVariable")

        solver.assert(disjunctionWithItself eq bv)
        solver.assert(disjunctionWithZero eq bv)
        solver.assert(disjunctionWithOnes eq mkBvNotExpr(zero))
        solver.assert(mkBvOrExpr(bv, anotherBv) eq disjunctionResult)

        solver.check()

        val actualValue = (solver.model().eval(disjunctionResult) as KBitVec64Value).numberValue
        val expectedValue = value or anotherBv.numberValue

        assertEquals(expectedValue, actualValue)
    }

    private fun testBinaryOperation(
        symbolicOperation: (KExpr<KBv64Sort>, KExpr<KBv64Sort>) -> KExpr<KBv64Sort>,
        concreteOperation: (Long, Long) -> Long
    ): Unit = with(context) {
        val (negativeValue, positiveValue) = createTwoRandomLongValues()

        val negativeBv = negativeValue.toBv()
        val positiveBv = positiveValue.toBv()

        val firstResult = mkBv64Sort().mkConst("symbolicVariable")
        val secondResult = mkBv64Sort().mkConst("anotherSymbolicVariable")

        solver.assert(symbolicOperation(negativeBv, positiveBv) eq firstResult)
        solver.assert(symbolicOperation(positiveBv, negativeBv) eq secondResult)
        solver.check()

        val firstActualValue = (solver.model().eval(firstResult) as KBitVec64Value).numberValue
        val secondActualValue = (solver.model().eval(secondResult) as KBitVec64Value).numberValue

        val firstExpectedValue = concreteOperation(negativeValue, positiveValue)
        val secondExpectedValue = concreteOperation(positiveValue, negativeValue)

        assertEquals(firstExpectedValue, firstActualValue)
        assertEquals(secondExpectedValue, secondActualValue)
    }

    @Test
    fun testXorExpr(): Unit = testBinaryOperation(context::mkBvXorExpr, Long::xor)

    @Test
    fun testNAndExpr(): Unit = testBinaryOperation(context::mkBvNAndExpr) { arg0: Long, arg1: Long ->
        (arg0 and arg1).inv()
    }

    @Test
    fun testNorExpr(): Unit = testBinaryOperation(context::mkBvNorExpr) { arg0: Long, arg1: Long ->
        (arg0 or arg1).inv()
    }

    @Test
    fun testXNorExpr(): Unit = testBinaryOperation(context::mkBvXNorExpr) { arg0: Long, arg1: Long ->
        (arg0 xor arg1).inv()
    }

    @Test
    fun testAddExpr(): Unit = testBinaryOperation(context::mkBvAddExpr, Long::plus)
}