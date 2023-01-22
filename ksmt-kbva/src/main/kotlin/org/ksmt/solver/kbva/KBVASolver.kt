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
import org.kosat.Solver
import org.ksmt.expr.rewrite.LiteralProvider
import org.ksmt.solver.kissat.KissatSolver
import org.ksmt.solver.model.KModelImpl

enum class SolverType {
    CDCL, KISSAT
}

@Suppress("UNCHECKED_CAST")
open class KBVASolver(private val ctx: KContext, solverType: SolverType) : KSolver {

    private val satSolver: Solver = if (solverType == SolverType.CDCL) {
        Kosat(mutableListOf())
    } else {
        KissatSolver()
    }
    private val currentCNF: MutableList<MutableList<Lit>> = mutableListOf()
    private val literalProvider: LiteralProvider = LiteralProvider(ctx, satSolver)

    override fun assert(expr: KExpr<KBoolSort>) {
        val exprBuilder = KExprBitBuilder(ctx, literalProvider, "kbvaVisitor")
        val bits = expr.cachedAccept(exprBuilder) as MutableList<Lit>
        exprBuilder.cnf.forEach {
            currentCNF.add(it.toMutableList())
            satSolver.addClause(it.toMutableList())
        }
        currentCNF.add(bits)
        satSolver.addClause(bits)
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
        var result: Boolean? = null
        val t = Thread {
            result = satSolver.solve()
        }
        t.start()
        val partialTimeout = timeout.inWholeMilliseconds / 1000
        for (i in 0 until 1000){
            Thread.sleep(partialTimeout)
            if (!t.isAlive || t.isInterrupted){
                break
            }
        }
        t.interrupt()
        return if (result == null) {
            KSolverStatus.UNKNOWN
        } else if (result!!) {
            KSolverStatus.SAT
        } else {
            KSolverStatus.UNSAT
        }
    }

    override fun checkWithAssumptions(assumptions: List<KExpr<KBoolSort>>, timeout: Duration): KSolverStatus {
        val exprBuilder = KExprBitBuilder(ctx, literalProvider, "kbvaVisitor")
        val assumptionClause = mutableListOf<Lit>()
        assumptions.forEach { assumption ->
            val bits = assumption.cachedAccept(exprBuilder) as MutableList<Lit>
            exprBuilder.cnf.forEach { satSolver.addClause(it.toMutableList()) }
            assumptionClause.add(bits.first())
        }
        var result: Boolean? = null
        val t = Thread {
            result = satSolver.solve(assumptionClause)
        }
        t.start()
        val partialTimeout = timeout.inWholeMilliseconds / 1000
        for (i in 0 until 1000){
            Thread.sleep(partialTimeout)
            if (!t.isAlive || t.isInterrupted){
                break
            }
        }
        t.interrupt()
        return if (result == null) {
            KSolverStatus.UNKNOWN
        } else if (result!!) {
            KSolverStatus.SAT
        } else {
            KSolverStatus.UNSAT
        }
    }

    override fun model(): KModel {
        val model = satSolver.getModel()
        return KModelImpl(ctx, literalProvider.getFuncInterpretationFromSolution(model))
    }

    override fun unsatCore(): List<KExpr<KBoolSort>> {
        TODO("Not yet implemented")
    }

    override fun reasonOfUnknown(): String {
        TODO("Not yet implemented")
    }

    override fun close() {}
}