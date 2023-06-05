package org.ksmt.solver.aig


import org.ksmt.KContext
import org.ksmt.expr.KExpr
import org.ksmt.sort.KBoolSort
import java.io.PrintStream

@Suppress("UNCHECKED_CAST")
class AssertionConverter(ctx: KContext) {

    private val literalProvider: LiteralProvider = LiteralProvider()
    private val aig: AndInverterGraph = AndInverterGraph(literalProvider)
    private val exprToAIGTransformer: ExprToAIGTransformer =
        ExprToAIGTransformer(ctx, aig, literalProvider, "AIGBuilder")

    fun assertionsToAAG(assertions: List<KExpr<KBoolSort>>, ps: PrintStream = System.out) : Boolean {
        assertions.forEach { expr ->
            val bits = expr.cachedAccept(exprToAIGTransformer) as MutableList<Lit>
            aig.addOutput(bits.first())
        }
        return aig.printAIGasASCII(ps)
    }
}
