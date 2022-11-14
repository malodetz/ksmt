package org.ksmt.solver.kbva

import org.ksmt.KContext
import org.ksmt.expr.KExpr
import org.ksmt.solver.KModel
import org.ksmt.solver.KSolver
import org.ksmt.solver.KSolverStatus
import org.ksmt.sort.KBoolSort
import kotlin.time.Duration

open class KBVASolver(private val ctx: KContext) : KSolver {

    private val bitsOfExpr: HashMap<KExpr<*>, List<SingleLiteral>> = HashMap()

    private val currentCNF: ArrayList<List<Int>> = ArrayList()

    private val exprBuilder: KBVAExpressionBuilder = KBVAExpressionBuilder(bitsOfExpr)

    override fun assert(expr: KExpr<KBoolSort>) {
        val (_, conditions) = exprBuilder.transformExpression(expr)
        if (conditions != null) {
            val cnf = conditions.cnf
            currentCNF.addAll(cnf)
        }
    }

    override fun assertAndTrack(expr: KExpr<KBoolSort>): KExpr<KBoolSort> {
        TODO("Not yet implemented")
    }

    override fun push() {
        TODO("Not yet implemented")
    }

    override fun pop(n: UInt) {
        TODO("Not yet implemented")
    }

    override fun check(timeout: Duration): KSolverStatus {
        println(currentCNF)
//        val solver: Solver = Kosat(currentCNF)
        return KSolverStatus.UNKNOWN
    }

    override fun checkWithAssumptions(assumptions: List<KExpr<KBoolSort>>, timeout: Duration): KSolverStatus {
        TODO("Not yet implemented")
    }

    override fun model(): KModel {
        TODO("Not yet implemented")
    }

    override fun unsatCore(): List<KExpr<KBoolSort>> {
        TODO("Not yet implemented")
    }

    override fun reasonOfUnknown(): String {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}