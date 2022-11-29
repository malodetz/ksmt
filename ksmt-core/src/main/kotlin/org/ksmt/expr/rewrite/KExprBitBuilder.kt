package org.ksmt.expr.rewrite

import org.ksmt.KContext
import org.ksmt.expr.*
import org.ksmt.sort.KSort

@Suppress("UNCHECKED_CAST")
class KExprBitBuilder(val ctx: KContext, builderId: String) : Builder(builderId) {

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

    override fun <T : KSort, A : KExpr<*>> transform(kApp: KApp<T, A>): MutableList<Lit> {
        // TODO  Something with args
        return literalProvider.makeBits(kApp)
    }

    override fun transform(expr: KConst<*>): MutableList<Lit> {
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

}
