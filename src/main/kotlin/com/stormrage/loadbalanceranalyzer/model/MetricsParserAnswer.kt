package com.stormrage.loadbalanceranalyzer.model

data class MetricsParserAnswer(
    val metrics: List<String>,
    val shouldContinue: Boolean
)

data class MetricsParserEntity(
    val metrics: List<MetricsEntry>,
    val shouldContinue: Boolean
)