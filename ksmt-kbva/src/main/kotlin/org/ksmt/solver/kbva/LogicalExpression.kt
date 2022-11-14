package org.ksmt.solver.kbva

import org.kosat.Lit


abstract class LogicalExpression(val id: Lit) {
    val cnf: List<List<Lit>> by lazy { buildCNF() }

    constructor(literalProducer: LiteralProducer) : this(literalProducer.newLiteral())

    private fun buildCNF(): List<List<Lit>> {
        val result = ArrayList<ArrayList<Lit>>()
        traverse(result)
        return result
    }

    abstract fun traverse(cnf: ArrayList<ArrayList<Lit>>)

}

class SingleLiteral(literalProducer: LiteralProducer) : LogicalExpression(literalProducer) {
    override fun traverse(cnf: ArrayList<ArrayList<Lit>>) {}
}

class NotExpression(
    literalProducer: LiteralProducer,
    private val expr: LogicalExpression
) : LogicalExpression(literalProducer) {

    override fun traverse(cnf: ArrayList<ArrayList<Lit>>) {
        expr.traverse(cnf)

        val c = id
        val a = expr.id
        cnf.add(arrayListOf(-a, -c))
        cnf.add(arrayListOf(a, c))
    }

}

class AndExpression(
    literalProducer: LiteralProducer,
    private val expr1: LogicalExpression,
    private val expr2: LogicalExpression
) : LogicalExpression(literalProducer) {

    override fun traverse(cnf: ArrayList<ArrayList<Lit>>) {
        expr1.traverse(cnf)
        expr2.traverse(cnf)

        val c = id
        val a = expr1.id
        val b = expr2.id

        cnf.add(arrayListOf(-a, -b, c))
        cnf.add(arrayListOf(a, -c))
        cnf.add(arrayListOf(b, -c))
    }

}

class OrExpression(
    literalProducer: LiteralProducer,
    private val expr1: LogicalExpression,
    private val expr2: LogicalExpression
) : LogicalExpression(literalProducer) {

    override fun traverse(cnf: ArrayList<ArrayList<Lit>>) {
        expr1.traverse(cnf)
        expr2.traverse(cnf)

        val c = id
        val a = expr1.id
        val b = expr2.id

        cnf.add(arrayListOf(a, b, -c))
        cnf.add(arrayListOf(-a, c))
        cnf.add(arrayListOf(-b, c))
    }

}

class ImpliesExpression(
    literalProducer: LiteralProducer,
    private val expr1: LogicalExpression,
    private val expr2: LogicalExpression
) : LogicalExpression(literalProducer) {

    override fun traverse(cnf: ArrayList<ArrayList<Lit>>) {
        expr1.traverse(cnf)
        expr2.traverse(cnf)

        val c = id
        val a = expr1.id
        val b = expr2.id

        cnf.add(arrayListOf(-a, b, -c))
        cnf.add(arrayListOf(a, c))
        cnf.add(arrayListOf(-b, c))
    }

}

class EquivExpression(
    literalProducer: LiteralProducer,
    private val expr1: LogicalExpression,
    private val expr2: LogicalExpression
) : LogicalExpression(literalProducer) {

    override fun traverse(cnf: ArrayList<ArrayList<Lit>>) {
        expr1.traverse(cnf)
        expr2.traverse(cnf)

        val c = id
        val a = expr1.id
        val b = expr2.id

        cnf.add(arrayListOf(-a, -b, c))
        cnf.add(arrayListOf(a, b, c))
        cnf.add(arrayListOf(a, -b, -c))
        cnf.add(arrayListOf(-a, b, -c))
    }

}

class XorExpression(
    literalProducer: LiteralProducer,
    private val expr1: LogicalExpression,
    private val expr2: LogicalExpression
) : LogicalExpression(literalProducer) {
    override fun traverse(cnf: ArrayList<ArrayList<Lit>>) {
        expr1.traverse(cnf)
        expr2.traverse(cnf)

        val c = id
        val a = expr1.id
        val b = expr2.id

        cnf.add(arrayListOf(-a, -b, -c))
        cnf.add(arrayListOf(a, b, -c))
        cnf.add(arrayListOf(a, -b, c))
        cnf.add(arrayListOf(-a, b, c))
    }

}