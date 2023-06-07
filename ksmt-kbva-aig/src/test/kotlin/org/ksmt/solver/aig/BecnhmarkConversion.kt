package org.ksmt.solver.aig

import org.ksmt.KContext
import org.ksmt.solver.KSolverStatus
import org.ksmt.solver.z3.KZ3SMTLibParser
import java.io.*
import kotlin.time.Duration.Companion.minutes
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
    val benchPath = File("benchmarks")
    benchPath.walk().filter { !it.isDirectory }.forEach { it ->
        val trueStatus = getRealStatus(it)
        if (trueStatus != KSolverStatus.UNKNOWN) {
            val ctx = KContext()
            val assertions = KZ3SMTLibParser(ctx).parse(it.toPath())
            println(it.name)
            val solver = Solver(ctx)
            assertions.forEach { solver.assert(it) }
            val s1 = solver.check(timeout = 1.minutes)
            println("${s1} $trueStatus")
        }
    }
}