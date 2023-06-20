package org.ksmt.solver.aig

import org.ksmt.KContext
import org.ksmt.solver.KSolverStatus
import org.ksmt.solver.z3.KZ3SMTLibParser
import java.io.*

fun main() {
    val benchPath = File("benchmarks")
    benchPath.walk().filter { !it.isDirectory }.forEach {
        val trueStatus = getRealStatus(it)
        if (trueStatus != KSolverStatus.UNKNOWN) {
            val ctx = KContext()
            val assertions = KZ3SMTLibParser(ctx).parse(it.toPath())
            val assertionConverter = AssertionConverter(ctx)
            val aagFilePath = "aigs/${it.nameWithoutExtension}.aag"
            File(aagFilePath).createNewFile()
            assertionConverter.assertionsToAAG(assertions, PrintStream(aagFilePath))
            val process = ProcessBuilder("./convert.sh", it.nameWithoutExtension).start()
            process.waitFor()
            println("${it.name} $trueStatus")
        }
    }
}