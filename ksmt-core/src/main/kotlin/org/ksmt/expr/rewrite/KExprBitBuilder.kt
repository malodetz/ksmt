package org.ksmt.expr.rewrite

import org.ksmt.KContext
import org.ksmt.expr.*
import org.ksmt.sort.KArithSort
import org.ksmt.sort.KBvSort
import org.ksmt.sort.KFpSort
import org.ksmt.sort.KSort
import kotlin.math.pow

@Suppress("UNCHECKED_CAST")
class KExprBitBuilder(private val ctx: KContext, private val literalProvider: LiteralProvider, builderId: String) :
    KVisitor(builderId) {

    val cnf: MutableList<List<Lit>> = mutableListOf()

    private fun getBitsOf(expr: KExpr<*>): MutableList<Lit> {
        return expr.cachedAccept(this) as MutableList<Lit>
    }

    private fun makeAnd(c: Lit, bits: List<Lit>) {
        val clause = mutableListOf(c)
        bits.forEach { clause.add(-it) }
        cnf.add(clause)
        bits.forEach { cnf.add(mutableListOf(it, -c)) }
    }

    private fun makeNot(c: Lit, a: Lit) {
        cnf.add(mutableListOf(-a, -c))
        cnf.add(mutableListOf(a, c))
    }

    private fun makeOr(c: Lit, bits: List<Lit>) {
        val clause = mutableListOf(-c)
        clause.addAll(bits)
        cnf.add(clause)
        bits.forEach { cnf.add(mutableListOf(-it, c)) }
    }

    private fun makeEq(c: Lit, a: Lit, b: Lit) {
        cnf.add(mutableListOf(-a, -b, c))
        cnf.add(mutableListOf(a, b, c))
        cnf.add(mutableListOf(a, -b, -c))
        cnf.add(mutableListOf(-a, b, -c))
    }

    private fun makeXor(c: Lit, a: Lit, b: Lit) {
        cnf.add(mutableListOf(-a, -b, -c))
        cnf.add(mutableListOf(a, b, -c))
        cnf.add(mutableListOf(a, -b, c))
        cnf.add(mutableListOf(-a, b, c))
    }

    private fun makeImplies(c: Lit, a: Lit, b: Lit) {
        cnf.add(mutableListOf(-a, b, -c))
        cnf.add(mutableListOf(a, c))
        cnf.add(mutableListOf(-b, c))
    }

    private fun makeNand(c: Lit, a: Lit, b: Lit) {
        cnf.add(mutableListOf(-a, -b, -c))
        cnf.add(mutableListOf(a, c))
        cnf.add(mutableListOf(b, c))
    }

    private fun makeNor(c: Lit, a: Lit, b: Lit) {
        cnf.add(mutableListOf(a, b, c))
        cnf.add(mutableListOf(-a, -c))
        cnf.add(mutableListOf(-b, -c))
    }

    fun <T : KSort, A : KExpr<*>> transform(kApp: KApp<T, A>): MutableList<Lit> {
        // TODO  Something with args
        return literalProvider.makeBits(kApp)
    }

    override fun <T : KSort> transform(expr: KFunctionApp<T>): Any {
        // TODO  Something with args
        return literalProvider.makeBits(expr)
    }

    override fun <T : KSort> transform(expr: KConst<T>): MutableList<Lit> {
        return literalProvider.makeBits(expr)
    }

    override fun transform(expr: KAndExpr): MutableList<Lit> {
        val bits = expr.args.map { getBitsOf(it).first() }
        val p = literalProvider.makeBits(expr)
        val c = p.first()
        makeAnd(c, bits)
        return p
    }

    override fun transform(expr: KOrExpr): MutableList<Lit> {
        val bits = expr.args.map { getBitsOf(it).first() }
        val p = literalProvider.makeBits(expr)
        val c = p.first()
        makeOr(c, bits)
        return p
    }

    override fun transform(expr: KNotExpr): MutableList<Lit> {
        val a = getBitsOf(expr.arg).first()
        val p = literalProvider.makeBits(expr)
        val c = p.first()
        makeNot(c, a)
        return p
    }

    override fun transform(expr: KImpliesExpr): MutableList<Lit> {
        val a = getBitsOf(expr.p).first()
        val b = getBitsOf(expr.q).first()
        val p = literalProvider.makeBits(expr)
        val c = p.first()
        makeImplies(c, a, b)
        return p
    }

    override fun transform(expr: KXorExpr): MutableList<Lit> {
        val a = getBitsOf(expr.a).first()
        val b = getBitsOf(expr.b).first()
        val p = literalProvider.makeBits(expr)
        val c = p.first()
        makeXor(c, a, b)
        return p
    }

    override fun transform(expr: KTrue): MutableList<Lit> {
        val p = literalProvider.makeBits(expr)
        cnf.add(p)
        return p
    }

    override fun transform(expr: KFalse): MutableList<Lit> {
        val p = literalProvider.makeBits(expr)
        cnf.add(mutableListOf(-p.first()))
        return p
    }

    override fun <T : KSort> transform(expr: KEqExpr<T>): MutableList<Lit> {
        val lhsBits = getBitsOf(expr.lhs)
        val rhsBits = getBitsOf(expr.rhs)
        val p = literalProvider.makeBits(expr)
        val c = p.first()
        val equalities = mutableListOf<Lit>()
        repeat(lhsBits.size) {
            equalities.add(literalProvider.newLiteral())
        }
        equalities.forEachIndexed { i, t -> makeEq(t, lhsBits[i], rhsBits[i]) }
        makeAnd(c, equalities)
        return p
    }

    override fun <T : KSort> transform(expr: KDistinctExpr<T>): MutableList<Lit> {
        val p = literalProvider.makeBits(expr)
        val total = mutableListOf<Lit>()
        for (i in 0 until expr.args.size) {
            for (j in i + 1 until expr.args.size) {
                val a = getBitsOf(expr.args[i])
                val b = getBitsOf(expr.args[j])
                val c = literalProvider.newLiteral()

                val inequalities = literalProvider.makeBits(expr.args[i])
                inequalities.forEachIndexed { idx, t -> makeXor(t, a[idx], b[idx]) }
                makeAnd(c, inequalities)
                total.add(c)
            }
        }
        makeAnd(p.first(), total)
        return p
    }

    private fun makeIte(
        c: MutableList<Lit>,
        n: Int,
        p: Lit,
        a: MutableList<Lit>,
        b: MutableList<Lit>
    ) {
        val equalities1 = mutableListOf<Lit>()
        repeat(n) {
            equalities1.add(literalProvider.newLiteral())
        }
        equalities1.forEachIndexed { i, t -> makeEq(t, c[i], a[i]) }
        val x = literalProvider.newLiteral()
        makeAnd(x, equalities1)
        cnf.add(mutableListOf(-p, x))

        val equalities2 = mutableListOf<Lit>()
        repeat(n) {
            equalities2.add(literalProvider.newLiteral())
        }
        equalities2.forEachIndexed { i, t -> makeEq(t, c[i], b[i]) }
        val y = literalProvider.newLiteral()
        makeAnd(y, equalities2)
        cnf.add(mutableListOf(p, y))
    }

    override fun <T : KSort> transform(expr: KIteExpr<T>): MutableList<Lit> {
        val a = getBitsOf(expr.trueBranch)
        val b = getBitsOf(expr.falseBranch)
        val p = getBitsOf(expr.condition).first()
        val c = literalProvider.makeBits(expr)

        makeIte(c, a.size, p, a, b)

        return c
    }

    private fun makeBvFromStringValue(expr: KBitVecValue<*>): List<Lit> {
        val a = literalProvider.makeBits(expr)
        a.forEachIndexed { index, lit ->
            if (expr.stringValue[index] == '1') {
                cnf.add(mutableListOf(lit))
            } else {
                cnf.add(mutableListOf(-lit))
            }
        }
        return a
    }


    override fun transform(expr: KBitVec1Value): Any {
        return makeBvFromStringValue(expr)
    }

    override fun transform(expr: KBitVec8Value): Any {
        return makeBvFromStringValue(expr)
    }

    override fun transform(expr: KBitVec16Value): Any {
        return makeBvFromStringValue(expr)
    }

    override fun transform(expr: KBitVec32Value): Any {
        return makeBvFromStringValue(expr)
    }

    override fun transform(expr: KBitVec64Value): Any {
        return makeBvFromStringValue(expr)
    }

    override fun transform(expr: KBitVecCustomValue): Any {
        return makeBvFromStringValue(expr)
    }

    override fun <T : KBvSort> transform(expr: KBvNotExpr<T>): Any {
        val a = getBitsOf(expr.value)
        val c = literalProvider.makeBits(expr)
        c.zip(a).forEach { (bit1, bit2) -> makeNot(bit1, bit2) }
        return c
    }

    override fun <T : KBvSort> transform(expr: KBvReductionAndExpr<T>): Any {
        val a = getBitsOf(expr.value)
        val c = literalProvider.makeBits(expr)
        makeAnd(c.first(), a)
        return c
    }

    override fun <T : KBvSort> transform(expr: KBvReductionOrExpr<T>): Any {
        val a = getBitsOf(expr.value)
        val c = literalProvider.makeBits(expr)
        makeOr(c.first(), a)
        return c
    }

    override fun <T : KBvSort> transform(expr: KBvAndExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val c = literalProvider.makeBits(expr)
        c.forEachIndexed { i, t -> makeAnd(t, mutableListOf(a[i], b[i])) }
        return c
    }

    override fun <T : KBvSort> transform(expr: KBvOrExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val c = literalProvider.makeBits(expr)
        c.forEachIndexed { i, t -> makeOr(t, mutableListOf(a[i], b[i])) }
        return c
    }

    override fun <T : KBvSort> transform(expr: KBvXorExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val c = literalProvider.makeBits(expr)
        c.forEachIndexed { i, t -> makeXor(t, a[i], b[i]) }
        return c
    }

    override fun <T : KBvSort> transform(expr: KBvNAndExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val c = literalProvider.makeBits(expr)
        c.forEachIndexed { i, t -> makeNand(t, a[i], b[i]) }
        return c
    }

    override fun <T : KBvSort> transform(expr: KBvNorExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val c = literalProvider.makeBits(expr)
        c.forEachIndexed { i, t -> makeNor(t, a[i], b[i]) }
        return c
    }

    override fun <T : KBvSort> transform(expr: KBvXNorExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val c = literalProvider.makeBits(expr)
        c.forEachIndexed { i, t -> makeEq(t, a[i], b[i]) }
        return c
    }

    private fun makeAddWithOverflowBit(
        n: Int,
        a: MutableList<Lit>,
        b: MutableList<Lit>,
        c: MutableList<Lit>
    ): Lit {
        val carry = mutableListOf<Lit>()
        for (i in 1..n) {
            carry.add(literalProvider.newLiteral())
        }

        makeAnd(carry[0], mutableListOf(a[0], b[0]))
        makeXor(c[0], a[0], b[0])
        for (i in 1 until n) {
            val a1 = literalProvider.newLiteral()
            val a2 = literalProvider.newLiteral()
            val a3 = literalProvider.newLiteral()
            makeAnd(a1, mutableListOf(a[i], b[i]))
            makeAnd(a2, mutableListOf(a[i], carry[i - 1]))
            makeAnd(a3, mutableListOf(carry[i - 1], b[i]))
            makeOr(carry[i], mutableListOf(a1, a2, a3))

            val a4 = literalProvider.newLiteral()
            makeXor(a4, a[i], b[i])
            makeXor(c[i], a4, carry[i - 1])
        }

        return carry.last()
    }

    override fun <T : KBvSort> transform(expr: KBvNegationExpr<T>): Any {
        val a = getBitsOf(expr.value).asReversed()
        val c = literalProvider.makeBits(expr)
        val n = c.size
        val inv = a.map {
            val bit = literalProvider.newLiteral()
            makeNot(bit, it)
            bit
        }.toMutableList()
        val b = mutableListOf<Lit>()
        for (i in 1..n) {
            val lit = literalProvider.newLiteral()
            b.add(lit)
            cnf.add(listOf(if (i == 1) lit else -lit))
        }
        makeAddWithOverflowBit(n, inv, b, c)
        return c.asReversed()
    }

    override fun <T : KBvSort> transform(expr: KBvAddExpr<T>): Any {
        val a = getBitsOf(expr.arg0).asReversed()
        val b = getBitsOf(expr.arg1).asReversed()
        val c = literalProvider.makeBits(expr)
        val n = c.size
        makeAddWithOverflowBit(n, a, b, c)
        return c.asReversed()
    }

    override fun <T : KBvSort> transform(expr: KBvSubExpr<T>): Any {
        val a = ctx.mkBvNegationExpr(expr.arg1)
        val b = ctx.mkBvAddExpr(expr.arg0, a)
        return b.cachedAccept(this)
    }

    override fun <T : KBvSort> transform(expr: KBvMulExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedDivExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvSignedDivExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedRemExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvSignedRemExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvSignedModExpr<T>): Any {
        TODO("Not yet implemented")
    }

    /**
     *
     */

    override fun <T : KBvSort> transform(expr: KBvUnsignedLessExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvSignedLessExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedLessOrEqualExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvSignedLessOrEqualExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedGreaterOrEqualExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvSignedGreaterOrEqualExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedGreaterExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvSignedGreaterExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KBvConcatExpr): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val c = literalProvider.makeBits(expr)
        for (i in 0 until a.size) {
            val eq = literalProvider.newLiteral()
            makeEq(eq, a[i], c[i])
            cnf.add(mutableListOf(eq))
        }
        for (i in 0 until b.size) {
            val eq = literalProvider.newLiteral()
            makeEq(eq, b[i], c[a.size + i])
            cnf.add(mutableListOf(eq))
        }
        return c
    }

    override fun transform(expr: KBvExtractExpr): Any {
        val a = getBitsOf(expr.value)
        val n = a.size
        val b = a.subList(n - expr.high - 1, n - expr.low)
        val c = literalProvider.makeBits(expr)
        c.zip(b).forEach { (b1, b2) ->
            val eq = literalProvider.newLiteral()
            makeEq(eq, b1, b2)
            cnf.add(mutableListOf(eq))
        }
        return c
    }

    override fun transform(expr: KBvSignExtensionExpr): Any {
        val a = getBitsOf(expr.value)
        val c = literalProvider.makeBits(expr)
        for (i in 0 until c.size - a.size) {
            val eq = literalProvider.newLiteral()
            makeEq(eq, a[0], c[i])
            cnf.add(mutableListOf(eq))
        }
        for (i in 0 until a.size) {
            val eq = literalProvider.newLiteral()
            makeEq(eq, a[i], c[c.size - a.size + i])
            cnf.add(mutableListOf(eq))
        }
        return c
    }

    override fun transform(expr: KBvZeroExtensionExpr): Any {
        val a = getBitsOf(expr.value)
        val c = literalProvider.makeBits(expr)
        for (i in 0 until c.size - a.size) {
            cnf.add(mutableListOf(-c[i]))
        }
        for (i in 0 until a.size) {
            val eq = literalProvider.newLiteral()
            makeEq(eq, a[i], c[c.size - a.size + i])
            cnf.add(mutableListOf(eq))
        }
        return c
    }

    override fun transform(expr: KBvRepeatExpr): Any {
        val a = getBitsOf(expr.value)
        val c = literalProvider.makeBits(expr)
        repeat(expr.repeatNumber) {
            for (i in 0 until a.size) {
                val eq = literalProvider.newLiteral()
                makeEq(eq, a[i], c[it * a.size + i])
                cnf.add(mutableListOf(eq))
            }
        }
        return c
    }

    private fun shiftLeft(a: MutableList<Lit>, n: Int): MutableList<Lit> {
        val b = mutableListOf<Lit>()
        repeat(a.size) {
            b.add(literalProvider.newLiteral())
        }
        if (n >= a.size) {
            b.forEach { cnf.add(mutableListOf(-it)) }
        } else {
            for (i in 0 until a.size - n) {
                val eq = literalProvider.newLiteral()
                makeEq(eq, a[i + n], b[i])
                cnf.add(mutableListOf(eq))
            }
            for (i in a.size - n until a.size) {
                cnf.add(mutableListOf(-b[i]))
            }
        }
        return b
    }

    override fun <T : KBvSort> transform(expr: KBvShiftLeftExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val n = a.size
        var c = a
        b.asReversed().forEachIndexed { idx, lit ->
            val next = mutableListOf<Lit>()
            repeat(n) {
                next.add(literalProvider.newLiteral())
            }
            val shift = 2.toDouble().pow(idx).toInt()
            val d = shiftLeft(c, shift)
            makeIte(next, n, lit, d, c)
            c = next
            if (shift >= n) {
                return@forEachIndexed
            }
        }
        return c
    }

    private fun shiftRight(a: MutableList<Lit>, n: Int, isArithShift: Boolean): MutableList<Lit> {
        val b = mutableListOf<Lit>()
        repeat(a.size) {
            b.add(literalProvider.newLiteral())
        }
        if (n >= a.size) {
            b.forEach { cnf.add(mutableListOf(-it)) }
        } else {
            for (i in n until a.size) {
                val eq = literalProvider.newLiteral()
                makeEq(eq, a[i - n], b[i])
                cnf.add(mutableListOf(eq))
            }
            for (i in 0 until n) {
                if (isArithShift) {
                    val eq = literalProvider.newLiteral()
                    makeEq(eq, a[0], b[i])
                    cnf.add(mutableListOf(eq))
                } else {
                    cnf.add(mutableListOf(-b[i]))
                }
            }
        }
        return b
    }

    override fun <T : KBvSort> transform(expr: KBvLogicalShiftRightExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val n = a.size
        var c = a
        b.asReversed().forEachIndexed { idx, lit ->
            val next = mutableListOf<Lit>()
            repeat(n) {
                next.add(literalProvider.newLiteral())
            }
            val shift = 2.toDouble().pow(idx).toInt()
            val d = shiftRight(c, shift, false)
            makeIte(next, n, lit, d, c)
            c = next
            if (shift >= n) {
                return@forEachIndexed
            }
        }
        return c
    }

    override fun <T : KBvSort> transform(expr: KBvArithShiftRightExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val n = a.size
        var c = a
        b.asReversed().forEachIndexed { idx, lit ->
            val next = mutableListOf<Lit>()
            repeat(n) {
                next.add(literalProvider.newLiteral())
            }
            val shift = 2.toDouble().pow(idx).toInt()
            val d = shiftRight(c, shift, true)
            makeIte(next, n, lit, d, c)
            c = next
            if (shift >= n) {
                return@forEachIndexed
            }
        }
        return c
    }

    private fun rotateLeft(a: MutableList<Lit>, n: Int): MutableList<Lit> {
        val b = mutableListOf<Lit>()
        repeat(a.size) {
            b.add(literalProvider.newLiteral())
        }
        val nn = n % a.size
        b.zip(a.drop(nn) + a.take(nn)).forEach { (x, y) ->
            val eq = literalProvider.newLiteral()
            makeEq(eq, x, y)
            cnf.add(mutableListOf(eq))
        }
        return b
    }

    override fun <T : KBvSort> transform(expr: KBvRotateLeftExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val n = a.size
        var c = a
        b.asReversed().forEachIndexed { idx, lit ->
            val next = mutableListOf<Lit>()
            repeat(n) {
                next.add(literalProvider.newLiteral())
            }
            val shift = (2.toDouble().pow(idx).toULong() % n.toULong()).toInt()
            val d = rotateLeft(c, shift)
            makeIte(next, n, lit, d, c)
            c = next
        }
        return c
    }

    override fun <T : KBvSort> transform(expr: KBvRotateLeftIndexedExpr<T>): Any {
        val a = getBitsOf(expr.value)
        val n = expr.rotationNumber
        return rotateLeft(a, n)
    }

    override fun <T : KBvSort> transform(expr: KBvRotateRightExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvRotateRightIndexedExpr<T>): Any {
        TODO("Not yet implemented")
    }

    /*
    *
    *
    *
    * */

    override fun transform(expr: KBv2IntExpr): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvAddNoOverflowExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvAddNoUnderflowExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvSubNoOverflowExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvSubNoUnderflowExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvDivNoOverflowExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvNegNoOverflowExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvMulNoOverflowExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvMulNoUnderflowExpr<T>): Any {
        TODO("Not yet implemented")
    }


    override fun transform(expr: KFp16Value): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KFp32Value): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KFp64Value): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KFp128Value): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KFpCustomSizeValue): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KFpRoundingModeExpr): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpAbsExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpNegationExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpAddExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpSubExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpMulExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpDivExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpFusedMulAddExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpSqrtExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpRemExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpRoundToIntegralExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpMinExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpMaxExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpLessOrEqualExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpLessExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpGreaterOrEqualExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpGreaterExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpEqualExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpIsNormalExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpIsSubnormalExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpIsZeroExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpIsInfiniteExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpIsNaNExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpIsNegativeExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpIsPositiveExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpToBvExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpToRealExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpToIEEEBvExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpFromBvExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KFpToFpExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KRealToFpExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KFpSort> transform(expr: KBvToFpExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <D : KSort, R : KSort> transform(expr: KArrayStore<D, R>): Any {
        TODO("Not yet implemented")
    }

    override fun <D : KSort, R : KSort> transform(expr: KArraySelect<D, R>): Any {
        TODO("Not yet implemented")
    }

    override fun <D : KSort, R : KSort> transform(expr: KArrayConst<D, R>): Any {
        TODO("Not yet implemented")
    }

    override fun <D : KSort, R : KSort> transform(expr: KFunctionAsArray<D, R>): Any {
        TODO("Not yet implemented")
    }

    override fun <D : KSort, R : KSort> transform(expr: KArrayLambda<D, R>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KArithSort<T>> transform(expr: KAddArithExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KArithSort<T>> transform(expr: KMulArithExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KArithSort<T>> transform(expr: KSubArithExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KArithSort<T>> transform(expr: KUnaryMinusArithExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KArithSort<T>> transform(expr: KDivArithExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KArithSort<T>> transform(expr: KPowerArithExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KArithSort<T>> transform(expr: KLtArithExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KArithSort<T>> transform(expr: KLeArithExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KArithSort<T>> transform(expr: KGtArithExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KArithSort<T>> transform(expr: KGeArithExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KModIntExpr): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KRemIntExpr): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KToRealIntExpr): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KInt32NumExpr): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KInt64NumExpr): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KIntBigNumExpr): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KToIntRealExpr): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KIsIntRealExpr): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KRealNumExpr): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KExistentialQuantifier): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KUniversalQuantifier): Any {
        TODO("Not yet implemented")
    }

}
