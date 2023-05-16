package com.stormrage.loadbalanceranalyzer.metrics

import com.stormrage.loadbalanceranalyzer.model.MetricsEntry
import com.stormrage.loadbalanceranalyzer.repository.MetricsRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.Instant
import javax.annotation.PostConstruct

@Service
class DatabaseMetricsProvider(
    private val metricsRepository: MetricsRepository,
    @Value("\${metrics.preload-period-hours}") private val preloadPeriodHours: Long
) : MetricsProvider {
    private val metricsData: MutableList<MetricsEntry> = mutableListOf()

    @PostConstruct
    fun init() {
        val now = Instant.now()
        val start = now.minusSeconds(60 * 60 * preloadPeriodHours)

        var page = 0
        while (true) {
            val metrics = metricsRepository.findByRequestTimeBetween(
                start.toEpochMilli(), now.toEpochMilli(), PageRequest.of(page, 500)
            )
            if (metrics.isEmpty()) {
                break
            }
            metricsData.addAll(metrics.map {
                MetricsEntry(it.requestTime, it.responseTime, it.duration, it.httpCode, it.serverInfo)
            })
            page++
        }
    }

    override fun getMetrics(): List<MetricsEntry> {
        return metricsData
    }
}