package org.ksmt.solver.aig

import org.ksmt.expr.KExpr
import org.ksmt.decl.KDecl
import org.ksmt.expr.KConst
import org.ksmt.sort.*


class LiteralProvider() {

    private val expressionToBits: HashMap<KDecl<*>, List<Lit>> = hashMapOf()
    var currentLiteral: Lit = 1

    fun newLiteral(): Lit {
        currentLiteral++
        return currentLiteral
    }

    fun makeBits(expr: KExpr<*>): MutableList<Lit> {
        val n = sizeBySort(expr.sort())
        val result = makeFreeBits(n)
        if (expr is KConst) {
            expressionToBits[expr.decl] = result
        }
        return result
    }

    fun makeFreeBits(n: Int): MutableList<Lit> {
        val result = mutableListOf<Lit>()
        repeat(n) {
            result.add(newLiteral())
        }
        return result
    }

    fun sizeBySort(sort: KSort): Int {
        return when (sort) {
            is KBoolSort -> {
                1
            }

            is KBvSort -> {
                sort.sizeBits.toInt()
            }

            is KFpSort -> {
                (sort.exponentBits + sort.significandBits).toInt()
            }

            else -> {
                error("No bit representation is define for $sort")
            }
        }
    }
}
