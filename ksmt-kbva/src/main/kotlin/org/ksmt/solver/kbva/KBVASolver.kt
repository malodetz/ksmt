package org.ksmt.solver.kbva

import org.kosat.Kosat
import org.kosat.Lit
import org.ksmt.KContext
import org.ksmt.expr.KExpr
import org.ksmt.expr.rewrite.KExprBitBuilder
import org.ksmt.solver.KModel
import org.ksmt.solver.KSolver
import org.ksmt.solver.KSolverStatus
import org.ksmt.sort.KBoolSort
import kotlin.time.Duration
import kotlinx.coroutines.*

open class KBVASolver(ctx: KContext) : KSolver {

    private val currentCNF: MutableList<MutableList<Lit>> = mutableListOf()

    private val exprBuilder: KExprBitBuilder = KExprBitBuilder(ctx)

    override fun assert(expr: KExpr<KBoolSort>) {
        val conditions = expr.accept(exprBuilder)
        conditions?.cnf?.forEach { currentCNF.add(it.toMutableList()) }
        currentCNF.add(mutableListOf(expr.expressionBits[0].id))
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
        val solver = Kosat(currentCNF, exprBuilder.varsNumber())
        val result = solver.solve()
        solver.getModel()
//        val result = runBlocking {
//            withTimeoutOrNull(timeout.inWholeMilliseconds) {
//                solver.solve()
//            }
//        } ?: return KSolverStatus.UNKNOWN
        return if (result) {
            KSolverStatus.SAT
        } else {
            KSolverStatus.UNSAT
        }
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