@file:Suppress("FunctionName", "unused")

package org.ksmt.solver.kissat.bindings

import com.sun.jna.Native
import com.sun.jna.Pointer
import org.ksmt.solver.KSolverStatus

/**
 * Pointer to the Kissat solver.
 */
typealias Kissat = Pointer

/**
 * Kissat term (array of literals).
 */
typealias KissatTerm = IntArray

/**
 * Kissat literal.
 */
typealias KissatLit = Int


/**
 * Kissat native bindings.
 */
object Native {

    init {
        Native.register("kissat")
    }


    /**
     * Initialize Kissat solver.
     */
    fun init(): Kissat = kissat_init()

    /**
     * Free (release) Kissat solver.
     */
    fun Kissat.release() = kissat_release(solver = this)


    /**
     * Get Kissat option value by option [name].
     */
    fun Kissat.getOption(name: String): Int = kissat_get_option(solver = this, name)

    /**
     * Set the given [new value][newValue] to the Kissat option by its [name].
     */
    fun Kissat.setOption(name: String, newValue: Int): Int = kissat_set_option(solver = this, name, newValue)


    /**
     * Reserve [maxVar] variables in Kissat formula builder.
     */
    fun Kissat.reserve(maxVar: Int) = kissat_reserve(solver = this, maxVar)


    /**
     * Add the given [literal] to Kissat formula builder.
     */
    fun Kissat.add(literal: KissatLit) = kissat_add(solver = this, literal)

    /**
     * Add the given [literals] to Kissat formula builder.
     */
    fun Kissat.add(literals: KissatTerm) = literals.forEach { add(it) }


    /**
     * Solve the formed formula and return the solving status.
     */
    @Suppress("MagicNumber")
    fun Kissat.solve(): KSolverStatus = when (kissat_solve(solver = this)) {
        10 -> KSolverStatus.SAT
        20 -> KSolverStatus.UNSAT
        else -> KSolverStatus.UNKNOWN
    }


    /**
     * Get boolean value of the given [literal], or null if impossible.
     */
    fun Kissat.value(literal: KissatLit): Boolean? = kissat_value(solver = this, literal).let {
        when {
            it > 0 -> true
            it < 0 -> false
            else -> null
        }
    }


    /**
     * Print solving statistics.
     */
    fun Kissat.printStatistics() = kissat_print_statistics(solver = this)


    // Info

    /**
     * Get Kissat signature.
     */
    fun signature(): String = kissat_signature()

    /**
     * Get Kissat id.
     */
    fun id(): String = kissat_id()

    /**
     * Get Kissat version.
     */
    fun version(): String = kissat_version()

    /**
     * Get compiler configuration that was used to build this Kissat library.
     */
    fun compiler(): String = kissat_compiler()

    /**
     * Get Kissat copyright info.
     */
    fun copyright(): String = kissat_copyright().getStringArray(0).joinToString(separator = "\n")

    /**
     * Print build info using the given [line prefix][linePrefix].
     */
    fun printBuildInfo(linePrefix: String) = kissat_build(linePrefix)

    /**
     * Print info banner using the given [line prefix][linePrefix] and [name of the app][nameOfApp].
     */
    fun printBanner(linePrefix: String, nameOfApp: String) = kissat_banner(linePrefix, nameOfApp)


    // Native functions


    // Default (partial) IPASIR interface.

    private external fun kissat_signature(): String

    private external fun kissat_init(): Kissat

    private external fun kissat_release(solver: Kissat)

    private external fun kissat_add(solver: Kissat, lit: KissatLit)

    private external fun kissat_solve(solver: Kissat): Int

    private external fun kissat_value(solver: Kissat, lit: KissatLit): Int


    // Additional API functions.

    private external fun kissat_reserve(solver: Kissat, maxVar: Int)

    private external fun kissat_id(): String
    private external fun kissat_version(): String
    private external fun kissat_compiler(): String
    private external fun kissat_copyright(): Pointer
    private external fun kissat_build(linePrefix: String)
    private external fun kissat_banner(linePrefix: String, nameOfApp: String)

    private external fun kissat_get_option(solver: Kissat, name: String): Int
    private external fun kissat_set_option(solver: Kissat, name: String, newValue: Int): Int

    private external fun kissat_print_statistics(solver: Kissat)
}
