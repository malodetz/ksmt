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

@Suppress("UNCHECKED_CAST")
open class KBVASolver(private val ctx: KContext) : KSolver {

    private val currentCNF: MutableList<MutableList<Lit>> = mutableListOf()

    override fun assert(expr: KExpr<KBoolSort>) {
        val exprBuilder = KExprBitBuilder(ctx, "kbvaBuilder")
        val bits = expr.cachedAccept(exprBuilder) as MutableList<Lit>
        exprBuilder.cnf.forEach { currentCNF.add(it.toMutableList()) }
        currentCNF.add(bits)
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
        val solver = Kosat(currentCNF, currentCNF.maxOf { list -> list.maxOf { it } })
        val result = solver.solve()
        println(currentCNF)
        println(solver.getModel())
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