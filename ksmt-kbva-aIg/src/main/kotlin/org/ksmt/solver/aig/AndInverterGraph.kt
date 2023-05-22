package org.ksmt.solver.aig

import java.io.PrintStream

typealias Lit = Int

class AndInverterGraph(private val literalProvider: LiteralProvider) {

    private val outputs: HashSet<Lit> = HashSet()
    private val triplets: HashSet<Triple<Lit, Lit, Lit>> = HashSet()

    private fun norm(id: Lit): Lit {
        if (id == -1) return 0
        if (id == 1) return 1
        return if (id < 0) {
            2 * (-id) + 1
        } else {
            2 * id
        }
    }

    fun addOutput(id: Lit) {
        outputs.add(norm(id))
    }

    fun addEdge(c: Lit, a: Lit, b: Lit) {
        triplets.add(Triple(norm(c), norm(a), norm(b)))
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
        val inputs = HashSet<Lit>((2..literalProvider.currentLiteral).toList())
        triplets.forEach { (a, _, _) -> inputs.remove(a / 2) }
        ps.println("aag ${literalProvider.currentLiteral} ${inputs.size} ${0} ${1} ${triplets.size}")
        if (inputs.size > 0) inputs.sorted().forEach { ps.println(2 * it) }
        ps.println(outputs.first())
        triplets.forEach { (a, b, c) -> ps.println("$a $b $c") }
        return true
    }
}