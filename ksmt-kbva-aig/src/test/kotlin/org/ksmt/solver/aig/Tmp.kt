package org.ksmt.solver.aig

import org.ksmt.KContext
import org.ksmt.utils.getValue

fun main() {

    val ctx = KContext()
    with(ctx){
        val assertionConverter = AssertionConverter(ctx)
        val a by boolSort
        val b by boolSort
        val c by boolSort
        val assertions = mutableListOf(mkOr(c,mkOr(a, b)))
        assertionConverter.assertionsToAAG(assertions)
    }
}