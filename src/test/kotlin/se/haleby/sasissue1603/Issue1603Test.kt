package se.haleby.sasissue1603

import io.restassured.RestAssured
import io.restassured.config.CsrfConfig.csrfConfig
import io.restassured.config.RedirectConfig.redirectConfig
import io.restassured.config.SessionConfig.sessionConfig
import io.restassured.filter.session.SessionFilter
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayNameGeneration(DisplayNameGenerator.Simple::class)
class Issue1603Test {
    private val log = loggerFor<Issue1603Test>()

    @LocalServerPort
    private val port: Int = 0

    @Autowired
    lateinit var properties: Properties

    @Test
    fun `can logout with valid id token hint and session`() {
        // Given
        val sessionFilter = SessionFilter()
        val idToken = loginAndExchangeCodeForTokens(sessionFilter)

        // When
        val redirectUrlAfterLogout = logout(idToken, sessionFilter)

        // Then
        assertThat(redirectUrlAfterLogout).endsWith(expectedUriAfterLogout)
    }

    @Test
    fun `can logout with invalid id token hint and valid session`() {
        // Given
        val sessionFilter = SessionFilter()
        val idToken = loginAndExchangeCodeForTokens(sessionFilter).replace('A', 'B')

        // When
        val redirectUrlAfterLogout = logout(idToken, sessionFilter)

        // Then
        assertThat(redirectUrlAfterLogout).endsWith(expectedUriAfterLogout)
    }

    @Test
    fun `can logout without id token hint but valid session`() {
        // Given
        val sessionFilter = SessionFilter()
        loginAndExchangeCodeForTokens(sessionFilter)

        // When
        val redirectUrlAfterLogout = logout(idToken = null, sessionFilter)

        // Then
        assertThat(redirectUrlAfterLogout).endsWith(expectedUriAfterLogout)
    }

    @Test
    fun `can logout with valid id token hint but no session`() {
        // Given
        val sessionFilter = SessionFilter()
        val idToken = loginAndExchangeCodeForTokens(sessionFilter)

        // When
        val redirectUrlAfterLogout = logout(idToken, sessionFilter = null)

        // Then
        assertThat(redirectUrlAfterLogout).endsWith(expectedUriAfterLogout)
    }

    @Test
    fun `can logout without id token hint and no session`() {
        // Given
        val sessionFilter = SessionFilter()
        loginAndExchangeCodeForTokens(sessionFilter)

        // When
        val redirectUrlAfterLogout = logout(idToken = null, sessionFilter = null)

        // Then
        assertThat(redirectUrlAfterLogout).endsWith(expectedUriAfterLogout)
    }

    // I couldn't find a way to configure id-token timeout in application.yaml so there's not test for that
    @Test
    fun `can logout with valid id token hint and expired session`() {
        // Given
        val sessionFilter = SessionFilter()
        val idToken = loginAndExchangeCodeForTokens(sessionFilter)

        Thread.sleep(properties.sessionTimeout.plusSeconds(1))

        // When
        val redirectUrlAfterLogout = logout(idToken, sessionFilter)

        // Then
        assertThat(redirectUrlAfterLogout).endsWith(expectedUriAfterLogout)
    }

    @BeforeEach
    fun `configure rest assured`() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
        RestAssured.port = port
        RestAssured.config = RestAssured.config()
            .sessionConfig(sessionConfig().sessionIdName("JSESSIONID"))
            .redirect(redirectConfig().followRedirects(false))
            .csrfConfig(csrfConfig().csrfMetaTagName("_csrf"))
    }

    @AfterEach
    fun `reset rest assured`() {
        RestAssured.reset()
    }

    private fun loginAndExchangeCodeForTokens(sessionFilter: SessionFilter): String {
        val loginRedirectUri = startOidcLoginFlow(properties.clientId, sessionFilter)
        val redirectUri = loginUsingFormAuthentication(loginRedirectUri, properties.username, properties.password, sessionFilter)
        val authorizationCode = getAuthorizationCode(redirectUri, sessionFilter)
        val idToken = exchangeCodeForTokens(authorizationCode, properties.clientId, properties.clientSecret)
        return idToken
    }

    private fun startOidcLoginFlow(clientId: String, sessionFilter: SessionFilter): String {
        log.info("Initializing oidc authorization for client $clientId")
        val response = Given {
            filter(sessionFilter)
            queryParam("response_type", "code")
            queryParam("client_id", clientId)
            queryParam("redirect_uri", "http://127.0.0.1:8080/login/oauth2/code/$clientId")
            queryParam("scope", "openid profile offline_access")
            queryParam("state", "xyz")
            queryParam("nonce", "abc")
        } When {
            get("/oauth2/authorize")
        } Then {
        } Extract {
            response()
        }

        return response.header("Location")
    }

    private fun loginUsingFormAuthentication(loginUrl: String, username: String, password: String, sessionFilter: SessionFilter): String {
        log.info("Logging in with $username by posting to $loginUrl")
        return Given {
            filter(sessionFilter)
            csrf(URI.create(loginUrl).path)
            formParam("username", username)
            formParam("password", password)
        } When {
            post(loginUrl)
        } Then {
            statusCode(302)
        } Extract {
            header("Location")
        }
    }

    private fun getAuthorizationCode(uri: String, sessionFilter: SessionFilter): String {
        log.info("Getting authorization code for uri $uri")
        val location = Given {
            filter(sessionFilter)
            urlEncodingEnabled(false)
        } When {
            get(uri)
        } Then {
            statusCode(302)
        } Extract {
            header("Location")
        }
        return UriComponentsBuilder.fromUriString(location)
            .build()
            .queryParams
            .getFirst("code")!!
    }

    private fun exchangeCodeForTokens(authorizationCode: String, clientId: String, clientSecret: String): String =
        Given {
            auth().preemptive().basic(clientId, clientSecret)
            formParam("grant_type", "authorization_code")
            formParam("code", authorizationCode)
            formParam("redirect_uri", "http://127.0.0.1:8080/login/oauth2/code/$clientId")
        } When {
            post("/oauth2/token")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getString("id_token")
        }

    private fun logout(idToken: String? = null, sessionFilter: SessionFilter? = null): String {
        val logoutUrl = getLogoutUrl()

        val location = Given {
            apply { if (idToken != null) queryParam("id_token_hint", idToken) }
            apply { if (sessionFilter != null) filter(sessionFilter) }
        } When {
            get(logoutUrl)
        } Then {
            statusCode(302)
        } Extract {
            header("Location")
        }
        return location
    }

    private fun getLogoutUrl(): String =
        When {
            get("/.well-known/openid-configuration")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath().getString("end_session_endpoint")
        }

    val expectedUriAfterLogout: String get() = "http://localhost:$port/"

}