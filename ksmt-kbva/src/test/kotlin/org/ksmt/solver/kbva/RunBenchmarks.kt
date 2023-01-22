package org.ksmt.solver.kbva

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.ksmt.KContext
import org.ksmt.solver.KSolverStatus
import org.ksmt.solver.z3.KZ3SMTLibParser
import org.ksmt.solver.z3.KZ3Solver
import org.ksmt.solver.bitwuzla.KBitwuzlaSolver
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.seconds


fun getRealStatus(file: File): KSolverStatus {
    try {
        val reader = BufferedReader(FileReader(file))
        var line = reader.readLine()
        while (line != null) {
            line = reader.readLine()
            if (line.startsWith("(set-info :status sat)")) {
                return KSolverStatus.SAT
            } else if (line.startsWith("(set-info :status unsat)")) {
                return KSolverStatus.UNSAT
            } else if (line.startsWith("(set-info :status")) {
                return KSolverStatus.UNKNOWN
            }
        }
    } catch (e: IOException) {
        return KSolverStatus.UNKNOWN
    }
    return KSolverStatus.UNKNOWN
}

fun main() {
    val benchPath = File("smtLibBenchmark/QF_BV")
    var total = 0
    var correct = 0
    var outtimed = 0
    benchPath.walk().filter { !it.isDirectory }.forEach {
        val ctx = KContext()
        val assertions = KZ3SMTLibParser(ctx).parse(it.toPath())
        val trueStatus = getRealStatus(it)
        if (trueStatus != KSolverStatus.UNKNOWN) {
            print(it)
            total += 1
            var isComplete = true
            var isCorrect = false
            val time = measureTimeMillis {
                val solver = KBVASolver(ctx, SolverType.KISSAT)
//                val solver = KBitwuzlaSolver(ctx)
                val isAsserted = runBlocking {
                    val job = async { assertions.forEach { assertion -> solver.assert(assertion) } }
                    val res = withTimeoutOrNull(30.seconds) {
                        job.await()
                        true
                    } ?: false
                    if (!job.isCompleted) {
                        job.cancel()
                    }
                    res
                }
                if (!isAsserted) {
                    isComplete = false
                } else {
                    val solverStatus = solver.check(timeout = 30.seconds)
                    if (solverStatus == trueStatus) {
                        isCorrect = true
                    } else if (solverStatus == KSolverStatus.UNKNOWN) {
                        isComplete = false
                    }
                }
            }
            if (!isComplete) {
                outtimed += 1
                println(" TIMEOUT")
            } else {
                if (isCorrect) {
                    correct += 1
                    print(" CORRECT ")
                } else {
                    print(" WRONG ")
                }
                println(time)
            }
        }
    }
    println("Total: $total")
    println("Timeout: $outtimed")
    println("Correct: $correct")
    println("Wrong: ${total - correct - outtimed}")
}