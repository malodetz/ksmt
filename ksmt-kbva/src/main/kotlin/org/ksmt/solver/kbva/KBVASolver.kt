package org.ksmt.solver.kbva

import org.kosat.Kosat
import org.kosat.Lit
import org.kosat.Solver
import org.ksmt.KContext
import org.ksmt.expr.KExpr
import org.ksmt.solver.KModel
import org.ksmt.solver.KSolver
import org.ksmt.solver.KSolverStatus
import org.ksmt.sort.KBoolSort
import kotlin.time.Duration

open class KBVASolver (private val ctx: KContext) : KSolver {

    private val solver : Solver = Kosat(ArrayList())

    private val bitsOfExpr : HashMap<KExpr<*>, List<Lit>> = HashMap()

    private val currentCNF : List<List<Int>> = ArrayList()

    private val exprBuilder: ExpressionBuilder = ExpressionBuilder(bitsOfExpr)

    override fun assert(expr: KExpr<KBoolSort>) {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
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