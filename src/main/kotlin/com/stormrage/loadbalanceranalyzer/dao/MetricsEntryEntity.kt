package com.stormrage.loadbalanceranalyzer.dao

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "metrics")
data class MetricsEntryEntity(
    @Id val id: String,
    val requestTime: Long,
    val responseTime: Long,
    val duration: Int,
    val httpCode: Int,
    val serverInfo: String
)
