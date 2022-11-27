package org.ksmt.solver.kbva

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.ksmt.KContext
import org.ksmt.solver.KSolverStatus

class Simple {

    @Test
    fun test1() = with(KContext()) {
        val a = mkTrue()

        val solver = KBVASolver(this)
        solver.assert(a)
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test2() = with(KContext()) {
        val a = mkFalse()
        val solver = KBVASolver(this)
        solver.assert(a)
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }

    @Test
    fun test3() = with(KContext()) {
        val a = mkTrue()
        val solver = KBVASolver(this)
        solver.assert(mkNot(a))
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }

    @Test
    fun test4() = with(KContext()) {
        val a = mkFalse()
        val solver = KBVASolver(this)
        solver.assert(mkNot(a))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }
}
