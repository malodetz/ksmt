package org.ksmt.solver.aig

import org.ksmt.KContext
import org.ksmt.expr.rewrite.KVisitor;
import org.ksmt.expr.*
import org.ksmt.sort.KArithSort
import org.ksmt.sort.KBvSort
import org.ksmt.sort.KFpSort
import org.ksmt.sort.KSort
import kotlin.math.pow

@Suppress("UNCHECKED_CAST")
class ExprToAIGTransformer(
    private val ctx: KContext,
    private val aig: AndInverterGraph,
    private val literalProvider: LiteralProvider,
    builderId: String
) :
    KVisitor(builderId) {

    private val trueLiteral: Lit = 1
    private val falseLiteral: Lit = 0

    private fun getBitsOf(expr: KExpr<*>): MutableList<Lit> {
        return expr.cachedAccept(this) as MutableList<Lit>
    }

    private fun makeAnd(c: Lit, bits: List<Lit>) {
        val q = ArrayDeque(bits)
        while (bits.size > 2) {
            val a = q.removeFirst()
            val b = q.removeFirst()
            val t = literalProvider.newLiteral()
            aig.addEdge(t, a, b)
        }
        aig.addEdge(c, q[0], q[1])
    }

    private fun makeOr(c: Lit, bits: List<Lit>) {
        val q = ArrayDeque(bits)
        while (bits.size > 2) {
            val a = q.removeFirst()
            val b = q.removeFirst()
            val t = literalProvider.newLiteral()
            aig.addEdge(-t, -a, -b)
        }
        aig.addEdge(-c, -q[0], -q[1])
    }

    private fun makeEq(c: Lit, a: Lit, b: Lit) {
        val t1 = literalProvider.newLiteral()
        val t2 = literalProvider.newLiteral()
        aig.addEdge(t1, a, b)
        aig.addEdge(t2, -a, -b)
        aig.addEdge(-c, -t1, -t2)
    }

    private fun makeXor(c: Lit, a: Lit, b: Lit) {
        val t1 = literalProvider.newLiteral()
        val t2 = literalProvider.newLiteral()
        aig.addEdge(t1, a, b)
        aig.addEdge(t2, -a, -b)
        aig.addEdge(c, -t1, -t2)
    }

    private fun makeImplies(c: Lit, a: Lit, b: Lit) {
        aig.addEdge(-c, a, -b)
    }

    private fun makeNand(c: Lit, a: Lit, b: Lit) {
        aig.addEdge(-c, a, b)
    }

    private fun makeNor(c: Lit, a: Lit, b: Lit) {
        aig.addEdge(c, -a, -b)
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
        return mutableListOf(-a)
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
        return mutableListOf(trueLiteral)
    }

    override fun transform(expr: KFalse): MutableList<Lit> {
        return mutableListOf(falseLiteral)
    }

    override fun <T : KSort> transform(expr: KEqExpr<T>): MutableList<Lit> {
        val lhsBits = getBitsOf(expr.lhs)
        val rhsBits = getBitsOf(expr.rhs)
        return makeEquals(lhsBits, rhsBits)
    }

    private fun makeEquals(
        lhsBits: MutableList<Lit>,
        rhsBits: MutableList<Lit>
    ): MutableList<Lit> {
        val equalities = literalProvider.makeFreeBits(lhsBits.size)
        equalities.forEachIndexed { i, t -> makeEq(t, lhsBits[i], rhsBits[i]) }
        val p = literalProvider.newLiteral()
        makeAnd(p, equalities)
        return mutableListOf(p)
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
                makeOr(c, inequalities)
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
        val t1 = literalProvider.newLiteral()
        aig.addEdge(-t1, p, -x)
        aig.addOutput(t1)

        val equalities2 = mutableListOf<Lit>()
        repeat(n) {
            equalities2.add(literalProvider.newLiteral())
        }
        equalities2.forEachIndexed { i, t -> makeEq(t, c[i], b[i]) }
        val y = literalProvider.newLiteral()
        makeAnd(y, equalities2)
        val t2 = literalProvider.newLiteral()
        aig.addEdge(-t2, -p, y)
        aig.addOutput(t2)
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
        return expr.stringValue.map {
            if (it == '1') {
                trueLiteral
            } else {
                falseLiteral
            }
        }.toMutableList()
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
        return a.map { -it }.toMutableList()
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
    ) {
        val carry = literalProvider.makeFreeBits(n)
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
    }

    private fun makeNeg(
        a: MutableList<Lit>
    ): MutableList<Lit> {
        val n = a.size
        val res = literalProvider.makeFreeBits(n)
        val inv = a.map { -it }.toMutableList()
        val b = mutableListOf(trueLiteral)
        repeat(n - 1) {
            b.add(falseLiteral)
        }
        makeAddWithOverflowBit(n, inv, b, res)
        return res.asReversed()
    }

    override fun <T : KBvSort> transform(expr: KBvNegationExpr<T>): Any {
        val a = getBitsOf(expr.value).asReversed()
        return makeNeg(a)
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
        val a = getBitsOf(expr.arg0).asReversed()
        val b = getBitsOf(expr.arg1).asReversed()
        return makeSub(a, b)
    }

    private fun makeSub(
        a: MutableList<Lit>,
        b: MutableList<Lit>
    ): MutableList<Lit> {
        val n = a.size
        val c = makeNeg(b).asReversed()
        val bits = literalProvider.makeFreeBits(n)
        makeAddWithOverflowBit(n, a, c, bits)
        return bits.asReversed()
    }

    private fun mul(x: MutableList<Lit>, y: MutableList<Lit>, step: Int = 0): MutableList<Lit> {
        val n = x.size
        val zero = mutableListOf<Lit>()
        repeat(n) {
            zero.add(falseLiteral)
        }
        return if (step == n) {
            zero
        } else {
            val res = mutableListOf<Lit>()
            repeat(n) {
                res.add(literalProvider.newLiteral())
            }
            val a = mul(shiftRight(x, 1, false), y, step + 1)
            val b = mutableListOf<Lit>()
            repeat(n) {
                b.add(literalProvider.newLiteral())
            }
            makeIte(b, n, y[step], x, zero)
            makeAddWithOverflowBit(n, a, b, res)
            res
        }
    }

    override fun <T : KBvSort> transform(expr: KBvMulExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        return mul(a.asReversed(), b.asReversed()).asReversed()
    }

    private fun makeUnsignedDivRem(a: MutableList<Lit>, b: MutableList<Lit>): Pair<MutableList<Lit>, MutableList<Lit>> {
        val n = a.size
        for (i in 0 until n) {
            a.add(falseLiteral)
            b.add(falseLiteral)
        }
        val div = literalProvider.makeFreeBits(2 * n)
        val rem = literalProvider.makeFreeBits(2 * n)
        val t1 = mul(b, div.asReversed())
        makeAddWithOverflowBit(2 * n, t1, rem.asReversed(), a)

        val y = makeUnsignedGreaterOrEqual(rem, b.asReversed())
        aig.addOutput(-y)

        val z = makeUnsignedGreaterOrEqual(div, a.asReversed())
        aig.addOutput(-z)

        return Pair(div, rem)
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedDivExpr<T>): Any {
        val a = getBitsOf(expr.arg0).asReversed()
        val b = getBitsOf(expr.arg1).asReversed()
        val (div, _) = makeUnsignedDivRem(a.toMutableList(), b.toMutableList())
        return div.takeLast(a.size)
    }

    private fun makeAbs(a: MutableList<Lit>): MutableList<Lit> {
        val res = literalProvider.makeFreeBits(a.size)
        makeIte(res, a.size, a[0], makeNeg(a.asReversed()), a)
        return res
    }

    private fun makeSignedDiv(a: MutableList<Lit>, b: MutableList<Lit>): MutableList<Lit> {
        val n = a.size
        val x = makeAbs(a).asReversed()
        val y = makeAbs(b).asReversed()
        val div = makeUnsignedDivRem(x, y).first.takeLast(n).toMutableList()
        val c = literalProvider.newLiteral()
        makeXor(c, a[0], b[0])
        val res = literalProvider.makeFreeBits(n)
        makeIte(res, n, c, makeNeg(div.asReversed()), div)
        return res
    }

    override fun <T : KBvSort> transform(expr: KBvSignedDivExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        return makeSignedDiv(a.toMutableList(), b.toMutableList())
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedRemExpr<T>): Any {
        val a = getBitsOf(expr.arg0).asReversed()
        val b = getBitsOf(expr.arg1).asReversed()
        val (_, rem) = makeUnsignedDivRem(a.toMutableList(), b.toMutableList())
        return rem.takeLast(a.size)
    }

    override fun <T : KBvSort> transform(expr: KBvSignedRemExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val div = makeSignedDiv(a.toMutableList(), b.toMutableList())
        val qd = mul(b.asReversed(), div.asReversed())
        return makeSub(a.asReversed(), qd)
    }

    override fun <T : KBvSort> transform(expr: KBvSignedModExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val div = makeSignedDiv(a.toMutableList(), b.toMutableList())
        val qd = mul(b.asReversed(), div.asReversed())
        val rem = makeSub(a.asReversed(), qd)
        val n = a.size
        val x = literalProvider.makeFreeBits(n)
        makeAddWithOverflowBit(n, rem.asReversed(), b.asReversed(), x.asReversed())
        val y = literalProvider.makeFreeBits(n)
        val c = literalProvider.newLiteral()
        makeXor(c, a[0], b[0])
        makeIte(y, n, c, x, rem)
        return y
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedLessExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val res = makeUnsignedLess(b, a)
        return mutableListOf(res)
    }

    private fun makeUnsignedLess(
        b: MutableList<Lit>,
        a: MutableList<Lit>
    ): Lit {
        val res = literalProvider.newLiteral()
        val leq = makeUnsignedGreaterOrEqual(b, a)
        val eq = makeEquals(a, b).first()
        makeAnd(res, mutableListOf(leq, -eq))
        return res
    }

    override fun <T : KBvSort> transform(expr: KBvSignedLessExpr<T>): Any {
        return ctx.mkNot(ctx.mkBvSignedGreaterOrEqualExpr(expr.arg0, expr.arg1)).cachedAccept(this)
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedLessOrEqualExpr<T>): Any {
        return ctx.mkNot(ctx.mkBvUnsignedGreaterExpr(expr.arg0, expr.arg1)).cachedAccept(this)
    }

    override fun <T : KBvSort> transform(expr: KBvSignedLessOrEqualExpr<T>): Any {
        return ctx.mkNot(ctx.mkBvSignedGreaterExpr(expr.arg0, expr.arg1)).cachedAccept(this)
    }

    private fun makeUnsignedGreaterOrEqual(x: List<Lit>, y: List<Lit>): Lit {
        val res = literalProvider.newLiteral()
        if (x.size == 1) {
            makeImplies(res, y[0], x[0])
        } else {
            val a = literalProvider.newLiteral()
            makeAnd(a, mutableListOf(x[0], -y[0]))
            val b = literalProvider.newLiteral()
            makeEq(b, x[0], y[0])
            val t = makeUnsignedGreaterOrEqual(x.drop(1), y.drop(1))
            val c = literalProvider.newLiteral()
            makeAnd(c, mutableListOf(b, t))
            makeOr(res, mutableListOf(a, c))
        }
        return res
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedGreaterOrEqualExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        return mutableListOf(makeUnsignedGreaterOrEqual(a, b))
    }

    private fun makeNegativeGreaterOrEqual(x: List<Lit>, y: List<Lit>): Lit {
        val res = literalProvider.newLiteral()
        if (x.size == 1) {
            makeImplies(res, x[0], y[0])
        } else {
            val a = literalProvider.newLiteral()
            makeAnd(a, mutableListOf(-x[0], y[0]))
            val b = literalProvider.newLiteral()
            makeEq(b, x[0], y[0])
            val t = makeUnsignedGreaterOrEqual(x.drop(1), y.drop(1))
            val c = literalProvider.newLiteral()
            makeAnd(c, mutableListOf(b, t))
            makeOr(res, mutableListOf(a, c))
        }
        return res
    }

    private fun makeSignedGreaterOrEqual(
        a: MutableList<Lit>,
        b: MutableList<Lit>
    ): Lit {
        val res = literalProvider.newLiteral()

        val c = literalProvider.newLiteral()
        makeAnd(c, mutableListOf(-a[0], -b[0]))
        val d = literalProvider.newLiteral()
        makeAnd(d, mutableListOf(-a[0], b[0]))
        val e = literalProvider.newLiteral()
        makeAnd(e, mutableListOf(a[0], -b[0]))

        val h = mutableListOf(literalProvider.newLiteral())
        makeIte(
            h,
            1,
            e,
            mutableListOf(falseLiteral),
            mutableListOf(makeNegativeGreaterOrEqual(a, b))
        )
        val i = mutableListOf(literalProvider.newLiteral())
        makeIte(i, 1, d, mutableListOf(trueLiteral), h)
        makeIte(mutableListOf(res), 1, c, mutableListOf(makeUnsignedGreaterOrEqual(a, b)), i)

        return res
    }

    override fun <T : KBvSort> transform(expr: KBvSignedGreaterOrEqualExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)

        return mutableListOf(makeSignedGreaterOrEqual(a, b))
    }

    override fun <T : KBvSort> transform(expr: KBvUnsignedGreaterExpr<T>): Any {
        val a = ctx.mkNot(ctx.mkEq(expr.arg0, expr.arg1))
        return ctx.mkAnd(ctx.mkBvUnsignedGreaterOrEqualExpr(expr.arg0, expr.arg1), a).accept(this)
    }

    override fun <T : KBvSort> transform(expr: KBvSignedGreaterExpr<T>): Any {
        val a = ctx.mkNot(ctx.mkEq(expr.arg0, expr.arg1))
        return ctx.mkAnd(ctx.mkBvSignedGreaterOrEqualExpr(expr.arg0, expr.arg1), a).accept(this)

    }

    override fun transform(expr: KBvConcatExpr): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        return a + b
    }

    override fun transform(expr: KBvExtractExpr): Any {
        val a = getBitsOf(expr.value)
        val n = a.size
        return a.subList(n - expr.high - 1, n - expr.low)
    }

    override fun transform(expr: KBvSignExtensionExpr): Any {
        val a = getBitsOf(expr.value)
        val n = literalProvider.sizeBySort(expr.sort())
        val c = mutableListOf<Lit>()
        for (i in 0 until n - a.size) {
            c.add(a[0])
        }
        for (i in 0 until a.size) {
            c.add(a[i])
        }
        return c
    }

    override fun transform(expr: KBvZeroExtensionExpr): Any {
        val a = getBitsOf(expr.value)
        val n = literalProvider.sizeBySort(expr.sort())
        val c = mutableListOf<Lit>()
        for (i in 0 until n - a.size) {
            c.add(falseLiteral)
        }
        for (i in 0 until a.size) {
            c.add(a[i])
        }
        return c
    }

    override fun transform(expr: KBvRepeatExpr): Any {
        val a = getBitsOf(expr.value)
        val c = mutableListOf<Lit>()
        repeat(expr.repeatNumber) {
            for (i in 0 until a.size) {
                c.add(a[i])
            }
        }
        return c
    }

    private fun shiftLeft(a: MutableList<Lit>, n: Int): MutableList<Lit> {
        return if (n >= a.size) {
            MutableList(a.size) { falseLiteral }
        } else {
            (a.drop(n) + MutableList(a.size) { falseLiteral }).toMutableList()
        }
    }

    override fun <T : KBvSort> transform(expr: KBvShiftLeftExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val n = a.size
        var c = a
        b.asReversed().forEachIndexed { idx, lit ->
            val next = literalProvider.makeFreeBits(n)
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
        return if (n >= a.size) {
            MutableList(a.size) { falseLiteral }
        } else {
            val b = MutableList(a.size) { falseLiteral }
            for (i in n until a.size) {
                b[i] = a[i - n]
            }
            for (i in 0 until n) {
                if (isArithShift) {
                    b[i] = a[0]
                }
            }
            b
        }
    }

    override fun <T : KBvSort> transform(expr: KBvLogicalShiftRightExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val n = a.size
        var c = a
        b.asReversed().forEachIndexed { idx, lit ->
            val next = literalProvider.makeFreeBits(n)
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
            val next = literalProvider.makeFreeBits(n)
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
        val nn = n % a.size
        return (a.drop(nn) + a.take(nn)).toMutableList()
    }

    override fun <T : KBvSort> transform(expr: KBvRotateLeftExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val n = a.size
        var c = a
        b.asReversed().forEachIndexed { idx, lit ->
            val next = literalProvider.makeFreeBits(n)
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

    private fun rotateRight(a: MutableList<Lit>, n: Int): MutableList<Lit> {
        val nn = n % a.size
        return (a.takeLast(nn) + a.dropLast(nn)).toMutableList()
    }

    override fun <T : KBvSort> transform(expr: KBvRotateRightExpr<T>): Any {
        val a = getBitsOf(expr.arg0)
        val b = getBitsOf(expr.arg1)
        val n = a.size
        var c = a
        b.asReversed().forEachIndexed { idx, lit ->
            val next = literalProvider.makeFreeBits(n)
            val shift = (2.toDouble().pow(idx).toULong() % n.toULong()).toInt()
            val d = rotateRight(c, shift)
            makeIte(next, n, lit, d, c)
            c = next
        }
        return c
    }

    override fun <T : KBvSort> transform(expr: KBvRotateRightIndexedExpr<T>): Any {
        val a = getBitsOf(expr.value)
        val n = expr.rotationNumber
        return rotateRight(a, n)
    }

    override fun transform(expr: KBv2IntExpr): Any {
        TODO("Not yet implemented")
    }

    private fun makeSignedNoOverflow(a: MutableList<Lit>): MutableList<Lit> {
        val n = a.size
        val max = mutableListOf(falseLiteral, trueLiteral)
        repeat(n - 2) {
            max.add(falseLiteral)
        }
        val res = makeSignedGreaterOrEqual(a, max)
        return mutableListOf(-res)
    }

    private fun makeUnsignedNoOverflow(a: MutableList<Lit>): MutableList<Lit> {
        val n = a.size
        val max = mutableListOf(trueLiteral)
        repeat(n - 1) {
            max.add(falseLiteral)
        }
        val res = makeUnsignedGreaterOrEqual(a, max)
        return mutableListOf(-res)
    }

    private fun makeSignedNoUnderflow(a: MutableList<Lit>): MutableList<Lit> {
        val n = a.size
        val max = mutableListOf(trueLiteral, trueLiteral)
        repeat(n - 2) {
            max.add(falseLiteral)
        }
        return mutableListOf(makeSignedGreaterOrEqual(a, max))
    }

    private fun makeUnsignedNoUnderflow(a: MutableList<Lit>): MutableList<Lit> {
        val n = a.size
        val max = mutableListOf<Lit>()
        repeat(n) {
            max.add(falseLiteral)
        }
        return mutableListOf(makeSignedGreaterOrEqual(a, max))
    }

    override fun <T : KBvSort> transform(expr: KBvAddNoOverflowExpr<T>): Any {
        val a = getBitsOf(expr.arg0).toMutableList().asReversed()
        val b = getBitsOf(expr.arg1).toMutableList().asReversed()
        val n = a.size
        if (expr.isSigned) {
            a.add(a.last())
            b.add(b.last())
        } else {
            a.add(falseLiteral)
            b.add(falseLiteral)
        }
        val c = literalProvider.makeFreeBits(n + 1)
        makeAddWithOverflowBit(n + 1, a, b, c.asReversed())
        return if (expr.isSigned) {
            makeSignedNoOverflow(c)
        } else {
            makeUnsignedNoOverflow(c)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvAddNoUnderflowExpr<T>): Any {
        val a = getBitsOf(expr.arg0).toMutableList().asReversed()
        val b = getBitsOf(expr.arg1).toMutableList().asReversed()
        val n = a.size
        a.add(a.last())
        b.add(b.last())
        val c = literalProvider.makeFreeBits(n + 1)
        makeAddWithOverflowBit(n + 1, a, b, c.asReversed())
        return makeSignedNoUnderflow(c)
    }

    override fun <T : KBvSort> transform(expr: KBvSubNoOverflowExpr<T>): Any {
        val a = getBitsOf(expr.arg0).toMutableList().asReversed()
        val b = getBitsOf(expr.arg1).toMutableList().asReversed()
        a.add(a.last())
        b.add(b.last())
        val c = makeSub(a, b)
        return makeSignedNoOverflow(c)
    }

    override fun <T : KBvSort> transform(expr: KBvSubNoUnderflowExpr<T>): Any {
        val a = getBitsOf(expr.arg0).toMutableList().asReversed()
        val b = getBitsOf(expr.arg1).toMutableList().asReversed()
        if (expr.isSigned) {
            a.add(a.last())
            b.add(b.last())
        } else {
            a.add(falseLiteral)
            b.add(falseLiteral)
        }
        val c = makeSub(a, b)
        return if (expr.isSigned) {
            makeSignedNoUnderflow(c)
        } else {
            makeUnsignedNoUnderflow(c)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvDivNoOverflowExpr<T>): Any {
        val a = getBitsOf(expr.arg0).toMutableList()
        val b = getBitsOf(expr.arg1).toMutableList()
        val n = a.size
        val intMin = mutableListOf(trueLiteral)
        repeat(n - 1) {
            intMin.add(falseLiteral)
        }
        val x = makeEquals(a, intMin).first()
        val minusOne = mutableListOf<Lit>()
        repeat(n) {
            minusOne.add(trueLiteral)
        }
        val y = makeEquals(b, minusOne).first()
        val res = literalProvider.newLiteral()
        makeAnd(res, mutableListOf(x, y))
        return mutableListOf(-res)
    }

    override fun <T : KBvSort> transform(expr: KBvNegNoOverflowExpr<T>): Any {
        val a = getBitsOf(expr.value).toMutableList()
        val n = a.size
        val intMin = mutableListOf(trueLiteral)
        repeat(n - 1) {
            intMin.add(falseLiteral)
        }
        val res = makeEquals(a, intMin).first()
        return mutableListOf(-res)
    }

    override fun <T : KBvSort> transform(expr: KBvMulNoOverflowExpr<T>): Any {
        val a = getBitsOf(expr.arg0).toMutableList().asReversed()
        val b = getBitsOf(expr.arg1).toMutableList().asReversed()
        val n = a.size
        if (!expr.isSigned) {
            repeat(n) {
                a.add(falseLiteral)
                b.add(falseLiteral)
            }
            val c = mul(a, b).asReversed()
            val bits = c.subList(0, n)
            val res = literalProvider.newLiteral()
            makeOr(res, bits)
            return mutableListOf(-res)
        } else {
            val c = literalProvider.newLiteral()
            makeXor(c, a.last(), b.last())
            repeat(n) {
                a.add(a.last())
                b.add(b.last())
            }
            val d = mul(a, b).asReversed()
            val bits = d.subList(0, n + 1)
            val r = literalProvider.newLiteral()
            makeOr(r, bits)
            val res = literalProvider.newLiteral()
            makeImplies(res, -c, -r)
            return mutableListOf(res)
        }
    }

    override fun <T : KBvSort> transform(expr: KBvMulNoUnderflowExpr<T>): Any {
        val a = getBitsOf(expr.arg0).toMutableList().asReversed()
        val b = getBitsOf(expr.arg1).toMutableList().asReversed()
        val n = a.size
        val c = literalProvider.newLiteral()
        makeXor(c, a.last(), b.last())
        repeat(n) {
            a.add(a.last())
            b.add(b.last())
        }
        val d = mul(a, b).asReversed()
        val bits = d.subList(0, n + 1)
        val r = literalProvider.newLiteral()
        makeAnd(r, bits)
        val res = literalProvider.newLiteral()
        makeImplies(res, c, r)
        return mutableListOf(res)
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
