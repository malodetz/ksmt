package org.ksmt.solver.kbva

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.ksmt.KContext
import org.ksmt.decl.KBitVecValueDecl
import org.ksmt.expr.*
import org.ksmt.sort.KBoolSort
import org.ksmt.sort.KBv32Sort
import org.ksmt.sort.KBv64Sort
import org.ksmt.utils.mkConst
import org.ksmt.utils.toBinary
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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

    @Test
    fun testNegationExpr(): Unit = with(context) {
        val (negativeValue, positiveValue) = createTwoRandomLongValues()

        val negativeBv = negativeValue.toBv()
        val positiveBv = positiveValue.toBv()
        val zero = 0.toBv()

        val negNegativeValue = mkBv64Sort().mkConst("neg_negative_value")
        val negPositiveValue = mkBv64Sort().mkConst("neg_positive_value")
        val zeroValue = mkBv32Sort().mkConst("zero_value")

        solver.assert(mkBvNegationExpr(negativeBv) eq negNegativeValue)
        solver.assert(mkBvNegationExpr(positiveBv) eq negPositiveValue)
        solver.assert(mkBvNegationExpr(zero) eq zeroValue)

        solver.check()
        val model = solver.model()

        val evaluatedNegNegativeBv = model.eval(negNegativeValue) as KBitVec64Value
        val evaluatedNegPositiveBv = model.eval(negPositiveValue) as KBitVec64Value
        val evaluatedZeroValue = model.eval(zeroValue) as KBitVec32Value

        val message = "NegativeValue: $negativeValue, positiveValue: $positiveValue"

        assertEquals(-negativeValue, evaluatedNegNegativeBv.numberValue, message)
        assertEquals(-positiveValue, evaluatedNegPositiveBv.numberValue, message)
        assertEquals(expected = 0, evaluatedZeroValue.numberValue, message)
    }

    @Test
    fun testSubExpr(): Unit = testBinaryOperation(context::mkBvSubExpr, Long::minus)

    @Test
    fun testBvExtractExpr(): Unit = with(context) {
        val value = Random.nextLong().toBv()
        val high = Random.nextInt(from = 32, until = 64)
        val low = Random.nextInt(from = 5, until = 32)

        val symbolicValue = mkBvSort(high.toUInt() - low.toUInt() + 1u).mkConst("symbolicVariable")

        solver.assert(symbolicValue eq mkBvExtractExpr(high, low, value))
        solver.check()

        val result = solver.model().eval(symbolicValue) as KBitVecValue<*>
        val sizeBits = value.sort.sizeBits.toInt()
        val expectedResult = value.numberValue.toBinary().substring(sizeBits - high - 1, sizeBits - low)

        assertEquals(expectedResult, (result.decl as KBitVecValueDecl).value)
    }

    @Test
    fun testConcatExpr(): Unit = with(context) {
        val firstBv = Random.nextLong().toBv()
        val secondBv = Random.nextInt().toBv()

        val sizeBits = firstBv.sort.sizeBits + secondBv.sort.sizeBits
        val symbolicConst = mkBvSort(sizeBits).mkConst("symbolicConst")

        solver.assert(symbolicConst eq mkBvConcatExpr(firstBv, secondBv))
        solver.check()

        val resultValue = solver.model().eval(symbolicConst) as KBitVecCustomValue
        val expectedResult = firstBv.numberValue.toBinary() + secondBv.numberValue.toBinary()

        assertEquals(expectedResult, resultValue.binaryStringValue)
    }

    @Test
    fun testBvSignExtensionExpr(): Unit = with(context) {
        val negativeBv = Random.nextInt(from = Int.MIN_VALUE, until = 0).toBv()
        val positiveBv = Random.nextInt(from = 1, until = Int.MAX_VALUE).toBv()

        val positiveSymbolicVariable = mkBvSort(Long.SIZE_BITS.toUInt()).mkConst("positiveSymbolicVariable")
        val negativeSymbolicVariable = mkBvSort(Long.SIZE_BITS.toUInt()).mkConst("negativeSymbolicVariable")

        solver.assert(positiveSymbolicVariable eq mkBvSignExtensionExpr(Int.SIZE_BITS, positiveBv))
        solver.assert(negativeSymbolicVariable eq mkBvSignExtensionExpr(Int.SIZE_BITS, negativeBv))
        solver.check()

        val positiveResult = (solver.model().eval(positiveSymbolicVariable) as KBitVec64Value).numberValue.toBinary()
        val negativeResult = (solver.model().eval(negativeSymbolicVariable) as KBitVec64Value).numberValue.toBinary()

        val expectedPositiveResult = positiveBv.numberValue.toBinary().padStart(Long.SIZE_BITS, '0')
        val expectedNegativeResult = negativeBv.numberValue.toBinary().padStart(Long.SIZE_BITS, '1')

        assertEquals(expectedPositiveResult, positiveResult)
        assertEquals(expectedNegativeResult, negativeResult)
    }

    @Test
    fun testBvZeroExtensionExpr(): Unit = with(context) {
        val negativeBv = Random.nextInt(from = Int.MIN_VALUE, until = 0).toBv()
        val positiveBv = Random.nextInt(from = 1, until = Int.MAX_VALUE).toBv()

        val positiveSymbolicVariable = mkBvSort(Long.SIZE_BITS.toUInt()).mkConst("positiveSymbolicVariable")
        val negativeSymbolicVariable = mkBvSort(Long.SIZE_BITS.toUInt()).mkConst("negativeSymbolicVariable")

        solver.assert(positiveSymbolicVariable eq mkBvZeroExtensionExpr(Int.SIZE_BITS, positiveBv))
        solver.assert(negativeSymbolicVariable eq mkBvZeroExtensionExpr(Int.SIZE_BITS, negativeBv))
        solver.check()

        val positiveResult = (solver.model().eval(positiveSymbolicVariable) as KBitVec64Value).numberValue.toBinary()
        val negativeResult = (solver.model().eval(negativeSymbolicVariable) as KBitVec64Value).numberValue.toBinary()

        val expectedPositiveResult = positiveBv.numberValue.toBinary().padStart(Long.SIZE_BITS, '0')
        val expectedNegativeResult = negativeBv.numberValue.toBinary().padStart(Long.SIZE_BITS, '0')

        assertEquals(expectedPositiveResult, positiveResult)
        assertEquals(expectedNegativeResult, negativeResult)
    }

    @Test
    fun testBvRepeatExpr(): Unit = with(context) {
        val bv = Random.nextInt().toShort().toBv()
        val numberOfRepetitions = 4u

        val symbolicVariable = mkBvSort(bv.sort.sizeBits * numberOfRepetitions).mkConst("symbolicVariable")

        solver.assert(symbolicVariable eq mkBvRepeatExpr(numberOfRepetitions.toInt(), bv))
        solver.check()

        val result = (solver.model().eval(symbolicVariable) as KBitVec64Value).numberValue.toBinary()
        val expectedValue = bv.numberValue.toBinary().repeat(numberOfRepetitions.toInt())

        assertEquals(expectedValue, result)
    }


    private fun testShift(
        symbolicOperation: (KExpr<KBv64Sort>, KExpr<KBv64Sort>) -> KExpr<KBv64Sort>,
        concreteOperation: (Long, Int) -> Long
    ) = with(context) {
        val value = Random.nextLong().toBv()
        val shiftSize = Random.nextInt(from = 1, until = 50).toLong().toBv()

        val symbolicVariable = value.sort().mkConst("symbolicVariable")

        solver.assert(symbolicVariable eq symbolicOperation(value, shiftSize))
        solver.check()

        val expectedResult = concreteOperation(value.numberValue, shiftSize.numberValue.toInt())
        val result = (solver.model().eval(symbolicVariable) as KBitVec64Value).numberValue
        assertEquals(expectedResult, result)
    }

    @Test
    fun testBvShiftLeftExpr(): Unit = testShift(context::mkBvShiftLeftExpr, Long::shl)

    @Test
    fun testBvLogicalShiftRightExpr(): Unit = testShift(context::mkBvLogicalShiftRightExpr, Long::ushr)

    @Test
    fun testBvArithShiftRightExpr(): Unit = testShift(context::mkBvArithShiftRightExpr, Long::shr)

    @Test
    fun testIndexedRotateLeft(): Unit = with(context) {
        val bv = Random.nextLong().toBv()
        val rotateSize = Random.nextInt(from = 1, until = 4)

        val symbolicVariable = bv.sort().mkConst("symbolicVariable")

        solver.assert(symbolicVariable eq mkBvRotateLeftIndexedExpr(rotateSize, bv))
        solver.check()

        val expectedResult = bv.numberValue.toBinary().let {
            it.substring(rotateSize, it.length) + it.substring(0, rotateSize)
        }
        val actualResult = (solver.model().eval(symbolicVariable) as KBitVec64Value).numberValue.toBinary()

        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun testRotateLeft(): Unit = with(context) {
        val bv = Random.nextLong().toBv()
        val rotateSize = Random.nextLong(from = 1, until = 4).toBv()

        val symbolicVariable = bv.sort().mkConst("symbolicVariable")

        solver.assert(symbolicVariable eq mkBvRotateLeftExpr(bv, rotateSize))
        solver.check()

        val expectedResult = bv.numberValue.toBinary().let {
            it.substring(rotateSize.numberValue.toInt(), it.length) + it.substring(0, rotateSize.numberValue.toInt())
        }
        val actualResult = (solver.model().eval(symbolicVariable) as KBitVec64Value).numberValue.toBinary()

        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun testRotateRight(): Unit = with(context) {
        val bv = Random.nextLong().toBv()
        val rotateSize = Random.nextLong(from = 1, until = 4).toBv()

        val symbolicVariable = bv.sort().mkConst("symbolicVariable")

        solver.assert(symbolicVariable eq mkBvRotateRightExpr(bv, rotateSize))
        solver.check()

        val expectedResult = bv.numberValue.toBinary().let {
            val firstPart = it.substring(it.length - rotateSize.numberValue.toInt(), it.length)
            val secondPart = it.substring(0, it.length - rotateSize.numberValue.toInt())
            firstPart + secondPart
        }
        val actualResult = (solver.model().eval(symbolicVariable) as KBitVec64Value).numberValue.toBinary()

        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun testIndexedRotateRight(): Unit = with(context) {
        val bv = Random.nextLong().toBv()
        val rotateSize = Random.nextInt(from = 1, until = 4)

        val symbolicVariable = bv.sort().mkConst("symbolicVariable")

        solver.assert(symbolicVariable eq mkBvRotateRightIndexedExpr(rotateSize, bv))
        solver.check()

        val expectedResult = bv.numberValue.toBinary().let {
            val firstPart = it.substring(it.length - rotateSize, it.length)
            val secondPart = it.substring(0, it.length - rotateSize)
            firstPart + secondPart
        }
        val actualResult = (solver.model().eval(symbolicVariable) as KBitVec64Value).numberValue.toBinary()

        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun testMulExpr(): Unit = testBinaryOperation(context::mkBvMulExpr, Long::times)

    private fun testLogicalOperation(
        symbolicOperation: (KExpr<KBv64Sort>, KExpr<KBv64Sort>) -> KExpr<KBoolSort>,
        concreteOperation: (Long, Long) -> Boolean
    ): Unit = with(context) {
        val values = (0 until 2).map { Random.nextLong() }.sorted()
        val bvValues = values.map { it.toBv() }

        val withItselfConst = mkBoolSort().mkConst("withItself")
        val firstWithSecondConst = mkBoolSort().mkConst("firstWithSecond")
        val secondWithFirstConst = mkBoolSort().mkConst("secondWithFirst")

        val withItself = symbolicOperation(bvValues[0], bvValues[0]) eq withItselfConst
        val firstWithSecond = symbolicOperation(bvValues[0], bvValues[1]) eq firstWithSecondConst
        val secondWithFirst = symbolicOperation(bvValues[1], bvValues[0]) eq secondWithFirstConst

        solver.assert(withItself)
        solver.assert(firstWithSecond)
        solver.assert(secondWithFirst)

        solver.check()
        val model = solver.model()

        val expectedValues = listOf(
            concreteOperation(values[0], values[0]),
            concreteOperation(values[0], values[1]),
            concreteOperation(values[1], values[0])
        )

        val actualValues = listOf(
            model.eval(withItselfConst),
            model.eval(firstWithSecondConst),
            model.eval(secondWithFirstConst)
        )

        assertFalse("Values: $values") { expectedValues[0] xor (actualValues[0] is KTrue) }
        assertFalse("Values: $values") { expectedValues[1] xor (actualValues[1] is KTrue) }
        assertFalse("Values: $values") { expectedValues[2] xor (actualValues[2] is KTrue) }
    }

    @Test
    fun testUnsignedLessExpr(): Unit = testLogicalOperation(context::mkBvUnsignedLessExpr) { arg0: Long, arg1: Long ->
        arg0.toULong() < arg1.toULong()
    }

    @Test
    fun testSignedLessExpr(): Unit = testLogicalOperation(context::mkBvSignedLessExpr) { arg0: Long, arg1: Long ->
        arg0 < arg1
    }

    @Test
    fun testUnsignedLessOrEqualExpr(): Unit =
        testLogicalOperation(context::mkBvUnsignedLessOrEqualExpr) { arg0: Long, arg1: Long ->
            arg0.toULong() <= arg1.toULong()
        }

    @Test
    fun testSignedLessOrEqualExpr(): Unit =
        testLogicalOperation(context::mkBvSignedLessOrEqualExpr) { arg0: Long, arg1: Long ->
            arg0 <= arg1
        }

    @Test
    fun testUnsignedGreaterOrEqualExpr(): Unit =
        testLogicalOperation(context::mkBvUnsignedGreaterOrEqualExpr) { arg0: Long, arg1: Long ->
            arg0.toULong() >= arg1.toULong()
        }

    @Test
    fun testSignedGreaterOrEqualExpr(): Unit =
        testLogicalOperation(context::mkBvSignedGreaterOrEqualExpr) { arg0: Long, arg1: Long ->
            arg0 >= arg1
        }

    @Test
    fun testUnsignedGreaterExpr(): Unit =
        testLogicalOperation(context::mkBvUnsignedGreaterExpr) { arg0: Long, arg1: Long ->
            arg0.toULong() > arg1.toULong()
        }

    @Test
    fun testSignedGreaterExpr(): Unit = testLogicalOperation(context::mkBvSignedGreaterExpr) { arg0: Long, arg1: Long ->
        arg0 > arg1
    }
    private fun testSmallBinaryOperation(
        symbolicOperation: (KExpr<KBv32Sort>, KExpr<KBv32Sort>) -> KExpr<KBv32Sort>,
        concreteOperation: (Int, Int) -> Int
    ): Unit = with(context) {
        val negativeValue = Random.nextInt(from = Int.MIN_VALUE, until = 0)
        val positiveValue = Random.nextInt(from = 1, until = Int.MAX_VALUE)

        val negativeBv = negativeValue.toBv()
        val positiveBv = positiveValue.toBv()

        val firstResult = mkBv32Sort().mkConst("symbolicVariable")
        val secondResult = mkBv32Sort().mkConst("anotherSymbolicVariable")

        solver.assert(symbolicOperation(negativeBv, positiveBv) eq firstResult)
        solver.assert(symbolicOperation(positiveBv, negativeBv) eq secondResult)
        solver.check()

        val firstActualValue = (solver.model().eval(firstResult) as KBitVec32Value).numberValue
        val secondActualValue = (solver.model().eval(secondResult) as KBitVec32Value).numberValue

        val firstExpectedValue = concreteOperation(negativeValue, positiveValue)
        val secondExpectedValue = concreteOperation(positiveValue, negativeValue)

        assertEquals(firstExpectedValue, firstActualValue)
        assertEquals(secondExpectedValue, secondActualValue)
    }

    @Test
    fun testUnsignedDivExpr(): Unit = testSmallBinaryOperation(context::mkBvUnsignedDivExpr) { arg0: Int, arg1: Int ->
        (arg0.toUInt() / arg1.toUInt()).toInt()
    }

    @Test
    fun testUnsignedRemExpr(): Unit = testSmallBinaryOperation(context::mkBvUnsignedRemExpr) { arg0: Int, arg1: Int ->
        arg0.toUInt().rem(arg1.toUInt()).toInt()
    }
}