package com.stormrage.loadbalanceranalyzer.service

import com.stormrage.loadbalanceranalyzer.model.MetricsParserEntity
import com.stormrage.loadbalanceranalyzer.parser.MetricsParser
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class LoadBalancerAnalyzerService(
    private val metricParser: MetricsParser,
    private val meterRegistry: MeterRegistry
) {

    val serverRequestCounter: Counter = Counter.builder("server_requests_total")
        .tag("server", "code")
        .register(meterRegistry)

    val serverRequestDuration = Timer.builder("server_request_duration_ms")
        .description("Request duration for each server")
        .tag("server", "value")
        .register(meterRegistry)

    @Scheduled(fixedRate = 15 * 1000)
    fun runMetrics() {
        printServerRequestStats()
    }

    fun printServerRequestStats() {
        var metrics: MetricsParserEntity
        val serverRequestCounts = mutableMapOf<String, Int>()
        while (true) {
            metrics = metricParser.getMetrics()
            metrics.metrics.forEach {
                serverRequestCounter.increment()
                serverRequestDuration.record(it.duration.toLong(), TimeUnit.MILLISECONDS)
                meterRegistry.counter("server_requests_total", "server", it.serverInfo, "code", it.httpCode.toString()).increment()
                meterRegistry.timer("server_request_duration_ms", "server", it.serverInfo).record(it.duration.toLong(), TimeUnit.MILLISECONDS)
                serverRequestCounts[it.serverInfo] = serverRequestCounts.getOrDefault(it.serverInfo, 0) + 1
            }
            for ((server, count) in serverRequestCounts) {
                val share = count.toDouble() / metrics.metrics.size * 100 // total requests
                println("Server $server has $count requests (${"%.2f".format(share)}% of total requests)")
            }
            if (!metrics.shouldContinue) {
                break
            }
        }
    }
}
