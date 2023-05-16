package com.stormrage.loadbalanceranalyzer.model

data class DistributionInfo(
    val goodDistribution: Boolean,
    val host: String,
    val share: Double
)
