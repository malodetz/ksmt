package org.ksmt.solver.aig

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.ksmt.KContext
import org.ksmt.solver.KSolverStatus
import org.ksmt.solver.z3.KZ3SMTLibParser
import java.io.*
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.minutes

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
    val benchPath = File("benchmarks")
    var total = 0
    var correct = 0
    var outtimed = 0
    benchPath.walk().filter { !it.isDirectory }.forEach {
        val ctx = KContext()
        val assertions = KZ3SMTLibParser(ctx).parse(it.toPath())
        val trueStatus = getRealStatus(it)
        if (trueStatus != KSolverStatus.UNKNOWN) {
            print(it.name)
            total += 1
            var isComplete = true
            var isCorrect = false
            val solver = Solver(ctx)
            val isAsserted = runBlocking {
                val job = async {
                    assertions.forEach { assertion -> solver.assert(assertion) }
                    solver.prepare()
                }
                val res = withTimeoutOrNull(1.minutes) {
                    job.await()
                    true
                } ?: false
                if (!job.isCompleted) {
                    job.cancel()
                }
                res
            }
            val time = measureTimeMillis {
                if (!isAsserted) {
                    isComplete = false
                } else {
                    val solverStatus = solver.check(timeout = 1.minutes)
                    if (solverStatus == trueStatus) {
                        isCorrect = true
                    } else if (solverStatus == KSolverStatus.UNKNOWN) {
                        isComplete = false
                    }
                }
            }
            if (!isAsserted) {
                outtimed += 1
                println(" ASSERTION_TIMEOUT")
            } else if (!isComplete) {
                outtimed +=1
                println(" SOLVING_TIMEOUT")
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