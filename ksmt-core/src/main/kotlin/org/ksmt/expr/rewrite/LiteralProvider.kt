package org.ksmt.expr.rewrite

import org.ksmt.expr.KExpr
import org.kosat.Solver
import org.ksmt.KContext
import org.ksmt.decl.KDecl
import org.ksmt.expr.KConst
import org.ksmt.solver.KModel
import org.ksmt.sort.*

typealias Lit = Int

class LiteralProvider(private val ctx: KContext, private val satSolver: Solver) {

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
        val trueLiterals = HashSet<Lit>()
        literals.filter { it > 0 }.forEach { trueLiterals.add(it) }
        val result = hashMapOf<KDecl<*>, KModel.KFuncInterp<*>>()
        expressionToBits.forEach { (t, u) ->
            result[t] =
                KModel.KFuncInterp(t.sort, emptyList(), emptyList(), expressionFromBits(t, u, trueLiterals))
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : KSort> expressionFromBits(decl: KDecl<*>, literals: List<Lit>, trueLiterals: Set<Lit>): KExpr<T> {
        val stringRepresentation =  literals.map {
            if (trueLiterals.contains(it)) {
                '1'
            } else {
                '0'
            }
        }.joinToString("")
        return when (decl.sort) {
            is KBoolSort -> {
                if (stringRepresentation == "1") {
                    ctx.mkTrue()
                } else {
                    ctx.mkFalse()
                }
            }

            is KBvSort -> {
                ctx.mkBv(stringRepresentation, stringRepresentation.length.toUInt())
            }

            else -> {
                error("Can't build $decl from literals")
            }
        } as KExpr<T>
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
                error("No bit representation is define for $sort")
            }
        }
    }
}
