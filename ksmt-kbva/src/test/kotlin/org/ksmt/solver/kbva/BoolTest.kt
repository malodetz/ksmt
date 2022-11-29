package org.ksmt.solver.kbva

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.ksmt.KContext
import org.ksmt.solver.KSolverStatus

class BoolTest {

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

    @Test
    fun test5() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkImplies(mkTrue(), mkFalse()))
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }


    @Test
    fun test6() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkImplies(mkTrue(), mkTrue()))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test7() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkImplies(mkFalse(), mkTrue()))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test8() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkImplies(mkFalse(), mkFalse()))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test9() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkEq(mkFalse(), mkFalse()))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test10() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkEq(mkTrue(), mkTrue()))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test11() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkEq(mkTrue(), mkFalse()))
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }

    @Test
    fun test12() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkEq(mkFalse(), mkTrue()))
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }

    @Test
    fun test13() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkDistinct(mutableListOf(mkFalse(), mkFalse())))
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }

    @Test
    fun test14() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkDistinct(mutableListOf(mkFalse(), mkFalse())))
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }

    @Test
    fun test15() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkDistinct(mutableListOf(mkTrue(), mkFalse())))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test16() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkDistinct(mutableListOf(mkFalse(), mkTrue())))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test17() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkDistinct(mutableListOf(mkTrue())))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test18() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkDistinct(mutableListOf(mkTrue(), mkFalse(), mkTrue())))
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }

    @Test
    fun test19() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkXor(mkTrue(), mkFalse()))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }


    @Test
    fun test20() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkXor(mkTrue(), mkTrue()))
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }

    @Test
    fun test21() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkXor(mkFalse(), mkTrue()))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test22() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkXor(mkFalse(), mkFalse()))
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }

    @Test
    fun test23() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkAnd(mkTrue(), mkFalse()))
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }


    @Test
    fun test24() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkAnd(mkTrue(), mkTrue()))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test25() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkAnd(mkFalse(), mkTrue()))
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }

    @Test
    fun test26() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkAnd(mkFalse(), mkFalse()))
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }

    @Test
    fun test27() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkAnd(mkTrue()))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test28() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkAnd(mkFalse()))
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }

    @Test
    fun test29() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkOr(mkTrue(), mkFalse()))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test30() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkOr(mkTrue(), mkTrue()))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test31() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkOr(mkFalse(), mkTrue()))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test32() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkOr(mkFalse(), mkFalse()))
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }

    @Test
    fun test33() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkOr(mkTrue()))
        val status = solver.check()
        assertEquals(KSolverStatus.SAT, status)
    }

    @Test
    fun test34() = with(KContext()) {
        val solver = KBVASolver(this)
        solver.assert(mkOr(mkFalse()))
        val status = solver.check()
        assertEquals(KSolverStatus.UNSAT, status)
    }
}
