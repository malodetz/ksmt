package org.ksmt.solver.kbva

import org.ksmt.KContext
import org.ksmt.solver.KSolverStatus
import org.ksmt.solver.z3.KZ3SMTLibParser
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
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
            println(it)
            total += 1
            val solver = KBVASolver(ctx)
            assertions.forEach { assertion -> solver.assert(assertion) }
            val solverStatus = solver.check(timeout = 1.minutes)
            if (solverStatus == trueStatus) {
                correct += 1
            } else if (solverStatus == KSolverStatus.UNKNOWN) {
                outtimed += 1
            }
            println("$trueStatus $solverStatus")
        }
    }
    println(total)
    println(correct)
    println(outtimed)
}