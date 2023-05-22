package org.ksmt.solver.aig

import org.ksmt.KContext
import org.ksmt.solver.KSolverStatus
import org.ksmt.solver.z3.KZ3SMTLibParser
import java.io.*

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
    benchPath.walk().filter { !it.isDirectory }.forEach {
        val ctx = KContext()
        val assertions = KZ3SMTLibParser(ctx).parse(it.toPath())
        val trueStatus = getRealStatus(it)
        val assertionConverter = AssertionConverter(ctx)
        val aagFilePath = "aigs/${it.nameWithoutExtension}.aag"
        File(aagFilePath).createNewFile()
        assertionConverter.assertionsToAAG(assertions, PrintStream(aagFilePath))
    }
}