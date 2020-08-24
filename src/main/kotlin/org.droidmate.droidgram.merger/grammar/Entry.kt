package org.droidmate.droidgram.merger.grammar

import java.io.Serializable

data class Entry(
    val symbol: Symbol,
    val probability: Double
)