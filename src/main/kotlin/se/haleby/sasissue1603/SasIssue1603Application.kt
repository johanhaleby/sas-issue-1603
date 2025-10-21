package se.haleby.sasissue1603

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference


inline fun <reified T : Any> loggerFor(): Logger = LoggerFactory.getLogger(T::class.java)

@SpringBootApplication
class SasIssue1603Application {
    @Bean("tokenExpiryRef")
    fun tokenExpiryRef(): AtomicReference<Instant?> = AtomicReference(null)

    @Bean
    fun jwtCustomizer(): OAuth2TokenCustomizer<JwtEncodingContext> {
        return OAuth2TokenCustomizer { context: JwtEncodingContext ->
            val claims = context.claims
            if (context.tokenType.value == OidcParameterNames.ID_TOKEN) {
                val instant = tokenExpiryRef().get()
                if (instant != null) {
                    claims.issuedAt(instant.minusMillis(1))
                    claims.expiresAt(instant)
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<SasIssue1603Application>(*args)
}
