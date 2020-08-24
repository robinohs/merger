package org.droidmate.droidgram.merger.grammar

import com.google.gson.GsonBuilder
import org.droidmate.droidgram.merger.config.Config
import org.slf4j.LoggerFactory
import java.lang.StringBuilder

open class ProbabilisticGrammar(
    initialGrammar: Map<Production, Set<Production>> = emptyMap()
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val symbols: MutableMap<Production, MutableList<ProbabilisticProduction>> = mutableMapOf()

    fun addProduction(key: Production, production: ProbabilisticProduction) {
        if(symbols.containsKey(key)) {
            symbols[key]!!.add(production);
        } else {
            symbols[key] = mutableListOf(production)
        }
    }

    fun getProductions(key: Production): List<ProbabilisticProduction> {
        return if(symbols.containsKey(key)) {
            symbols[key]!!.toList();
        } else {
            listOf()
        }
    }

    protected open fun dump(): Map<String, Set<Pair<String, Double>>> {
        return symbols
            .entries
            .sortedBy { it.key }
            .map { entry ->
                val key = entry.key.values.first().value
                val value = entry.value.map { Pair(it.rule.asString(), it.probability) }.toSet()

                Pair(key, value)
            }.toMap()
    }

    open fun asJsonStr(): String {
        val gSon = GsonBuilder().setPrettyPrinting().create()
        val entries = dump()
        //logger.debug(entries.toString())
        val output = StringBuilder()

        /*
        output.append("{\n")
        var currentIndex = 0
        var maxIndex: Int = entries.size
        entries.forEach { (t, u) ->

            output.append("\t\"$t\": [\n")
            var currentPairIndex = 0
            var maxPairIndex: Int = u.size
            u.forEach {
                if(currentPairIndex == maxPairIndex-1) {
                    output.append("\t\t(\"${it.first}\", opts(prob=${it.second}))\n")
                    //output.append("\t\t\"${it.first}\"\n")
                } else {
                    output.append("\t\t(\"${it.first}\", opts(prob=${it.second})),\n")
                    //output.append("\t\t\"${it.first}\",\n")
                }
                currentPairIndex++
            }
            if(currentIndex == maxIndex-1) {
                output.append("\t]\n")
            } else {
                output.append("\t],\n")
            }
            currentIndex++
        }
        output.append("}")
         */
        return gSon.toJson(entries)
    }
}