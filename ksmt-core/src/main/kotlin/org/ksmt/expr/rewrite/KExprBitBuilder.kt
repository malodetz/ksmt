package org.ksmt.expr.rewrite

import org.ksmt.KContext
import org.ksmt.expr.*
import org.ksmt.sort.KArithSort
import org.ksmt.sort.KBvSort
import org.ksmt.sort.KFpSort
import org.ksmt.sort.KSort

@Suppress("UNCHECKED_CAST")
class KExprBitBuilder(val ctx: KContext, builderId: String) : KVisitor(builderId) {

    val cnf: MutableList<List<Lit>> = mutableListOf()

    private val literalProvider: LiteralProvider = LiteralProvider()

    private fun getBitsOf(expr: KExpr<*>): MutableList<Lit> {
        return expr.cachedAccept(this) as MutableList<Lit>
    }

    private fun makeAnd(c: Lit, bits: List<Lit>) {
        val clause = mutableListOf(c)
        bits.forEach { clause.add(-it) }
        cnf.add(clause)
        bits.forEach { cnf.add(mutableListOf(it, -c)) }
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

        cnf.add(mutableListOf(-a, -c))
        cnf.add(mutableListOf(a, c))
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
        val equalities = literalProvider.makeBits(expr.lhs)
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

    override fun <T : KSort> transform(expr: KIteExpr<T>): MutableList<Lit> {
        val a = getBitsOf(expr.trueBranch)
        val b = getBitsOf(expr.falseBranch)
        val p = getBitsOf(expr.condition).first()
        val c = literalProvider.makeBits(expr)

        val equalities1 = literalProvider.makeBits(expr)
        equalities1.forEachIndexed { i, t -> makeEq(t, c[i], a[i]) }
        val x = literalProvider.newLiteral()
        makeAnd(x, equalities1)
        cnf.add(mutableListOf(-p, x))

        val equalities2 = literalProvider.makeBits(expr)
        equalities2.forEachIndexed { i, t -> makeEq(t, c[i], b[i]) }
        val y = literalProvider.newLiteral()
        makeAnd(y, equalities2)
        cnf.add(mutableListOf(p, y))

        return c
    }

    override fun transform(expr: KBitVec1Value): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KBitVec8Value): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KBitVec16Value): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KBitVec32Value): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KBitVec64Value): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KBitVecCustomValue): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvNotExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvReductionAndExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvReductionOrExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvAndExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvOrExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvXorExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvNAndExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvNorExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvXNorExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvNegationExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvAddExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvSubExpr<T>): Any {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun transform(expr: KBvExtractExpr): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KBvSignExtensionExpr): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KBvZeroExtensionExpr): Any {
        TODO("Not yet implemented")
    }

    override fun transform(expr: KBvRepeatExpr): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvShiftLeftExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvLogicalShiftRightExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvArithShiftRightExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvRotateLeftExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvRotateLeftIndexedExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvRotateRightExpr<T>): Any {
        TODO("Not yet implemented")
    }

    override fun <T : KBvSort> transform(expr: KBvRotateRightIndexedExpr<T>): Any {
        TODO("Not yet implemented")
    }

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
