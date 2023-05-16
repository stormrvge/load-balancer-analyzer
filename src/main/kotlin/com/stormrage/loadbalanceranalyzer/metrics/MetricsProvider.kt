package com.stormrage.loadbalanceranalyzer.metrics

import com.stormrage.loadbalanceranalyzer.model.MetricsParserAnswer

interface MetricsProvider {
    fun getMetrics(): MetricsParserAnswer
}
