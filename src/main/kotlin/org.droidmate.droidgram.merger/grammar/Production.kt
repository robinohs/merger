package org.droidmate.droidgram.merger.grammar

import org.slf4j.LoggerFactory

open class Production(
    val values: List<Symbol>
) : Comparable<Production> {
    private val logger = LoggerFactory.getLogger(javaClass)

    constructor(value: Symbol) : this(listOf(value))

    constructor(value: String) : this(listOf(Symbol(value)))

    constructor(values: Array<String>) : this(values.map { Symbol(it) })

    val terminals by lazy {
        values.filter { it.isTerminal() }
    }

    val nonTerminals by lazy {
        values.filterNot { it.isTerminal() }
    }

    fun isTerminal(): Boolean {
        return values.all { it.isTerminal() }
    }

    fun asString(): String {
        return values.joinToString("ÿ") { p -> p.value }
    }

    fun replace(condition: (Symbol) -> Boolean, newSymbol: Symbol): Production {
        val newValues = values.map { symbol ->
            if (condition(symbol)) {
                newSymbol
            } else {
                symbol
            }
        }

        return Production(newValues)
    }

    fun replace(oldSymbol: Symbol, newSymbol: Symbol): Production {
        return replace({ symbol -> symbol == oldSymbol }, newSymbol)
    }

    fun replaceByEpsilon(oldSymbol: Symbol): Production {
        return replace(oldSymbol, Symbol.epsilon)
    }

    fun isStart(): Boolean {
        return values.all { it == Symbol.start }
    }

    fun isEpsilon(): Boolean {
        return values.all { it == Symbol.epsilon }
    }

    fun isEmpty(): Boolean {
        return values.all { it == Symbol.empty }
    }

    fun hasValue(): Boolean {
        return values.isNotEmpty()
    }

    fun contains(symbol: Symbol): Boolean {
        return this.values.contains(symbol)
    }

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)

        @JvmStatic
        val epsilon = Production(Symbol.epsilon)
        @JvmStatic
        val start = Production(Symbol.start)
        @JvmStatic
        val empty = Production(Symbol.empty)

        fun fromString(str: String): Production {
            val list: Array<String> = str.split("ÿ").toTypedArray()
            return Production(list)
        }

    }

    override fun compareTo(other: Production): Int {
        return values.toString().compareTo(other.values.toString())
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Production -> values == other.values
            else -> false
        }
    }

    override fun toString(): String {
        return values.joinToString(" ")
    }

    override fun hashCode(): Int {
        return values.hashCode()
    }
}