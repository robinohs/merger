package org.droidmate.droidgram.merger.grammar

import org.droidmate.droidgram.merger.config.Config
import org.slf4j.LoggerFactory
import java.io.File

class TranslationTable {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)

        fun fromFile(file: File): TranslationTable {
            println(file)
            val bufferedReader = file.bufferedReader()
            val text:List<String> = bufferedReader.readLines()
            for(line in text){
                println(line)
            }
            return TranslationTable()
        }

    }
}