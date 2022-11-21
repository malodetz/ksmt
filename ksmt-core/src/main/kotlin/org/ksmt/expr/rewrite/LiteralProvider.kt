package org.ksmt.expr.rewrite

import org.ksmt.expr.KExpr
import org.ksmt.expr.SingleLiteral
import org.ksmt.sort.KBoolSort
import org.ksmt.sort.KBvSort
import org.ksmt.sort.KFpSort
import org.ksmt.sort.KSort

typealias Lit = Int

class LiteralProvider {
    private var bitCount: Lit = 0

    fun newLiteral(): Lit {
        return bitCount++
    }

    fun expressionBits(expr: KExpr<*>): List<SingleLiteral> {
        if (expr.expressionBits.isEmpty()) {
            val n = sizeBySort(expr.sort())
            repeat(n) {
                expr.expressionBits.add(SingleLiteral(this))
            }
        }
        return expr.expressionBits
    }

    private fun sizeBySort(sort: KSort): Int {
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
                error("No bit representation is define for this $sort")
            }
        }
    }
}