package com.leapfinance.infopulse.sample

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
    import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class SampleServiceApplication

fun main(args: Array<String>) {
    runApplication<SampleServiceApplication>(*args)
}
