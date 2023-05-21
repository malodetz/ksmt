package org.ksmt.solver.kbva

import org.ksmt.KContext
import org.ksmt.expr.KExpr
import org.ksmt.sort.KBoolSort

class AssertionConverter(ctx: KContext) {

    private val literalProvider: LiteralProvider = LiteralProvider(ctx)
    private val aig: AndInverterGraph = AndInverterGraph(literalProvider)
    private val exprToAIGTransformer: ExprToAIGTransformer =
        ExprToAIGTransformer(ctx, aig, literalProvider, "AIGBuilder")

    fun assertionsToAAG(assertions: List<KExpr<KBoolSort>>) {
        assertions.forEach {expr ->
            val bits = expr.cachedAccept(exprToAIGTransformer) as MutableList<Lit>
            aig.addOutput(bits.first())
        }
        aig.printAIGasASCII()
    }
}
