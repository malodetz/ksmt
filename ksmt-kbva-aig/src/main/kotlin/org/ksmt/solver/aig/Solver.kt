package org.ksmt.solver.aig

import org.ksmt.KContext
import org.ksmt.expr.KExpr
import org.ksmt.solver.KModel
import org.ksmt.solver.KSolver
import org.ksmt.solver.KSolverStatus
import org.ksmt.sort.KBoolSort
import java.io.File
import java.io.PrintStream
import kotlin.time.Duration
import org.ksmt.solver.kissat.KissatSolver
import java.io.FileInputStream
import java.util.Scanner
import kotlin.math.absoluteValue
import kotlin.math.sign

class Solver(private val ctx: KContext) : KSolver {
    private val assertions: MutableList<KExpr<KBoolSort>> = mutableListOf()

    override fun assert(expr: KExpr<KBoolSort>) {
        assertions.add(expr)
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

    private fun parseDIMACS(dimacsFilePath: String, satSolver: KissatSolver) {
        val inputStream = FileInputStream(dimacsFilePath)
        val sc = Scanner(inputStream)
        sc.nextLine()
        repeat(2) { sc.next() }
        val vars = sc.nextInt()
        val clauses = sc.nextInt()
        repeat(vars) { satSolver.addVariable() }
        repeat(clauses) {
            val l = mutableListOf<Int>()
            var t = sc.nextInt()
            while (t != 0) {
                val x = t.absoluteValue + 2
                l.add(x * t.sign)
                t = sc.nextInt()
            }
            satSolver.addClause(l)
        }
        inputStream.close()
    }

    override fun check(timeout: Duration): KSolverStatus {
        val assertionConverter = AssertionConverter(ctx)
        val path = "ksmt-temp/"
        val aagFilePath = path + "output.aag"
        val dimacsFilePath = path + "output.dimacs"
        assertionConverter.assertionsToAAG(assertions, PrintStream(aagFilePath))
        val p = Runtime.getRuntime().exec("sh aig_to_simple_cnf.sh")
        p.waitFor()
        val satSolver = KissatSolver()
        parseDIMACS(dimacsFilePath, satSolver)
        var result: Boolean? = null
        if (timeout != Duration.INFINITE) {
            val t = Thread {
                result = satSolver.solve()
            }
            t.start()
            val partialTimeout = timeout.inWholeMilliseconds / 1000
            for (i in 0 until 1000) {
                Thread.sleep(partialTimeout)
                if (!t.isAlive || t.isInterrupted) {
                    break
                }
            }
            t.interrupt()
        } else {
            result = satSolver.solve()
        }
        return if (result == null) {
            KSolverStatus.UNKNOWN
        } else if (result!!) {
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