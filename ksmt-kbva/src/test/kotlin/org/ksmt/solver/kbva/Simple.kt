package org.ksmt.solver.kbva

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.ksmt.KContext
import org.ksmt.expr.KFalse
import org.ksmt.expr.KNotExpr
import org.ksmt.expr.KTrue
import org.ksmt.solver.KSolverStatus

class Simple {

    @Test
    @Suppress("USELESS_IS_CHECK")

    fun test() = with(KContext()) {
        val b = KFalse(this)
        val c = mkNot(b)

        val solver = KBVASolver(this)
        solver.assert(c)
        val status = solver.check()
        assertEquals(status, KSolverStatus.SAT)
    }
}
