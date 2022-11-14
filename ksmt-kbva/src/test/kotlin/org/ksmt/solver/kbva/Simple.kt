package org.ksmt.solver.kbva

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.ksmt.KContext
import org.ksmt.expr.KTrue
import org.ksmt.solver.KSolverStatus
import org.ksmt.sort.KArraySort
import org.ksmt.utils.getValue
import org.ksmt.utils.mkConst

class Simple {

    @Test
    @Suppress("USELESS_IS_CHECK")

    fun test() = with(KContext()) {
        val a = KTrue(this)
        val b by boolSort
        val c = a implies b

        val solver = KBVASolver(this)
        solver.assert(c)
        val status = solver.check()
        assertEquals(status, KSolverStatus.UNKNOWN)
    }
}
