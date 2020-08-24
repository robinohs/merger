package org.droidmate.droidgram.merger.grammar

fun Symbol.isTerminate(): Boolean {
    return value.contains("Terminate")
}

fun Production.test(): Boolean {
    return false
}

fun Production.isTerminate(): Boolean {
    return values.any { it.isTerminate() }
}

fun Production.isAction(): Boolean {
    return values.any { it.isNonTerminal() && it.contains("(") }
}

fun Production.isState(): Boolean {
    return values.size == 1 &&
            values.first().value.startsWith("<s")
}