package org.ksmt.solver.aig

import java.io.PrintStream
import kotlin.math.absoluteValue

typealias Lit = Int

class AndInverterGraph(private val literalProvider: LiteralProvider) {

    private val outputs: HashSet<Lit> = HashSet()
    private val negated: HashSet<Lit> = HashSet()
    private val triplets: HashSet<Triple<Lit, Lit, Lit>> = HashSet()

    private fun norm(id: Lit): Lit {
        if (id == -1) return 0
        if (id == 1) return 1
        val x = if (negated.contains(id.absoluteValue)) {
            -id
        } else {
            id
        }
        return if (x < 0) {
            2 * (-x) - 1
        } else {
            2 * x - 2
        }
    }

    fun addOutput(id: Lit) {
        outputs.add(norm(id))
    }

    fun addEdge(c: Lit, a: Lit, b: Lit) {
        val x = norm(c)
        if (x % 2 != 0) {
            negated.add(x / 2 + 1)
            triplets.add(Triple(x - 1, norm(a), norm(b)))
        } else {
            triplets.add(Triple(x, norm(a), norm(b)))
        }
    }

    private fun normalizeOutputs() {
        while (outputs.size > 1) {
            val a = outputs.first()
            outputs.remove(a)
            val b = outputs.first()
            outputs.remove(b)
            val c = 2 * literalProvider.newLiteral()
            triplets.add(Triple(c, a, b))
            outputs.add(c)
        }
    }

    fun printAIGasASCII(ps: PrintStream): Boolean {
        normalizeOutputs()
        if (outputs.size == 0) return false
        val inputs = HashSet<Lit>((2 until 2 * literalProvider.currentLiteral step 2).toList())
        triplets.forEach { (a, _, _) -> inputs.remove(a - a % 2) }
        ps.println("aag ${literalProvider.currentLiteral - 1} ${inputs.size} ${0} ${1} ${triplets.size}")
        if (inputs.size > 0) inputs.sorted().forEach { ps.println(it) }
        ps.println(outputs.first())
        triplets.forEach { (a, b, c) -> ps.println("$a $b $c") }
        return true
    }
}