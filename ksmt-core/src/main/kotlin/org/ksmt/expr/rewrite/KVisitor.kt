package org.ksmt.expr.rewrite

import org.ksmt.expr.*
import org.ksmt.sort.*

abstract class KVisitor(val visitorId: String) {

    abstract fun <T : KSort> transform(expr: KFunctionApp<T>): Any
    abstract fun <T : KSort> transform(expr: KConst<T>): Any

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

    // bit-vec transformers
    abstract fun transform(expr: KBitVec1Value): Any
    abstract fun transform(expr: KBitVec8Value): Any
    abstract fun transform(expr: KBitVec16Value): Any
    abstract fun transform(expr: KBitVec32Value): Any
    abstract fun transform(expr: KBitVec64Value): Any
    abstract fun transform(expr: KBitVecCustomValue): Any

    // bit-vec expressions transformers
    abstract fun <T : KBvSort> transform(expr: KBvNotExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvReductionAndExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvReductionOrExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvAndExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvOrExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvXorExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvNAndExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvNorExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvXNorExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvNegationExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvAddExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvSubExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvMulExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvUnsignedDivExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvSignedDivExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvUnsignedRemExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvSignedRemExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvSignedModExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvUnsignedLessExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvSignedLessExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvUnsignedLessOrEqualExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvSignedLessOrEqualExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvUnsignedGreaterOrEqualExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvSignedGreaterOrEqualExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvUnsignedGreaterExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvSignedGreaterExpr<T>): Any
    abstract fun transform(expr: KBvConcatExpr): Any
    abstract fun transform(expr: KBvExtractExpr): Any
    abstract fun transform(expr: KBvSignExtensionExpr): Any
    abstract fun transform(expr: KBvZeroExtensionExpr): Any
    abstract fun transform(expr: KBvRepeatExpr): Any
    abstract fun <T : KBvSort> transform(expr: KBvShiftLeftExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvLogicalShiftRightExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvArithShiftRightExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvRotateLeftExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvRotateLeftIndexedExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvRotateRightExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvRotateRightIndexedExpr<T>): Any
    abstract fun transform(expr: KBv2IntExpr): Any
    abstract fun <T : KBvSort> transform(expr: KBvAddNoOverflowExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvAddNoUnderflowExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvSubNoOverflowExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvSubNoUnderflowExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvDivNoOverflowExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvNegNoOverflowExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvMulNoOverflowExpr<T>): Any
    abstract fun <T : KBvSort> transform(expr: KBvMulNoUnderflowExpr<T>): Any

    // fp value transformers
    abstract fun transform(expr: KFp16Value): Any
    abstract fun transform(expr: KFp32Value): Any
    abstract fun transform(expr: KFp64Value): Any
    abstract fun transform(expr: KFp128Value): Any
    abstract fun transform(expr: KFpCustomSizeValue): Any

    // fp rounding mode
    abstract fun transform(expr: KFpRoundingModeExpr): Any

    // fp operations tranformation
    abstract fun <T : KFpSort> transform(expr: KFpAbsExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpNegationExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpAddExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpSubExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpMulExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpDivExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpFusedMulAddExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpSqrtExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpRemExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpRoundToIntegralExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpMinExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpMaxExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpLessOrEqualExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpLessExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpGreaterOrEqualExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpGreaterExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpEqualExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpIsNormalExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpIsSubnormalExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpIsZeroExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpIsInfiniteExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpIsNaNExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpIsNegativeExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpIsPositiveExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpToBvExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpToRealExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpToIEEEBvExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpFromBvExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KFpToFpExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KRealToFpExpr<T>): Any
    abstract fun <T : KFpSort> transform(expr: KBvToFpExpr<T>): Any

    // array transformers
    abstract fun <D : KSort, R : KSort> transform(expr: KArrayStore<D, R>): Any
    abstract fun <D : KSort, R : KSort> transform(expr: KArraySelect<D, R>): Any
    abstract fun <D : KSort, R : KSort> transform(expr: KArrayConst<D, R>): Any
    abstract fun <D : KSort, R : KSort> transform(expr: KFunctionAsArray<D, R>): Any
    abstract fun <D : KSort, R : KSort> transform(expr: KArrayLambda<D, R>): Any

    // arith transformers
    abstract fun <T : KArithSort<T>> transform(expr: KAddArithExpr<T>): Any
    abstract fun <T : KArithSort<T>> transform(expr: KMulArithExpr<T>): Any
    abstract fun <T : KArithSort<T>> transform(expr: KSubArithExpr<T>): Any
    abstract fun <T : KArithSort<T>> transform(expr: KUnaryMinusArithExpr<T>): Any
    abstract fun <T : KArithSort<T>> transform(expr: KDivArithExpr<T>): Any
    abstract fun <T : KArithSort<T>> transform(expr: KPowerArithExpr<T>): Any
    abstract fun <T : KArithSort<T>> transform(expr: KLtArithExpr<T>): Any
    abstract fun <T : KArithSort<T>> transform(expr: KLeArithExpr<T>): Any
    abstract fun <T : KArithSort<T>> transform(expr: KGtArithExpr<T>): Any
    abstract fun <T : KArithSort<T>> transform(expr: KGeArithExpr<T>): Any

    // integer transformers
    abstract fun transform(expr: KModIntExpr): Any
    abstract fun transform(expr: KRemIntExpr): Any
    abstract fun transform(expr: KToRealIntExpr): Any
    abstract fun transform(expr: KInt32NumExpr): Any
    abstract fun transform(expr: KInt64NumExpr): Any
    abstract fun transform(expr: KIntBigNumExpr): Any

    // real transformers
    abstract fun transform(expr: KToIntRealExpr): Any
    abstract fun transform(expr: KIsIntRealExpr): Any
    abstract fun transform(expr: KRealNumExpr): Any

    // quantifier transformers
    abstract fun transform(expr: KExistentialQuantifier): Any
    abstract fun transform(expr: KUniversalQuantifier): Any
}
