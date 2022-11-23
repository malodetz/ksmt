package org.ksmt.expr.logicalexpression

import org.ksmt.expr.rewrite.Lit
import org.ksmt.expr.rewrite.LiteralProvider


typealias CNFList = MutableList<List<Lit>>

abstract class LogicalExpression(val id: Lit) {
    val cnf: CNFList by lazy { buildCNF() }

    constructor(literalProvider: LiteralProvider) : this(literalProvider.newLiteral())

    abstract fun buildCNF(): CNFList

}

open class SingleLiteral(id: Lit) : LogicalExpression(id) {

    constructor(literalProvider: LiteralProvider) : this(literalProvider.newLiteral())

    override fun buildCNF(): CNFList {
        return mutableListOf()
    }
}

class TrueLiteral(id: Lit) : SingleLiteral(id) {

    override fun buildCNF(): CNFList {
        return mutableListOf(mutableListOf(id))
    }
}

class FalseLiteral(id: Lit) : SingleLiteral(id) {

    override fun buildCNF(): CNFList {
        return mutableListOf(mutableListOf(-id))
    }
}


class NotExpression(
    literalProvider: LiteralProvider,
    private val expr: LogicalExpression
) : LogicalExpression(literalProvider) {

    override fun buildCNF(): CNFList {
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

    override fun buildCNF(): CNFList {
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

    override fun buildCNF(): CNFList {
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


    override fun buildCNF(): CNFList {
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

    override fun buildCNF(): CNFList {
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

    override fun buildCNF(): CNFList {
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