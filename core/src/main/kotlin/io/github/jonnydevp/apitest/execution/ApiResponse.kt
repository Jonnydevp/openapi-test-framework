package io.github.jonnydevp.apitest.execution

import com.fasterxml.jackson.databind.JsonNode

/** Унифицированное представление HTTP-ответа для валидации */
data class ApiResponse(
    val statusCode: Int,
    val contentType: String?,
    val body: JsonNode?,
    val rawBody: String,
    val headers: Map<String, String>,
) {
    val isJson: Boolean get() = contentType?.contains("json", ignoreCase = true) == true
}
