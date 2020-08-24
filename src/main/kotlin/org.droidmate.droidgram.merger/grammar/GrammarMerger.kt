package org.droidmate.droidgram.merger.grammar

import org.droidmate.droidgram.merger.config.Config
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter
import java.lang.Exception

class GrammarMerger(
    private val config: Config
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)

        fun createMerger(config: Config): GrammarMerger {
            val grammarMerger = GrammarMerger(config)
            logger.debug("Created new instance with debug set to ${grammarMerger.config.debug}")
            return grammarMerger
        }

    }

    fun runMerge() {
        // extract all grammars from input path
        val grammars: MutableList<Grammar> = extractGrammars()
        val translationTables = extractTranslationTables()

        // write all grammars to console if debug is true
        if(config.debug)
            grammars.forEach {
                logger.debug("\n${it.asString()}")
            }

        // check if grammars were found else abort program
        if(grammars.size == 0)
            return logger.error("No grammars were found!")

        /*
        var mergedGrammar: Grammar? = null

        // check if simpleMerge is activated
        mergedGrammar = if(config.simpleMerge) {
            simpleMerge(grammars)
        } else {
            probabilisticMerge(grammars)
        }
        */
        // output file
        if(config.simpleMerge) {
            writeStringToFile(simpleMerge(grammars).asJsonStr())
        } else {
            writeStringToFile(probabilisticMerge(grammars).asJsonStr())
        }
    }

    private fun probabilisticMerge(grammars: MutableList<Grammar>): ProbabilisticGrammar {
        val productions: MutableMap<Production, Int> = mutableMapOf()
        val symbols: MutableList<Symbol> = mutableListOf(Symbol("<start>"))
        val seenSymbols: MutableList<Symbol> = mutableListOf(Symbol("<start>"))

        val probabilisticGrammar = ProbabilisticGrammar()

        while(symbols.isNotEmpty()) {
            val currentSymbol: Symbol = symbols.first();

            // if symbol is empty,
            if(currentSymbol == Symbol("<empty>")) {
                probabilisticGrammar.addProduction(Production(currentSymbol.value), ProbabilisticProduction(Production(""), 1.0))
                symbols.remove(currentSymbol);
                continue;
            }

            var totalRules = 0

            grammars.forEach { grammar ->
                try
                {
                    grammar[currentSymbol].forEach {
                        if (productions.containsKey(it)) {
                            productions[it] = productions[it]!!.plus(1)
                        }
                        else {
                            productions[it] = 1;
                        }

                        totalRules++;

                        val symbol: Symbol = it.nonTerminals.first()
                        if(!symbols.contains(symbol) && !seenSymbols.contains(symbol)) {
                            symbols.add(symbol)
                            seenSymbols.add(symbol)
                        }
                    }
                } catch (e: NoSuchElementException){

                }
            }

            symbols.remove(currentSymbol);

            productions.forEach {
                    (t, u) ->
                println("$t occurred $u times")
                val probability: Double = u.toDouble() / totalRules.toDouble();
                probabilisticGrammar.addProduction(Production(currentSymbol.value), ProbabilisticProduction(t, probability));
            }
            productions.clear()
        }
        return probabilisticGrammar
    }

    private fun simpleMerge(grammars: List<Grammar>): Grammar {
        val mergedGrammar = Grammar()
        grammars.forEach { grammar ->
            grammar.asMap().forEach{ entry ->
                entry.value.forEach { prod ->
                    mergedGrammar.addProduction(entry.key.asString(), prod.asString())
                }
            }
        }
        return mergedGrammar
    }

    private fun writeStringToFile(str: String) {
        if(config.debug)
            logger.debug("Output:\n${str}")

        try {
            val writer = PrintWriter("${config.outputPath}/grammar.txt")
            writer.append(str)
            writer.close()
        } catch (e: Exception) {
            logger.error("Could not write string to file ${config.outputPath}/grammar.txt")
        }
    }

    /*
    private fun extractMergeInstructionFile(): Any? {
        // check if mergeInstructionFile is valid
        if(!File(config.mergeInstructionFile!!.toUri()).isFile) {
            logger.error("mergeInstructionFile arg is no valid path to file")
            throw Exception("mergeInstructionFile arg is no valid path to file")
        }

        return null
    }
    */

    private fun extractTranslationTables(): MutableList<TranslationTable> {
        val translationTables: MutableList<TranslationTable> = mutableListOf()
        File(config.inputPath.toUri()).walk().forEach {
            if(it.endsWith("translationTable.txt")) {
                try {
                    val translationTable: TranslationTable = TranslationTable.fromFile(it)
                    translationTables.add(translationTable)
                    logger.info("Successfully read file: ${it.absolutePath}")
                } catch (e: Exception) {
                    logger.error("Skipping file: ${it.absolutePath}")
                }
            }
        }
        return translationTables
    }

    private fun extractGrammars(): MutableList<Grammar> {
        val grammars: MutableList<Grammar> = mutableListOf()
        File(config.inputPath.toUri()).walk().forEach {
            if(it.endsWith("grammar.txt")) {
                try {
                    val grammar: Grammar = Grammar.fromJson(it)
                    if(grammar.isValid()) {
                        grammars.add(grammar)
                        logger.info("Successfully read file: ${it.absolutePath}")
                    }
                    else
                        throw Exception()
                } catch (e: Exception) {
                    logger.error("Skipping file: ${it.absolutePath}")
                }
            }
        }
        return grammars
    }
}
