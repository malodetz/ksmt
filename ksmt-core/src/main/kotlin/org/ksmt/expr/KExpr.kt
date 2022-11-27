package org.ksmt.expr

import org.ksmt.KAst
import org.ksmt.KContext
import org.ksmt.expr.rewrite.Builder
import org.ksmt.expr.transformer.KTransformerBase
import org.ksmt.sort.KSort

abstract class KExpr<T : KSort>(ctx: KContext) : KAst(ctx) {

    private val buildCache = mutableMapOf<String, Any>()

    abstract fun sort(): T

    abstract fun accept(transformer: KTransformerBase): KExpr<T>

    fun cachedAccept(builder: Builder): Any = buildCache.getOrPut(builder.builderId) { return accept(builder) }

    open fun accept(builder: Builder): Any = error("No transformation for $this")

    //  Contexts guarantee that any two equivalent expressions will be the same kotlin object
    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}
