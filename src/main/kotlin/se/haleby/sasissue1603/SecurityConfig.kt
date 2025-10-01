package se.haleby.sasissue1603

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint

@Configuration
class SecurityConfig {

    @Bean
    @Order(1)
    fun authorizationServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val asConfigurer = OAuth2AuthorizationServerConfigurer()
        val endpoints = asConfigurer.endpointsMatcher

        http
            .securityMatcher(endpoints)
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .csrf { it.ignoringRequestMatchers(endpoints) }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(LoginUrlAuthenticationEntryPoint("/login"))
            }
            .with(asConfigurer) { asCustomizer ->
                asCustomizer.oidc(Customizer.withDefaults()) // enable OIDC
            }
            .oauth2ResourceServer { it.jwt(Customizer.withDefaults()) }

        return http.build()
    }

    @Bean
    @Order(2)
    fun applicationSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .formLogin(Customizer.withDefaults())
            .build()
}