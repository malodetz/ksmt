package org.ksmt.expr.rewrite

import org.ksmt.expr.*
import org.ksmt.sort.KSort

abstract class Builder(val builderId: String) {

    abstract fun <T : KSort, A : KExpr<*>> transform(kApp: KApp<T, A>): Any
    abstract fun transform(expr: KConst<*>): Any

    abstract fun transform(expr: KAndExpr): Any
    abstract fun transform(expr: KOrExpr): Any
    abstract fun transform(expr: KNotExpr): Any
    abstract fun transform(expr: KImpliesExpr): Any
    abstract fun transform(expr: KXorExpr): Any
    abstract fun transform(expr: KTrue): Any
    abstract fun transform(expr: KFalse): Any
    abstract fun <T : KSort> transform(expr: KEqExpr<T>): Any
    abstract fun <T : KSort> transform(expr: KDistinctExpr<T>): Any
    abstract fun <T : KSort> transform(expr: KIteExpr<T>): Any

}
