package org.ksmt.expr.rewrite

import org.ksmt.expr.KExpr
import org.ksmt.sort.KBoolSort
import org.ksmt.sort.KBvSort
import org.ksmt.sort.KFpSort
import org.ksmt.sort.KSort

typealias Lit = Int

class LiteralProvider {
    private var bitCount: Lit = 1

    fun newLiteral(): Lit {
        return bitCount++
    }

//    fun varsNumber(): Int = bitCount - 1

    fun makeBits(expr: KExpr<*>): MutableList<Lit> {
        val result = mutableListOf<Lit>()
        val n = sizeBySort(expr.sort())
        repeat(n) {
            result.add(newLiteral())
        }
        return result
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
