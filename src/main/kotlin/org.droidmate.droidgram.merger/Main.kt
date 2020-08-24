package org.droidmate.droidgram.merger

import org.droidmate.droidgram.merger.config.Config
import org.droidmate.droidgram.merger.grammar.GrammarMerger

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val config: Config = Config.createConfig(args)
        val grammarMerger: GrammarMerger = GrammarMerger.createMerger(config)
        grammarMerger.runMerge()
    }
}