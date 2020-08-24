package org.droidmate.droidgram.merger.grammar

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.slf4j.LoggerFactory
import java.io.File
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

open class Grammar @JvmOverloads constructor(
    initialGrammar: Map<Production, Set<Production>> = emptyMap()
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    protected open val productionSet: MutableMap<Production, MutableSet<Production>> by lazy {
        initialGrammar
            .map { Pair(it.key, it.value.toMutableSet()) }
            .toMap()
            .toMutableMap()
    }

    fun asMap(): Map<Production, Set<Production>> {
        return productionSet.toMap()
    }

    protected fun Map<Production, Set<Production>>.containsKey(symbol: Symbol): Boolean {
        val key = Production(symbol)
        return this.keys.any { it == key }
    }

    private fun Map<Production, Set<Production>>.get(key: Symbol): Set<Production> {
        val symbol = Production(key)
        return this.entries
            .first { it.key == symbol }
            .value
    }

    protected open fun remove(symbol: Symbol) {
        productionSet.remove(Production(symbol))
    }

    open fun key(production: Production): Set<Production> {
        val keys = mutableSetOf<Production>()

        for (symbol in production.values) {
            val key = Production(symbol)

            if (symbol.isNonTerminal()) {
                check(productionSet.keys.any { it == key })
            }

            keys.add(key)
        }

        return keys
    }

    operator fun get(key: Production): Set<Production> {
        return productionSet[key].orEmpty()
    }

    operator fun get(key: Symbol): Set<Production> {
        return productionSet.get(key)
    }

    fun addProduction(name: String, item: String) {

        val key = Production(name)

        val value = Production.fromString(item)

        if (!productionSet.containsKey(key.values.first())) {
            productionSet[key] = mutableSetOf()
        }

        val existingKey = productionSet.keys.first { it == key }

        val values = productionSet[existingKey].orEmpty()

        // There's a bug in Kotlin
        // If we do value in values, it returns false due to different overrides of equals method
        // If we do value in values.map { it }, it returns true (correctly)
        if (value in values.map { it }) {
            // If the value was already there, need to merge the coverages
            val oldValue = values.first { it == value }
            val newValue = Production(value.values)

            val newValues = values
                .filterNot { it == value }
                .toMutableSet()
                .also { it.add(newValue) }

            productionSet.replace(existingKey, values.toMutableSet(), newValues)
        } else {
            productionSet[existingKey]?.add(value)
        }
    }

    open fun reachableNonTerminals(): Set<Symbol> {
        val reachable = mutableSetOf<Symbol>()

        val queue = LinkedList<Symbol>()
        queue.add(Symbol.start)

        while (queue.isNotEmpty()) {
            val symbol = queue.pop()

            if (symbol !in reachable) {
                val newSymbols = productionSet.get(symbol)
                    .flatMap { value ->
                        val nonTerminals = value.nonTerminals
                            .filterNot { reachable.contains(it) }

                        nonTerminals
                    }

                queue.addAll(newSymbols)
                reachable.add(symbol)
            }
        }

        return reachable
    }

    protected open fun unreachableNonTerminals(): Set<Symbol> {
        val reachableNonTerminals = reachableNonTerminals()

        return productionSet.keys
            .map { it.values.first() }
            .filterNot { it in reachableNonTerminals }
            .toSet()
    }

    open fun definedNonTerminals(): Set<Symbol> =
        productionSet.keys
            .flatMap { it.values }
            .toSet()

    open fun reachableTerminals(): Set<Symbol> {
        val processed = mutableSetOf<Symbol>()
        val reachable = mutableSetOf<Symbol>()

        val queue = LinkedList<Symbol>()
        queue.add(Symbol.start)

        while (queue.isNotEmpty()) {
            val symbol = queue.pop()

            if (symbol !in reachable) {
                if (productionSet.containsKey(symbol)) {
                    val newSymbols = productionSet.get(symbol)
                        .flatMap { value ->
                            val nonTerminals = value.nonTerminals
                                .filterNot { processed.contains(it) }

                            nonTerminals
                        }

                    val terminals = productionSet.get(symbol)
                        .flatMap { value -> value.terminals }

                    queue.addAll(newSymbols)
                    reachable.addAll(terminals)

                    processed.add(symbol)
                }
            }
        }

        return reachable
    }

    open fun definedTerminals(): Set<Symbol> =
        productionSet.values
            .flatMap { it.flatMap { production -> production.terminals } }
            .filter { it.isTerminal() }
            .toSet()

    protected open fun usedNonTerminals(): Set<Symbol> {
        return productionSet.keys.flatMap { definedNonTerminal ->
            val expansions = productionSet[definedNonTerminal]

            if (expansions.isNullOrEmpty()) {
                logger.error("Non-terminal $definedNonTerminal: expansion list empty")
            }

            expansions.orEmpty().flatMap { expansion -> (expansion.nonTerminals) }
        }.toSet()
    }

    open fun isValid(): Boolean {
        // All keys should be a single non terminal
        val multiSymbolKeys = productionSet.keys.filterNot { it.values.size == 1 }
        if (multiSymbolKeys.isNotEmpty()) {
            multiSymbolKeys.forEach {
                logger.error("Key $it contains more than 1 symbol")
            }
            return false
        }

        val terminalKeys = productionSet.keys
            .filter { it.terminals.isNotEmpty() }
        if (terminalKeys.isNotEmpty()) {
            terminalKeys.forEach {
                logger.error("Key $it contains 1 or more terminal symbols")
            }
            return false
        }

        val definedTerminals = definedTerminals().sorted()
        val reachableTerminals = reachableTerminals().sorted()
        val unreachableTerminals = definedTerminals
            .filterNot { reachableTerminals.contains(it) }

        if (unreachableTerminals.isNotEmpty()) {
            unreachableTerminals.forEach {
                logger.error("$it is not reachable")
                return false
            }
        }

        val definedNonTerminals = definedNonTerminals()
        val usedNonTerminals = usedNonTerminals().toMutableSet()

        // It must have terms and all terms must have a value
        if (definedNonTerminals.isEmpty() || usedNonTerminals.isEmpty()) {
            return false
        }

        // Do not complain about '<start>' being not used, even if [startSymbol] is different
        usedNonTerminals.add(Symbol.start)

        val unusedNonTerminals = definedNonTerminals
            .filterNot { usedNonTerminals.contains(it) }

        if (unusedNonTerminals.isNotEmpty()) {
            unusedNonTerminals.forEach {
                logger.error("$it defined but not used")
            }

            return false
        }

        val undefinedNonTerminals = usedNonTerminals
            .filterNot { definedNonTerminals.contains(it) }

        if (undefinedNonTerminals.isNotEmpty()) {
            undefinedNonTerminals.forEach {
                logger.error("$it used but not defined")
            }

            return false
        }

        // Symbols must be reachable either from <start> or given start symbol
        val unreachable = unreachableNonTerminals()

        if (unreachable.isNotEmpty()) {
            unreachable.forEach {
                logger.error("$it is unreachable from ${Symbol.start}")
            }

            return false
        }
        return true
    }

    fun checkValid() {
        // check if there is start
        //productionSet["<start>"] ?: throw Exception("Grammar has no <start> nonTerminal")


    }

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)
        fun fromJson(grammarFile: File): Grammar {
            logger.debug("Reading file: $grammarFile")

            return try {
                val gson = Gson()

                // read file
                val reader: Reader = Files.newBufferedReader(Paths.get(grammarFile.toURI()))
                val data: Map<String, List<String>> = gson.fromJson(reader, Map::class.java) as Map<String, List<String>>
                reader.close()

                // create grammar from map
                val grammar = Grammar(mutableMapOf())
                data.forEach { (t: String, u: List<String>) ->
                    u.forEach { str ->
                        grammar.addProduction(t, str)
                    }
                }
                grammar
            } catch (ex: Exception) {
                logger.error("Error while reading file: $grammarFile")
                throw java.lang.Exception("Could not read file: $grammarFile")
            }

        }
    }

    protected open fun dump(): Map<String, Set<String>> {
        return productionSet
            .entries
            .sortedBy { it.key }
            .map { entry ->
                val key = entry.key.values.first().value
                val value = entry.value.map { it.asString() }.toSortedSet()

                Pair(key, value)
            }.toMap()
    }

    open fun asJsonStr(): String {
        val gSon = GsonBuilder().setPrettyPrinting().create()
        val entries = dump()
        return gSon.toJson(entries)
    }

    open fun asString(): String {
        return dump()
            .map { entry ->
                val key = entry.key
                val value = entry.value
                "'${("$key'").padEnd(20)} : \t\t[${value.joinToString(", ") { "'$it'" } }],"
            }.joinToString("\n")
    }
}
