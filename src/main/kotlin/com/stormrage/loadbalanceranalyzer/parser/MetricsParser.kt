package com.stormrage.loadbalanceranalyzer.parser

import com.stormrage.loadbalanceranalyzer.model.MetricsEntry
import com.stormrage.loadbalanceranalyzer.model.MetricsParserAnswer
import com.stormrage.loadbalanceranalyzer.model.MetricsParserEntity
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.RandomAccessFile

@Service
class MetricsParser {

    @Value("\${metrics.filepath}")
    private lateinit var filePath: String

    private lateinit var file: RandomAccessFile

    companion object {
        private const val READ_MODE = "r"
        private const val MAX_METRICS_LIST_SIZE = 500
        private var lastReadPosition = 0L
    }

    @PostConstruct
    fun init() {
        file = RandomAccessFile(File(filePath), READ_MODE)
    }

    fun getMetrics(): MetricsParserEntity {
        val metrics = mutableListOf<MetricsEntry>()
        val metricsText = getNewMetricsFromFile()
        metricsText.metrics.forEach {
            metrics.add(parseMetric(it))
        }

        return MetricsParserEntity(
            metrics,
            metricsText.shouldContinue
        )
    }

    private fun getNewMetricsFromFile(): MetricsParserAnswer {
        file.seek(lastReadPosition)

        val newMetrics = mutableListOf<String>()
        var line: String?
        var amount = 0

        while (true) {
            line = file.readLine()
            if (line == null) {
                break
            }
            if (amount > MAX_METRICS_LIST_SIZE) {
                break
            }
            newMetrics.add(line)
            amount++
        }

        lastReadPosition = file.filePointer
        return MetricsParserAnswer(
            newMetrics,
            amount > MAX_METRICS_LIST_SIZE
        )
    }

    private fun parseMetric(log: String): MetricsEntry {
        val parts = log.split(",")
        return MetricsEntry(
            parts[0].toLong(),
            parts[1].toLong(),
            parts[2].toInt(),
            parts[3].toInt(),
            parts[4]
        )
    }
}
