package io.github.jonnydevp.apitest.execution

import io.github.jonnydevp.apitest.spec.model.Endpoint
import io.github.jonnydevp.apitest.util.jackson
import io.restassured.RestAssured
import io.restassured.filter.Filter
import io.restassured.response.Response

/**
 * Тонкая обёртка над REST Assured: собирает HTTP-запрос из [Endpoint] и [RequestData],
 * выполняет его и возвращает унифицированный [ApiResponse].
 *
 * @param baseUrl базовый URL тестируемого API (например, `http://localhost:8080`)
 * @param filters фильтры REST Assured (например, Allure-фильтр для attach запроса/ответа)
 */
class ApiClient(
    private val baseUrl: String,
    private val filters: List<Filter> = emptyList(),
) {
    fun execute(endpoint: Endpoint, data: RequestData): ApiResponse {
        var request = RestAssured.given().baseUri(baseUrl)
        filters.forEach { request = request.filter(it) }

        data.headers.forEach { (name, value) -> request = request.header(name, value) }
        if (data.queryParams.isNotEmpty()) request = request.queryParams(data.queryParams)
        data.pathParams.forEach { (name, value) -> request = request.pathParam(name, value) }

        if (data.body != null) {
            request = request.contentType("application/json").body(jackson.writeValueAsString(data.body))
        }

        val response: Response = request.request(endpoint.method.name, endpoint.path)
        return toApiResponse(response)
    }

    private fun toApiResponse(response: Response): ApiResponse {
        val raw = response.body?.asString().orEmpty()
        val contentType = response.contentType
        val isJson = contentType?.contains("json", ignoreCase = true) == true
        val body = if (isJson && raw.isNotBlank()) runCatching { jackson.readTree(raw) }.getOrNull() else null
        val headers = response.headers.associate { it.name to it.value }
        return ApiResponse(
            statusCode = response.statusCode,
            contentType = contentType,
            body = body,
            rawBody = raw,
            headers = headers,
        )
    }
}
