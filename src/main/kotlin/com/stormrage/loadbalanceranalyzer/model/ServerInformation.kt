package com.stormrage.loadbalanceranalyzer.model

data class ServerInformation(
    val host: String,
    var windowRequestCount: Long = 0,
    var totalRequestCount: Long = 0,
    var retries: Int = 0
)