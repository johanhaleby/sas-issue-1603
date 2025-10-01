package se.haleby.sasissue1603

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration

@Component
data class Properties(
    @param:Value($$"${spring.security.oauth2.authorizationserver.client.oidc-debugger.registration.client-id}")
    val clientId: String,
    @param:Value($$"${spring.security.oauth2.authorizationserver.client.oidc-debugger.registration.client-secret}")
    private val configuredClientSecret: String,
    @param:Value($$"${spring.security.user.name}")
    val username: String,
    @param:Value($$"${spring.security.user.password}")
    private val configuredPassword: String,
    @param:Value($$"${server.servlet.session.timeout}")
    val sessionTimeout: Duration,
) {
    val password: String get() = configuredPassword.removePrefix("{noop}")
    val clientSecret: String get() = configuredClientSecret.removePrefix("{noop}")
}