package org.ksmt.expr.rewrite

import org.ksmt.expr.*
import org.ksmt.expr.logicalexpression.*


class KExprBitBuilder {

    private val literalProvider: LiteralProvider = LiteralProvider()

    private fun not(expr: LogicalExpression): LogicalExpression {
        return NotExpression(literalProvider, expr)
    }

    private fun and(expr1: LogicalExpression, expr2: LogicalExpression): LogicalExpression {
        return AndExpression(literalProvider, expr1, expr2)
    }

    private fun or(expr1: LogicalExpression, expr2: LogicalExpression): LogicalExpression {
        return OrExpression(literalProvider, expr1, expr2)
    }

    private fun xor(expr1: LogicalExpression, expr2: LogicalExpression): LogicalExpression {
        return XorExpression(literalProvider, expr1, expr2)
    }

    private fun eq(expr1: LogicalExpression, expr2: LogicalExpression): LogicalExpression {
        return EquivExpression(literalProvider, expr1, expr2)
    }

    private fun implies(expr1: LogicalExpression, expr2: LogicalExpression): LogicalExpression {
        return ImpliesExpression(literalProvider, expr1, expr2)
    }

    fun transform(expr: KExpr<*>): Any = error("transformer is not implemented for this type of expression")

    fun transform(expr: KConst<*>): LogicalExpression? {
        literalProvider.expressionBits(expr)
        return null
    }

    fun transform(expr: KTrue): LogicalExpression {
        return literalProvider.expressionBits(expr)[0]
    }

    fun transform(expr: KFalse): LogicalExpression {
        return not(literalProvider.expressionBits(expr)[0])
    }

    fun transform(expr: KNotExpr): LogicalExpression? {
        val condition = transform(expr.arg)

        val bit = bits[0]
        val literal = SingleLiteral(literalProvider)

        var newConds = eq(literal, not(bit))
        if (conds != null) {
            newConds = and(newConds, conds)
        }
        return Pair(mutableListOf(literal), newConds)
    }


//    fun transform(expr: KXorExpr): LogicalExpression? {
//        val (bits1, conds1) = transformExpression(expr.a)
//        val bit1 = bits1[0]
//
//        val (bits2, conds2) = transformExpression(expr.b)
//        val bit2 = bits2[0]
//
//        val literal = SingleLiteral(literalProvider)
//        var newConds = eq(literal, xor(bit1, bit2))
//        if (conds1 != null) {
//            newConds = and(newConds, conds1)
//        }
//        if (conds2 != null) {
//            newConds = and(newConds, conds2)
//        }
//        return Pair(mutableListOf(literal), newConds)
//    }
//
//    fun transform(expr: KImpliesExpr): LogicalExpression? {
//        val (bits1, conds1) = transformExpression(expr.p)
//        val bit1 = bits1[0]
//
//        val (bits2, conds2) = transformExpression(expr.q)
//        val bit2 = bits2[0]
//
//        val literal = SingleLiteral(literalProvider)
//        var newConds = eq(literal, implies(bit1, bit2))
//        if (conds1 != null) {
//            newConds = and(newConds, conds1)
//        }
//        if (conds2 != null) {
//            newConds = and(newConds, conds2)
//        }
//        return Pair(mutableListOf(literal), newConds)
//    }


}