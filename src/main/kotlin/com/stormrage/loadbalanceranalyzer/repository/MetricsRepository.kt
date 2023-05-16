package com.stormrage.loadbalanceranalyzer.repository

import com.stormrage.loadbalanceranalyzer.dao.MetricsEntryEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface MetricsRepository : MongoRepository<MetricsEntryEntity, String> {
    fun findByRequestTimeBetween(start: Long, end: Long, pageable: Pageable): List<MetricsEntryEntity>
}
