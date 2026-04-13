package sample

import com.fasterxml.jackson.annotation.JsonInclude
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path
import io.ktor.server.response.respond

/**
 * Uniform JSON error body for this sample (handlers + [io.ktor.server.plugins.statuspages.StatusPages]).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val status: Int,
    val message: String,
    val exception: String? = null,
    val path: String? = null,
)

suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String, exception: String? = null) {
    respond(status, ErrorResponse(status.value, message, exception, request.path()))
}
