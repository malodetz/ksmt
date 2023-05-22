package org.ksmt.solver.kbva

import org.ksmt.expr.KExpr
import org.ksmt.KContext
import org.ksmt.decl.KDecl
import org.ksmt.expr.KConst
import org.ksmt.solver.KModel
import org.ksmt.sort.*


class LiteralProvider(private val ctx: KContext) {

    private val expressionToBits: HashMap<KDecl<*>, List<Lit>> = hashMapOf()
    var currentLiteral: Lit = 0

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
        val stringRepresentation = literals.map {
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
