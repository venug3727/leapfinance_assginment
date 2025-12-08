package com.leapfinance.infopulse.collector

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(exclude = [MongoAutoConfiguration::class])
@EnableScheduling
class CollectorServiceApplication

fun main(args: Array<String>) {
    runApplication<CollectorServiceApplication>(*args)
}
