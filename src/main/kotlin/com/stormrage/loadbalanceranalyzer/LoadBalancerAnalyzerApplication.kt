package com.stormrage.loadbalanceranalyzer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class LoadBalancerAnalyzerApplication

fun main(args: Array<String>) {
	runApplication<LoadBalancerAnalyzerApplication>(*args)
}
