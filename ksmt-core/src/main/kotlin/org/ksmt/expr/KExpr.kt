package org.ksmt.expr

import org.ksmt.KAst
import org.ksmt.KContext
import org.ksmt.expr.logicalexpression.LogicalExpression
import org.ksmt.expr.logicalexpression.SingleLiteral
import org.ksmt.expr.rewrite.KExprBitBuilder
import org.ksmt.expr.transformer.KTransformerBase
import org.ksmt.sort.KSort

abstract class KExpr<T : KSort>(ctx: KContext) : KAst(ctx) {

    val expressionBits: MutableList<SingleLiteral> = mutableListOf()

    abstract fun sort(): T

    abstract fun accept(transformer: KTransformerBase): KExpr<T>

    open fun accept(expressionBuilder: KExprBitBuilder): LogicalExpression? = error("No transformation for $this")

    //  Contexts guarantee that any two equivalent expressions will be the same kotlin object
    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}
