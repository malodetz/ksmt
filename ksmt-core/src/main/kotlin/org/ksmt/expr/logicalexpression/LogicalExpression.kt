package org.ksmt.expr.logicalexpression

import org.ksmt.expr.rewrite.Lit
import org.ksmt.expr.rewrite.LiteralProvider


abstract class LogicalExpression(val id: Lit) {
    val cnf: MutableList<List<Lit>> by lazy { buildCNF() }

    constructor(literalProvider: LiteralProvider) : this(literalProvider.newLiteral())

    abstract fun buildCNF(): MutableList<List<Lit>>

}

class SingleLiteral(literalProvider: LiteralProvider) : LogicalExpression(literalProvider) {
    override fun buildCNF(): MutableList<List<Lit>> {
        return mutableListOf()
    }
}

class NotExpression(
    literalProvider: LiteralProvider,
    private val expr: LogicalExpression
) : LogicalExpression(literalProvider) {

    override fun buildCNF(): MutableList<List<Lit>> {
        val cnf = expr.cnf

        val c = id
        val a = expr.id
        cnf.add(arrayListOf(-a, -c))
        cnf.add(arrayListOf(a, c))
        return cnf
    }

}

class AndExpression(
    literalProvider: LiteralProvider,
    private val expr1: LogicalExpression,
    private val expr2: LogicalExpression
) : LogicalExpression(literalProvider) {

    override fun buildCNF(): MutableList<List<Lit>> {
        val cnf = expr1.cnf
        cnf.addAll(expr2.cnf)
        val c = id
        val a = expr1.id
        val b = expr2.id

        cnf.add(arrayListOf(-a, -b, c))
        cnf.add(arrayListOf(a, -c))
        cnf.add(arrayListOf(b, -c))
        return cnf
    }

}

class OrExpression(
    literalProvider: LiteralProvider,
    private val expr1: LogicalExpression,
    private val expr2: LogicalExpression
) : LogicalExpression(literalProvider) {

    override fun buildCNF(): MutableList<List<Lit>> {
        val cnf = expr1.cnf
        cnf.addAll(expr2.cnf)

        val c = id
        val a = expr1.id
        val b = expr2.id

        cnf.add(arrayListOf(a, b, -c))
        cnf.add(arrayListOf(-a, c))
        cnf.add(arrayListOf(-b, c))
        return cnf
    }

}

class ImpliesExpression(
    literalProvider: LiteralProvider,
    private val expr1: LogicalExpression,
    private val expr2: LogicalExpression
) : LogicalExpression(literalProvider) {


    override fun buildCNF(): MutableList<List<Lit>> {
        val cnf = expr1.cnf
        cnf.addAll(expr2.cnf)

        val c = id
        val a = expr1.id
        val b = expr2.id

        cnf.add(arrayListOf(-a, b, -c))
        cnf.add(arrayListOf(a, c))
        cnf.add(arrayListOf(-b, c))
        return cnf
    }

}

class EquivExpression(
    literalProvider: LiteralProvider,
    private val expr1: LogicalExpression,
    private val expr2: LogicalExpression
) : LogicalExpression(literalProvider) {

    override fun buildCNF(): MutableList<List<Lit>> {
        val cnf = expr1.cnf
        cnf.addAll(expr2.cnf)

        val c = id
        val a = expr1.id
        val b = expr2.id

        cnf.add(arrayListOf(-a, -b, c))
        cnf.add(arrayListOf(a, b, c))
        cnf.add(arrayListOf(a, -b, -c))
        cnf.add(arrayListOf(-a, b, -c))
        return cnf
    }

}

class XorExpression(
    literalProvider: LiteralProvider,
    private val expr1: LogicalExpression,
    private val expr2: LogicalExpression
) : LogicalExpression(literalProvider) {

    override fun buildCNF(): MutableList<List<Lit>> {
        val cnf = expr1.cnf
        cnf.addAll(expr2.cnf)

        val c = id
        val a = expr1.id
        val b = expr2.id

        cnf.add(arrayListOf(-a, -b, -c))
        cnf.add(arrayListOf(a, b, -c))
        cnf.add(arrayListOf(a, -b, c))
        cnf.add(arrayListOf(-a, b, c))
        return cnf
    }

}