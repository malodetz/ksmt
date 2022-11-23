package org.ksmt.expr.rewrite

import org.ksmt.KContext
import org.ksmt.expr.*
import org.ksmt.expr.logicalexpression.*
import org.ksmt.sort.KBoolSort
import org.ksmt.sort.KSort

@Suppress("UNUSED_PARAMETER")
class KExprBitBuilder(val ctx: KContext) {

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

    private fun getBitOf(expr: KExpr<KBoolSort>): SingleLiteral {
        return literalProvider.expressionBits(expr)[0]
    }

    private fun uniteConditions(
        mainCondition: LogicalExpression,
        conditions: List<LogicalExpression?>
    ): LogicalExpression {
        var result = mainCondition
        conditions.forEach {
            if (it != null) {
                result = and(result, it)
            }
        }
        return result
    }

    fun varsNumber(): Int {
        return literalProvider.varsNumber()
    }

    fun transform(expr: KExpr<*>): LogicalExpression? {
        return expr.accept(this)
    }

    fun <T : KSort, A : KExpr<*>> transform(expr: KApp<T, A>): LogicalExpression? {
        if (expr.args.isNotEmpty()) {
            TODO("Do something")
        }
        literalProvider.expressionBits(expr)
        return null
    }

    fun transform(expr: KConst<*>): LogicalExpression? {
        literalProvider.expressionBits(expr)
        return null
    }

    fun transform(expr: KTrue): LogicalExpression {
        return TrueLiteral(getBitOf(expr).id)
    }

    fun transform(expr: KFalse): LogicalExpression {
        return FalseLiteral(getBitOf(expr).id)
    }

    fun transform(expr: KNotExpr): LogicalExpression {
        val condition = transform(expr.arg)

        val literal = getBitOf(expr)
        val otherLiteral = getBitOf(expr.arg)

        return uniteConditions(eq(literal, not(otherLiteral)), mutableListOf(condition))
    }

    fun transform(expr: KXorExpr): LogicalExpression {
        val conditions1 = transform(expr.a)
        val conditions2 = transform(expr.b)
        val bit1 = getBitOf(expr.a)
        val bit2 = getBitOf(expr.b)
        val bit = getBitOf(expr)

        return uniteConditions(eq(bit, xor(bit1, bit2)), mutableListOf(conditions1, conditions2))
    }

    fun transform(expr: KImpliesExpr): LogicalExpression {
        val conditions1 = transform(expr.p)
        val conditions2 = transform(expr.q)
        val bit1 = getBitOf(expr.p)
        val bit2 = getBitOf(expr.q)
        val bit = getBitOf(expr)

        return uniteConditions(eq(bit, implies(bit1, bit2)), mutableListOf(conditions1, conditions2))
    }

    fun transform(expr: KAndExpr): LogicalExpression {
        val conditions = expr.args.map { transform(it) }.toCollection(mutableListOf())
        val bits = expr.args.map { transform(it)!! }.toCollection(mutableListOf())

        val equality = eq(getBitOf(expr), uniteConditions(bits[0], bits.drop(1)))
        return uniteConditions(equality, conditions)
    }

    fun transform(expr: KOrExpr): LogicalExpression {
        val conditions = expr.args.map { transform(it) }.toCollection(mutableListOf())
        val bits = expr.args.map { transform(it)!! }.toCollection(mutableListOf())

        var disjunction = bits[0]
        bits.drop(1).forEach { disjunction = or(disjunction, it) }
        val equality = eq(getBitOf(expr), disjunction)
        return uniteConditions(equality, conditions)
    }


    fun <T : KSort> transform(expr: KEqExpr<T>): LogicalExpression {
        val conditions1 = transform(expr.lhs)
        val conditions2 = transform(expr.rhs)
        val bits1 = literalProvider.expressionBits(expr.lhs)
        val bits2 = literalProvider.expressionBits(expr.rhs)

        val equalities = bits1.zip(bits2).map { (a, b) -> eq(a, b) }
        val equality = eq(getBitOf(expr), uniteConditions(equalities[0], equalities.drop(1)))
        return uniteConditions(equality, mutableListOf(conditions1, conditions2))
    }

    fun <T : KSort> transform(expr: KDistinctExpr<T>): LogicalExpression {
        val inequalities = mutableListOf<KExpr<KBoolSort>>()
        for (i in 0 until expr.args.size) {
            for (j in i + 1 until expr.args.size) {
                inequalities.add(KNotExpr(ctx, KEqExpr(ctx, expr.args[i], expr.args[j])))
            }
        }
        return transform(KAndExpr(ctx, inequalities))
    }

    fun <T : KSort> transform(expr: KIteExpr<T>): LogicalExpression {
        val conditions1 = transform(expr.trueBranch)
        val conditions2 = transform(expr.falseBranch)
        val conditions3 = transform(expr.condition)
        val bits1 = literalProvider.expressionBits(expr.trueBranch)
        val bits2 = literalProvider.expressionBits(expr.falseBranch)
        val bits = literalProvider.expressionBits(expr)
        val p = getBitOf(expr.condition)

        val equalities1 = bits1.zip(bits).map { (a, b) -> eq(a, b) }
        val equality1 = uniteConditions(equalities1[0], equalities1.drop(1))

        val equalities2 = bits2.zip(bits).map { (a, b) -> eq(a, b) }
        val equality2 = uniteConditions(equalities2[0], equalities2.drop(1))

        val finalExpr = and(implies(p, equality1), implies(not(p), equality2))
        return uniteConditions(finalExpr, mutableListOf(conditions1, conditions2, conditions3))
    }

}