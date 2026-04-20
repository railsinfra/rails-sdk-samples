package sample

import com.rails.api.client.okhttp.RailsOkHttpClient
import com.rails.api.errors.RailsServiceException
import com.rails.api.models.accounts.AccountCloseParams
import com.rails.api.models.accounts.AccountRetrieveParams
import com.rails.api.models.accounts.AccountUpdateStatusParams
import com.rails.api.models.transactions.TransactionListByAccountParams
import com.rails.api.models.transactions.TransactionRetrieveParams
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.request.path
import io.ktor.server.request.queryString
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class StatusBody(val status: String)

/** This sample always uses the sandbox environment for Rails account API calls. */
@Suppress("UNUSED_PARAMETER")
private fun resolveXEnvironment(call: ApplicationCall): String = "sandbox"

private fun ktorStatusForUpstreamCode(code: Int): HttpStatusCode = when (code) {
    200 -> HttpStatusCode.OK
    201 -> HttpStatusCode.Created
    204 -> HttpStatusCode.NoContent
    400 -> HttpStatusCode.BadRequest
    401 -> HttpStatusCode.Unauthorized
    403 -> HttpStatusCode.Forbidden
    404 -> HttpStatusCode.NotFound
    409 -> HttpStatusCode.Conflict
    422 -> HttpStatusCode.UnprocessableEntity
    500 -> HttpStatusCode.InternalServerError
    502 -> HttpStatusCode.BadGateway
    503 -> HttpStatusCode.ServiceUnavailable
    else -> if (code in 100..599) HttpStatusCode(code, "Upstream") else HttpStatusCode.InternalServerError
}

/**
 * Shared client for forwarded HTTP calls. Trust-all TLS when:
 * - env `RAILS_INSECURE_SSL=true`, or
 * - JVM system property `rails.insecure.ssl=true` (use `./gradlew run -PrailsInsecureSsl=true` if the Gradle daemon ignores your shell env).
 *
 * Dev/staging private CA only — do not enable for live traffic.
 */
private val proxyHttpClient: HttpClient by lazy { buildInsecureAwareHttpClient() }

private fun isInsecureTls(): Boolean {
    if (System.getenv("RAILS_INSECURE_SSL")?.equals("true", ignoreCase = true) == true) return true
    if (System.getProperty("rails.insecure.ssl")?.equals("true", ignoreCase = true) == true) return true
    return false
}

private fun buildInsecureAwareHttpClient(): HttpClient {
    val insecure = isInsecureTls()
    System.err.println(
        "[rails-sdk-sample] Proxy HttpClient trust-all TLS: ${if (insecure) "ON" else "OFF"} " +
            "(set RAILS_INSECURE_SSL=true or run with -PrailsInsecureSsl=true if PKIX still fails)",
    )
    if (!insecure) return HttpClient.newHttpClient()
    val trustAll = arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        },
    )
    val ctx = SSLContext.getInstance("TLS").apply { init(null, trustAll, SecureRandom()) }
    return HttpClient.newBuilder().sslContext(ctx).build()
}

private fun readResource(path: String): String =
    checkNotNull({}.javaClass.getResourceAsStream(path)) { "missing resource $path" }
        .bufferedReader().use { it.readText() }

/** Ktor rejects an empty status reason; java.net.http has no reason phrase to forward. */
private fun proxiedStatus(code: Int): HttpStatusCode = when (code) {
    200 -> HttpStatusCode.OK
    201 -> HttpStatusCode.Created
    204 -> HttpStatusCode.NoContent
    400 -> HttpStatusCode.BadRequest
    401 -> HttpStatusCode.Unauthorized
    403 -> HttpStatusCode.Forbidden
    404 -> HttpStatusCode.NotFound
    409 -> HttpStatusCode.Conflict
    422 -> HttpStatusCode.UnprocessableEntity
    500 -> HttpStatusCode.InternalServerError
    502 -> HttpStatusCode.BadGateway
    503 -> HttpStatusCode.ServiceUnavailable
    else -> if (code in 100..599) HttpStatusCode(code, "Proxied") else HttpStatusCode.InternalServerError
}

private suspend fun forwardCreateAccount(call: ApplicationCall, baseUrl: String, apiKey: String) {
    val body = call.receiveText()
    val http = proxyHttpClient
    val req = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl.trimEnd('/')}/api/v1/accounts"))
        .header("Content-Type", "application/json")
        .header("X-API-Key", apiKey)
        .header("X-Environment", "sandbox")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()
    val res = http.send(req, HttpResponse.BodyHandlers.ofString())
    call.respondText(res.body(), ContentType.Application.Json, proxiedStatus(res.statusCode()))
}

private fun genIdempotencyKey(prefix: String): String =
    "$prefix-${System.currentTimeMillis()}-${UUID.randomUUID().toString().replace("-", "")}"

private suspend fun forwardDeposit(call: ApplicationCall, baseUrl: String, apiKey: String) {
    val id = call.parameters["id"] ?: return call.respondError(HttpStatusCode.BadRequest, "missing id")
    val body = call.receiveText()
    if (body.isBlank()) return call.respondError(HttpStatusCode.BadRequest, "missing body")
    val idempotencyKey = call.request.headers["Idempotency-Key"] ?: genIdempotencyKey("dep")
    val http = proxyHttpClient
    val reqBuilder = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl.trimEnd('/')}/api/v1/accounts/$id/deposit"))
        .header("Content-Type", "application/json")
        .header("X-API-Key", apiKey)
        .header("Idempotency-Key", idempotencyKey)
        .header("X-Environment", "sandbox")
        .POST(HttpRequest.BodyPublishers.ofString(body))
    val res = http.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString())
    call.respondText(res.body(), ContentType.Application.Json, proxiedStatus(res.statusCode()))
}

private suspend fun forwardTransfer(call: ApplicationCall, baseUrl: String, apiKey: String) {
    val id = call.parameters["id"] ?: return call.respondError(HttpStatusCode.BadRequest, "missing id")
    val body = call.receiveText()
    if (body.isBlank()) return call.respondError(HttpStatusCode.BadRequest, "missing body")
    val idempotencyKey = call.request.headers["Idempotency-Key"] ?: genIdempotencyKey("xfr")
    val http = proxyHttpClient
    val reqBuilder = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl.trimEnd('/')}/api/v1/accounts/$id/transfer"))
        .header("Content-Type", "application/json")
        .header("X-API-Key", apiKey)
        .header("Idempotency-Key", idempotencyKey)
        .header("X-Environment", "sandbox")
        .POST(HttpRequest.BodyPublishers.ofString(body))
    val res = http.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString())
    call.respondText(res.body(), ContentType.Application.Json, proxiedStatus(res.statusCode()))
}

private suspend fun forwardWithdraw(call: ApplicationCall, baseUrl: String, apiKey: String) {
    val id = call.parameters["id"] ?: return call.respondError(HttpStatusCode.BadRequest, "missing id")
    val body = call.receiveText()
    if (body.isBlank()) return call.respondError(HttpStatusCode.BadRequest, "missing body")
    val idempotencyKey = call.request.headers["Idempotency-Key"] ?: genIdempotencyKey("wdr")
    val http = proxyHttpClient
    val reqBuilder = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl.trimEnd('/')}/api/v1/accounts/$id/withdraw"))
        .header("Content-Type", "application/json")
        .header("X-API-Key", apiKey)
        .header("Idempotency-Key", idempotencyKey)
        .header("X-Environment", "sandbox")
        .POST(HttpRequest.BodyPublishers.ofString(body))
    val res = http.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString())
    call.respondText(res.body(), ContentType.Application.Json, proxiedStatus(res.statusCode()))
}

private suspend fun rawGet(call: ApplicationCall, baseUrl: String, apiKey: String) {
    val path = call.request.queryParameters["path"] ?: "api/v1/accounts"
    val http = proxyHttpClient
    val req = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl.trimEnd('/')}/${path.trimStart('/')}"))
        .header("X-API-Key", apiKey)
        .GET()
        .build()
    val res = http.send(req, HttpResponse.BodyHandlers.ofString())
    call.respondText(res.body(), ContentType.Application.Json, proxiedStatus(res.statusCode()))
}

private suspend fun rawPost(call: ApplicationCall, baseUrl: String, apiKey: String) {
    val path = call.request.queryParameters["path"] ?: "api/v1/accounts"
    val body = call.receiveText()
    val http = proxyHttpClient
    val req = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl.trimEnd('/')}/${path.trimStart('/')}"))
        .header("Content-Type", "application/json")
        .header("X-API-Key", apiKey)
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()
    val res = http.send(req, HttpResponse.BodyHandlers.ofString())
    call.respondText(res.body(), ContentType.Application.Json, proxiedStatus(res.statusCode()))
}

private suspend fun forwardListAccounts(call: ApplicationCall, baseUrl: String, apiKey: String) {
    val qs = call.request.queryString().let { if (it.isEmpty()) "" else "?$it" }
    val http = proxyHttpClient
    val req = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl.trimEnd('/')}/api/v1/accounts$qs"))
        .header("X-API-Key", apiKey)
        .header("X-Environment", "sandbox")
        .GET()
        .build()
    val res = http.send(req, HttpResponse.BodyHandlers.ofString())
    call.respondText(res.body(), ContentType.Application.Json, proxiedStatus(res.statusCode()))
}

private fun normalizeBaseUrl(raw: String): String {
    val t = raw.trim().trimEnd('/')
    if (t.isEmpty()) return "https://api.railsinfra.com/"
    val withScheme = when {
        t.startsWith("http://", ignoreCase = true) -> t
        t.startsWith("https://", ignoreCase = true) -> t
        else -> "https://$t"
    }
    return "$withScheme/"
}

fun main() {
    val baseUrl = normalizeBaseUrl(System.getenv("RAILS_BASE_URL") ?: "")
    val apiKey = System.getenv("RAILS_API_KEY") ?: ""
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8081

    val client = RailsOkHttpClient.builder()
        .apiKey(apiKey)
        .baseUrl(baseUrl)
        .build()

    embeddedServer(CIO, port = port) {
        install(ContentNegotiation) { jackson() }
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Accept)
            allowHeader("X-Environment")
            allowHeader("Idempotency-Key")
            allowHeader("X-API-Key")
            allowHeader("x-correlation-id")
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Patch)
            allowMethod(HttpMethod.Delete)
            allowNonSimpleContentTypes = true
        }
        install(StatusPages) {
            exception<RailsServiceException> { call, cause ->
                cause.printStackTrace()
                val code = cause.statusCode()
                val status = ktorStatusForUpstreamCode(code)
                call.respond(
                    status,
                    ErrorResponse(
                        status = code,
                        message = cause.message ?: cause.javaClass.simpleName,
                        exception = cause.javaClass.name,
                        path = call.request.path(),
                    ),
                )
            }
            exception<Throwable> { call, cause ->
                cause.printStackTrace()
                val status = HttpStatusCode.InternalServerError
                call.respond(
                    status,
                    ErrorResponse(
                        status = status.value,
                        message = cause.message ?: cause.javaClass.simpleName,
                        exception = cause.javaClass.name,
                        path = call.request.path(),
                    ),
                )
            }
        }
        routing {
            get("/") {
                call.respondText(readResource("/swagger-ui.html"), ContentType.Text.Html)
            }
            get("/openapi.json") {
                call.respondText(readResource("/openapi.json"), ContentType.Application.Json)
            }

            get("/health") { call.respondText("""{"status":"ok"}""", ContentType.Application.Json) }

            // Forward create account so holder-based (email, first_name, last_name) and legacy (user_id) both work.
            post("/api/accounts") { forwardCreateAccount(call, baseUrl, apiKey) }
            post("/api/v1/accounts") { forwardCreateAccount(call, baseUrl, apiKey) }
            get("/api/accounts") { forwardListAccounts(call, baseUrl, apiKey) }
            get("/api/v1/accounts") { forwardListAccounts(call, baseUrl, apiKey) }

            get("/api/accounts/{id}") {
                val id = call.parameters["id"] ?: return@get call.respondError(HttpStatusCode.BadRequest, "missing id")
                val xEnv = resolveXEnvironment(call)
                val params = AccountRetrieveParams.builder().putAdditionalHeader("X-Environment", xEnv).build()
                val res = client.accounts().retrieve(id, params)
                call.respond(res)
            }
            get("/api/v1/accounts/{id}") {
                val id = call.parameters["id"] ?: return@get call.respondError(HttpStatusCode.BadRequest, "missing id")
                val xEnv = resolveXEnvironment(call)
                val params = AccountRetrieveParams.builder().putAdditionalHeader("X-Environment", xEnv).build()
                val res = client.accounts().retrieve(id, params)
                call.respond(res)
            }

            delete("/api/accounts/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respondError(HttpStatusCode.BadRequest, "missing id")
                val xEnv = resolveXEnvironment(call)
                val params = AccountCloseParams.builder().putAdditionalHeader("X-Environment", xEnv).build()
                val res = client.accounts().close(id, params)
                call.respond(res)
            }
            delete("/api/v1/accounts/{id}") {
                val id = call.parameters["id"] ?: return@delete call.respondError(HttpStatusCode.BadRequest, "missing id")
                val xEnv = resolveXEnvironment(call)
                val params = AccountCloseParams.builder().putAdditionalHeader("X-Environment", xEnv).build()
                val res = client.accounts().close(id, params)
                call.respond(res)
            }

            post("/api/accounts/{id}/deposit") { forwardDeposit(call, baseUrl, apiKey) }
            post("/api/v1/accounts/{id}/deposit") { forwardDeposit(call, baseUrl, apiKey) }

            post("/api/accounts/{id}/transfer") { forwardTransfer(call, baseUrl, apiKey) }
            post("/api/v1/accounts/{id}/transfer") { forwardTransfer(call, baseUrl, apiKey) }

            patch("/api/accounts/{id}/status") {
                val id = call.parameters["id"] ?: return@patch call.respondError(HttpStatusCode.BadRequest, "missing id")
                val body = call.receive<StatusBody>()
                val xEnv = resolveXEnvironment(call)
                val params =
                    AccountUpdateStatusParams.builder()
                        .id(id)
                        .status(com.rails.api.models.accounts.AccountUpdateStatusParams.Status.of(body.status))
                        .putAdditionalHeader("X-Environment", xEnv)
                        .build()
                val res = client.accounts().updateStatus(params)
                call.respond(res)
            }
            patch("/api/v1/accounts/{id}/status") {
                val id = call.parameters["id"] ?: return@patch call.respondError(HttpStatusCode.BadRequest, "missing id")
                val body = call.receive<StatusBody>()
                val xEnv = resolveXEnvironment(call)
                val params =
                    AccountUpdateStatusParams.builder()
                        .id(id)
                        .status(com.rails.api.models.accounts.AccountUpdateStatusParams.Status.of(body.status))
                        .putAdditionalHeader("X-Environment", xEnv)
                        .build()
                val res = client.accounts().updateStatus(params)
                call.respond(res)
            }
            patch("/api/v1/accounts/{id}") {
                val id = call.parameters["id"] ?: return@patch call.respondError(HttpStatusCode.BadRequest, "missing id")
                val body = call.receive<StatusBody>()
                val xEnv = resolveXEnvironment(call)
                val params =
                    AccountUpdateStatusParams.builder()
                        .id(id)
                        .status(com.rails.api.models.accounts.AccountUpdateStatusParams.Status.of(body.status))
                        .putAdditionalHeader("X-Environment", xEnv)
                        .build()
                val res = client.accounts().updateStatus(params)
                call.respond(res)
            }

            post("/api/accounts/{id}/withdraw") { forwardWithdraw(call, baseUrl, apiKey) }
            post("/api/v1/accounts/{id}/withdraw") { forwardWithdraw(call, baseUrl, apiKey) }

            get("/api/raw/get") { rawGet(call, baseUrl, apiKey) }
            get("/api/v1/raw/get") { rawGet(call, baseUrl, apiKey) }
            post("/api/raw/post") { rawPost(call, baseUrl, apiKey) }
            post("/api/v1/raw/post") { rawPost(call, baseUrl, apiKey) }

            get("/api/transactions/{id}") {
                val id = call.parameters["id"] ?: return@get call.respondError(HttpStatusCode.BadRequest, "missing id")
                val xEnv = resolveXEnvironment(call)
                val params = TransactionRetrieveParams.builder().putAdditionalHeader("X-Environment", xEnv).build()
                val res = client.transactions().retrieve(id, params)
                call.respond(res)
            }
            get("/api/v1/transactions/{id}") {
                val id = call.parameters["id"] ?: return@get call.respondError(HttpStatusCode.BadRequest, "missing id")
                val xEnv = resolveXEnvironment(call)
                val params = TransactionRetrieveParams.builder().putAdditionalHeader("X-Environment", xEnv).build()
                val res = client.transactions().retrieve(id, params)
                call.respond(res)
            }

            get("/api/accounts/{accountId}/transactions") {
                val accountId = call.parameters["accountId"] ?: return@get call.respondError(HttpStatusCode.BadRequest, "missing accountId")
                val limit = call.request.queryParameters["limit"]?.toLongOrNull()
                val xEnv = resolveXEnvironment(call)
                val b = TransactionListByAccountParams.builder().accountId(accountId).putAdditionalHeader("X-Environment", xEnv)
                limit?.let { b.limit(it) }
                val params = b.build()
                val res = client.transactions().listByAccount(params)
                call.respond(res)
            }
            get("/api/v1/accounts/{accountId}/transactions") {
                val accountId = call.parameters["accountId"] ?: return@get call.respondError(HttpStatusCode.BadRequest, "missing accountId")
                val limit = call.request.queryParameters["limit"]?.toLongOrNull()
                val xEnv = resolveXEnvironment(call)
                val b = TransactionListByAccountParams.builder().accountId(accountId).putAdditionalHeader("X-Environment", xEnv)
                limit?.let { b.limit(it) }
                val params = b.build()
                val res = client.transactions().listByAccount(params)
                call.respond(res)
            }
        }
    }.start(wait = true)
}
