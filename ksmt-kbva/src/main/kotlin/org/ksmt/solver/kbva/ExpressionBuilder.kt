package org.ksmt.solver.kbva

import org.kosat.Lit
import org.kosat.Solver
import org.ksmt.KContext
import org.ksmt.expr.KConst
import org.ksmt.expr.KExpr
import org.ksmt.expr.KTrue
import org.ksmt.sort.KBoolSort
import org.ksmt.utils.getValue

class LiteralProducer{
    private var bitCount: Int = 0

    fun newLiteral() : Lit {
        return bitCount++
    }
}

class ExpressionBuilder(
    private val bitsOfExpr: HashMap<KExpr<*>, List<Lit>>
) {

    private val literalProducer : LiteralProducer = LiteralProducer()

    fun transformExpression(expr: KExpr<*>): Pair<List<Lit>, LogicalExpression?> {
        return if (bitsOfExpr.containsKey(expr)) {
            Pair(bitsOfExpr[expr]!!, null)
        } else {
            val literalsAndExpr = transform(expr)
            bitsOfExpr[expr] = literalsAndExpr.first
            literalsAndExpr
        }
    }

    private fun transform(expr: KExpr<*>): Pair<List<Lit>, LogicalExpression?> =
        error("Expr $expr is not supported")

    private fun transform(expr: KConst<KBoolSort>): Pair<List<Lit>, LogicalExpression?> {
        val bit = literalProducer.newLiteral()
        return Pair(mutableListOf(bit), null)
    }

    private fun transform(expr: KTrue) {
        val bit = literalProducer.newLiteral()
    }

    private fun check() {
        val ctx = KContext()
        with(ctx){
            val a = mkBv(5)
        }
    }
}
