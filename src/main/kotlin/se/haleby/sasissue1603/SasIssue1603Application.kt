package se.haleby.sasissue1603

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

inline fun <reified T : Any> loggerFor(): Logger = LoggerFactory.getLogger(T::class.java)

@SpringBootApplication
class SasIssue1603Application

fun main(args: Array<String>) {
    runApplication<SasIssue1603Application>(*args)
}
