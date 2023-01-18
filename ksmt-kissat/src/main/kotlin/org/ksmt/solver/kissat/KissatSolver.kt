package org.ksmt.solver.kissat

import org.kosat.Lit
import org.kosat.Solver
import org.ksmt.solver.KSolverStatus
import org.ksmt.solver.kissat.bindings.KissatLit
import org.ksmt.solver.kissat.bindings.KissatTerm
import org.ksmt.solver.kissat.bindings.Native
import org.ksmt.solver.kissat.bindings.Native.add
import org.ksmt.solver.kissat.bindings.Native.release
import org.ksmt.solver.kissat.bindings.Native.reserve
import org.ksmt.solver.kissat.bindings.Native.setOption
import org.ksmt.solver.kissat.bindings.Native.solve
import org.ksmt.solver.kissat.bindings.Native.value

class KissatSolver : Solver, AutoCloseable {

    private var isClosed = false

    private val kissat = Native.init().apply {
        setOption(name = "quiet", newValue = 1)
    }

    override var numberOfClauses: Int = 0
        private set

    override var numberOfVariables: Int = 0
        private set

    init {
        numberOfVariables += 2
        addClause(TRUE)
        addClause(not(FALSE))
    }


    override fun addVariable(): Int = (++numberOfVariables).also { kissat.reserve(it) }


    override fun addClause(lit: Lit) = addClause(intArrayOf(lit))

    override fun addClause(lit1: Lit, lit2: Lit) = addClause(intArrayOf(lit1, lit2))

    override fun addClause(lit1: Lit, lit2: Lit, lit3: Lit) = addClause(intArrayOf(lit1, lit2, lit3))

    override fun addClause(literals: Iterable<Lit>) = addClause(literals.toList())

    override fun addClause(literals: List<Lit>) = addClause(literals.toIntArray())

    private fun addClause(literals: KissatTerm) {
        numberOfClauses += 1
        kissat.add(literals)
        kissat.add(AND)
    }


    override fun solve(): Boolean = kissat.solve() == KSolverStatus.SAT

    override fun solve(assumptions: Iterable<Lit>): Boolean = Utils.unsupportedIncSolving()

    override fun solve(assumptions: List<Lit>): Boolean = Utils.unsupportedIncSolving()


    override fun getValue(lit: Lit): Boolean = kissat.value(lit) ?: true

    override fun getModel(): List<Lit> = Utils.unsupportedIncSolving()


    companion object {

        /**
         * `AND` literal in DIMACS notation represented as `0`.
         */
        const val AND: Int = 0

        /**
         * `TRUE` can be represented as the first variable in CNF (true_var & !false_var & ...).
         * In DIMACS notation that would be `1`.
         */
        const val TRUE: Int = 1

        /**
         * `FALSE` can be represented as the second variable in CNF (true_var & !false_var & ...).
         * In DIMACS notation that would be `2`.
         */
        const val FALSE: Int = 2

        /**
         * `NOT` in DIMACS notation represented as a negated [variable index][literal].
         */
        fun not(literal: KissatLit): KissatLit = -literal
    }


    override fun close() {
        isClosed = true
        kissat.release()
    }
}
