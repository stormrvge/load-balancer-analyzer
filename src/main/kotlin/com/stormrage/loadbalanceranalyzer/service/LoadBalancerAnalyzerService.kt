package com.stormrage.loadbalanceranalyzer.service

import com.stormrage.loadbalanceranalyzer.dao.MetricsEntryEntity
import com.stormrage.loadbalanceranalyzer.model.DistributionInfo
import com.stormrage.loadbalanceranalyzer.model.MetricsParserAnswer
import com.stormrage.loadbalanceranalyzer.model.ServerInformation
import com.stormrage.loadbalanceranalyzer.metrics.MetricsParser
import com.stormrage.loadbalanceranalyzer.repository.MetricsRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import mu.KotlinLogging
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import kotlin.math.abs

@Service
class LoadBalancerAnalyzerService(
    private val metricParser: MetricsParser,
    private val metricsRepository: MetricsRepository,
    private val meterRegistry: MeterRegistry
) {
    private val log = KotlinLogging.logger { }

    @Value("\${database.enabled}")
    private var useDatabase: Boolean = false

    @Value("\${database.preload-period-hours}")
    private var preloadPeriodHours: Int = 2


    @Value("\${threshold}")
    private var threshold: Double = 0.1

    private val serversInformation: HashMap<String, ServerInformation> = HashMap()

    @PostConstruct
    private fun initDatabase() {
        if (useDatabase) {

        }
    }

    @Scheduled(fixedRateString = "\${scheduler.fixed-rate}", initialDelayString = "\${scheduler.delay}")
    fun runMetrics() {
        printServerRequestStats()
    }

    fun printServerRequestStats() {
        val thresholdNumberOfRequests = serversInformation.size * 3

        var metrics: MetricsParserAnswer
        val serversToRemove = mutableListOf<String>()

        while (true) {
            metrics = metricParser.getMetrics()
            val shouldCountForWindow = metrics.metrics.size > thresholdNumberOfRequests
            val grouped = metrics.metrics.groupBy { x -> x.serverInfo }

            serversInformation.forEach {
                it.value.windowRequestCount = 0
            }

            // Add new servers if exists
            grouped.forEach {
                if (!serversInformation.containsKey(it.key)) {
                    serversInformation[it.key] = ServerInformation(it.key)
                }
            }

            if (shouldCountForWindow) {
                // Check is all old servers is available
                serversInformation.forEach {
                    if (!grouped.containsKey(it.key)) {
                        if (it.value.retries > 5) {
                            serversToRemove.add(it.key)
                            log.debug("Server ${it.key} will be removed from list of available servers for long unavailable state.")
                        } else {
                            it.value.retries++
                            log.warn("Server ${it.key} doesn't work. Check it please.")
                            countNotAvailableTotal(it.key)
                        }
                    }
                }
            }

            metrics.metrics.forEach {
                // write metrics to prometheus
                countServerRequestsTotal(it.serverInfo)
                countServerResponseHttpCodeTotal(it.serverInfo, it.httpCode)
                timer(it.serverInfo).record(it.duration.toLong(), TimeUnit.MILLISECONDS)

                serversInformation[it.serverInfo]!!.windowRequestCount++
                serversInformation[it.serverInfo]!!.totalRequestCount++

                if (useDatabase) {
                    metricsRepository.save(MetricsEntryEntity(
                        ObjectId.get().toHexString(),
                        it.requestTime,
                        it.responseTime,
                        it.duration,
                        it.httpCode,
                        it.serverInfo
                    ))
                }
            }

            checkRequestDistribution(shouldCountForWindow)

            if (!metrics.shouldContinue) {
                break
            }
        }

        serversToRemove.forEach {
            serversInformation.remove(it)
            log.error("Server $it removed from list of available servers for long unavailable state.")
        }
    }

    private fun checkRequestDistribution(shouldCountForWindow: Boolean) {
        val idealShare = 1.0 / serversInformation.size

        // Check distribution for total requests
        distributionDiff(
            serversInformation.entries.sumOf { it.value.totalRequestCount }, idealShare
        ).forEach {
            if (!it.goodDistribution) {
                countTotalUnevenDistribution()
                log.debug("Total distribution. Server ${it.host} has an uneven distribution of requests: ${it.share * 100}%")
            }
        }

        // Check distribution for window
        if (shouldCountForWindow) {
            distributionDiff(
                serversInformation.entries.sumOf { it.value.windowRequestCount }, idealShare
            ).forEach {
                if (!it.goodDistribution) {
                    countWindowUnevenDistribution()
                    log.error("Window distribution. Server ${it.host} has an uneven distribution of requests: ${it.share * 100}%")
                }
            }
        }
    }

    private fun distributionDiff(requestsCount: Long, idealShare: Double): List<DistributionInfo> {
        val distributionInfoList = mutableListOf<DistributionInfo>()

        for ((server, info) in serversInformation) {
            val share = info.totalRequestCount.toDouble() / requestsCount
            val diff = abs(share - idealShare)
            if (diff > threshold) {
                distributionInfoList.add(DistributionInfo(false, server, share))
            } else {
                distributionInfoList.add(DistributionInfo(false, server, share))
            }
        }
        return listOf()
    }

    private fun countNotAvailableTotal(host: String) {
        Counter.builder("server_not_available_total")
            .tags("host", host)
            .register(meterRegistry)
            .increment()
    }

    private fun countTotalUnevenDistribution() {
        Counter.builder("server_uneven_distribution_total")
            .register(meterRegistry)
            .increment()
    }

    private fun countWindowUnevenDistribution() {
        Counter.builder("server_uneven_distribution_window")
            .register(meterRegistry)
            .increment()
    }

    private fun countServerRequestsTotal(host: String) {
        Counter.builder("server_requests_total")
            .tag("host", host)
            .register(meterRegistry)
            .increment()
    }

    private fun countServerResponseHttpCodeTotal(host: String, httpCode: Int) {
        Counter.builder("server_response_http_code_total")
            .tag("host", host)
            .tag("httpCode", httpCode.toString())
            .register(meterRegistry).increment()
    }

    private fun timer(host: String): Timer {
        return Timer.builder("server_request_duration_ms")
            .description("Request duration for each server")
            .tag("host", host).register(meterRegistry)
    }
}
