package org.ksmt.solver.kissat

object Utils {
    fun unsupported(): Nothing = error("Not supported")
    fun unsupportedByKissat(): Nothing = error("Not supported by the Kissat")
    fun unsupportedIncSolving(): Nothing = error(INC_SOLVING_UNSUPPORTED)
    const val INC_SOLVING_UNSUPPORTED = "Incremental solving not supported"
}
