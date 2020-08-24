package org.droidmate.droidgram.merger.grammar

data class ProbabilisticProduction(
    val rule: Production,
    val probability: Double
) {
    fun asString(): String {
        return rule.asString()
    }
}