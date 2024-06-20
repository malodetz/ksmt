package org.ksmt.solver.kissat

import org.ksmt.solver.kissat.KissatSolver.Companion.TRUE
import org.ksmt.solver.kissat.KissatSolver.Companion.not
import kotlin.test.Test
import kotlin.test.assertEquals

class Example {

    @Test
    fun test() = KissatSolver().use { solver ->
        val a = solver.addVariable()
        val b = solver.addVariable()
        val c = solver.addVariable()

        solver.addClause(a)
        solver.addClause(not(b))
        solver.addClause(TRUE, c)

        val status = solver.solve()

        assertEquals(expected = true, status)

        val aValue = solver.getValue(a)
        val bValue = solver.getValue(b)
        val cValue = solver.getValue(c)

        assertEquals(expected = true, aValue)
        assertEquals(expected = false, bValue)
        assertEquals(expected = true, cValue)
    }
}
