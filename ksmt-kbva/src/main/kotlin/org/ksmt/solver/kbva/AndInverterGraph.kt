package org.ksmt.solver.kbva

import java.io.PrintStream

typealias Lit = Int

class AndInverterGraph(private val literalProvider: LiteralProvider) {

    private val outputs: HashSet<Lit> = HashSet()
    private val triplets: HashSet<Triple<Lit, Lit, Lit>> = HashSet()

    private fun norm(id: Lit): Lit {
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
        triplets.add(Triple(norm(a), norm(b), norm(c)))
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

    fun printAIGasASCII(ps: PrintStream = System.out) {
        normalizeOutputs()
        val inputs = HashSet<Lit>((1..literalProvider.currentLiteral).toList())
        triplets.forEach { (a, _, _) -> inputs.remove(a / 2) }
        ps.println("${literalProvider.currentLiteral} ${inputs.size} ${0} ${1} ${outputs.size}")
        inputs.sorted().forEach { ps.println(it) }
        ps.println(outputs.first())
        triplets.forEach { (a, b, c) -> ps.println("$a $b $c") }
    }
}