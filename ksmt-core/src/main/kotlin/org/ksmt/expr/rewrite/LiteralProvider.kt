package org.ksmt.expr.rewrite

import org.ksmt.expr.KExpr
import org.kosat.Solver
import org.ksmt.decl.KDecl
import org.ksmt.expr.KConst
import org.ksmt.solver.KModel
import org.ksmt.sort.KBoolSort
import org.ksmt.sort.KBvSort
import org.ksmt.sort.KFpSort
import org.ksmt.sort.KSort

typealias Lit = Int

class LiteralProvider(private val satSolver: Solver) {

    private val expressionToBits: HashMap<KDecl<*>, List<Lit>> = hashMapOf()

    fun newLiteral(): Lit {
        return satSolver.addVariable()
    }

    fun makeBits(expr: KExpr<*>): MutableList<Lit> {
        val result = mutableListOf<Lit>()
        val n = sizeBySort(expr.sort())
        repeat(n) {
            result.add(newLiteral())
        }
        if (expr is KConst) {
            expressionToBits[expr.decl] = result
        }
        return result
    }

    fun getFuncInterpretationFromSolution(literals: List<Lit>): Map<KDecl<*>, KModel.KFuncInterp<*>> {
        println(literals)
        return hashMapOf()
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
