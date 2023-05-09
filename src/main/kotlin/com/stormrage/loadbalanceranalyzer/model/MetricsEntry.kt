package com.stormrage.loadbalanceranalyzer.model

data class MetricsEntry(
    val requestTime: Long,
    val responseTime: Long,
    val duration: Int,
    val httpCode: Int,
    val serverInfo: String
)
