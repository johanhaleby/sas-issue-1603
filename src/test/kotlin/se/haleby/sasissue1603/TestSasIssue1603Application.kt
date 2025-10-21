package se.haleby.sasissue1603

import org.springframework.boot.fromApplication
import org.springframework.boot.with

fun main(args: Array<String>) {
    fromApplication<SasIssue1603Application>().with(TestcontainersConfiguration::class).run(*args)
}
