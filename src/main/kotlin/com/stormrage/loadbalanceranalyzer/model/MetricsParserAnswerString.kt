package com.stormrage.loadbalanceranalyzer.model

data class MetricsParserAnswerString(
    val metrics: List<String>,
    val shouldContinue: Boolean
)

data class MetricsParserAnswer(
    val metrics: List<MetricsEntry>,
    val shouldContinue: Boolean
)