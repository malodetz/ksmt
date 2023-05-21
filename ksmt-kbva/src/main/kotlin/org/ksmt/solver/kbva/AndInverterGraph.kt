package org.ksmt.solver.kbva

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

    private fun normalizeOutputs(){
        while (outputs.size > 1){
            val a = outputs.first()
            outputs.remove(a)
            val b = outputs.first()
            outputs.remove(b)
            val c = 2 * literalProvider.newLiteral()
            triplets.add(Triple(c, a, b))
            outputs.add(c)
        }
    }

    fun printAIGasASCII() {

    }
}