package org.droidmate.droidgram.merger.grammar

import org.slf4j.LoggerFactory

data class Symbol(
    val value: String
) : Comparable<Symbol> {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun contains(str: String): Boolean {
        return str in value
    }

    fun isNonTerminal(): Boolean {
        return value.startsWith("<")
    }

    fun isTerminal(): Boolean {
        return !isNonTerminal()
    }

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)

        private const val startValue = "<start>"

        @JvmStatic
        val empty = Symbol("")

        @JvmStatic
        val epsilon = Symbol("<empty>")

        @JvmStatic
        val start = Symbol(startValue)

    }

    override fun compareTo(other: Symbol): Int {
        return value.compareTo(other.value)
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Symbol -> value == other.value
            else -> false
        }
    }

    override fun toString(): String {
        return value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}