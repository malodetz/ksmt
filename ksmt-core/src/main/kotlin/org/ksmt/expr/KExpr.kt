package org.ksmt.expr

import org.ksmt.KAst
import org.ksmt.KContext
import org.ksmt.expr.rewrite.KVisitor
import org.ksmt.sort.KSort

abstract class KExpr<T : KSort>(ctx: KContext) : KAst(ctx) {

    private val visitorCache = mutableMapOf<String, Any>()

    abstract fun sort(): T

    fun cachedAccept(visitor: KVisitor): Any = visitorCache.getOrPut(visitor.visitorId) { return accept(visitor) }

    open fun accept(visitor: KVisitor): Any = error("No transformation for $this in visitor ${visitor.visitorId}")

    //  Contexts guarantee that any two equivalent expressions will be the same kotlin object
    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)
}
