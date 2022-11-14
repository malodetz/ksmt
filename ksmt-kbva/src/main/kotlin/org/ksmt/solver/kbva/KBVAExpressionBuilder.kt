package org.ksmt.solver.kbva

import org.kosat.Lit
import org.ksmt.expr.*
import org.ksmt.sort.KBoolSort


class LiteralProducer {
    private var bitCount: Int = 0

    fun newLiteral(): Lit {
        return bitCount++
    }
}

@Suppress("UNUSED_PARAMETER")
class KBVAExpressionBuilder(
    private val bitsOfExpr: HashMap<KExpr<*>, List<SingleLiteral>>
) {

    private val literalProducer: LiteralProducer = LiteralProducer()

    fun transformExpression(expr: KExpr<*>): Pair<List<SingleLiteral>, LogicalExpression?> {
        return if (bitsOfExpr.containsKey(expr)) {
            Pair(bitsOfExpr[expr]!!, null)
        } else {
            val literalsAndExpr = transform(expr)
            bitsOfExpr[expr] = literalsAndExpr.first
            literalsAndExpr
        }
    }

    private fun transform(expr: KExpr<*>): Pair<List<SingleLiteral>, LogicalExpression?> {
        TODO("Not yet implemented")
    }

    private fun not(expr: LogicalExpression): LogicalExpression {
        return NotExpression(literalProducer, expr)
    }

    private fun and(expr1: LogicalExpression, expr2: LogicalExpression): LogicalExpression {
        return AndExpression(literalProducer, expr1, expr2)
    }

    private fun or(expr1: LogicalExpression, expr2: LogicalExpression): LogicalExpression {
        return OrExpression(literalProducer, expr1, expr2)
    }

    private fun xor(expr1: LogicalExpression, expr2: LogicalExpression): LogicalExpression {
        return XorExpression(literalProducer, expr1, expr2)
    }

    private fun eq(expr1: LogicalExpression, expr2: LogicalExpression): LogicalExpression {
        return EquivExpression(literalProducer, expr1, expr2)
    }

    private fun implies(expr1: LogicalExpression, expr2: LogicalExpression): LogicalExpression {
        return ImpliesExpression(literalProducer, expr1, expr2)
    }

    fun transform(expr: KConst<KBoolSort>): Pair<List<SingleLiteral>, LogicalExpression?> {
        val bit = SingleLiteral(literalProducer)
        return Pair(mutableListOf(bit), null)
    }

    fun transform(expr: KTrue): Pair<List<SingleLiteral>, LogicalExpression?> {
        val literal = SingleLiteral(literalProducer)
        return Pair(mutableListOf(literal), literal)
    }

    fun transform(expr: KFalse): Pair<List<SingleLiteral>, LogicalExpression?> {
        val literal = SingleLiteral(literalProducer)
        return Pair(mutableListOf(literal), not(literal))
    }

    fun transform(expr: KNotExpr): Pair<List<SingleLiteral>, LogicalExpression?> {
        val (bits, conds) = transformExpression(expr.arg)
        val bit = bits[0]
        val literal = SingleLiteral(literalProducer)

        var newConds = eq(literal, not(bit))
        if (conds != null) {
            newConds = and(newConds, conds)
        }
        return Pair(mutableListOf(literal), newConds)
    }


    fun transform(expr: KXorExpr): Pair<List<SingleLiteral>, LogicalExpression?> {
        val (bits1, conds1) = transformExpression(expr.a)
        val bit1 = bits1[0]

        val (bits2, conds2) = transformExpression(expr.b)
        val bit2 = bits2[0]

        val literal = SingleLiteral(literalProducer)
        var newConds = eq(literal, xor(bit1, bit2))
        if (conds1 != null) {
            newConds = and(newConds, conds1)
        }
        if (conds2 != null) {
            newConds = and(newConds, conds2)
        }
        return Pair(mutableListOf(literal), newConds)
    }

    fun transform(expr: KImpliesExpr): Pair<List<SingleLiteral>, LogicalExpression?> {
        val (bits1, conds1) = transformExpression(expr.p)
        val bit1 = bits1[0]

        val (bits2, conds2) = transformExpression(expr.q)
        val bit2 = bits2[0]

        val literal = SingleLiteral(literalProducer)
        var newConds = eq(literal, implies(bit1, bit2))
        if (conds1 != null) {
            newConds = and(newConds, conds1)
        }
        if (conds2 != null) {
            newConds = and(newConds, conds2)
        }
        return Pair(mutableListOf(literal), newConds)
    }

}
