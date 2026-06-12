package io.github.jonnydevp.apitest.spec.model

import com.fasterxml.jackson.databind.JsonNode

/** HTTP-метод операции OpenAPI */
enum class HttpMethod { GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS }

/** Расположение параметра запроса */
enum class ParamLocation { PATH, QUERY, HEADER, COOKIE }

/** Узел JSON-схемы во внутреннем представлении */
data class SchemaNode(val json: JsonNode) {
    val type: String? get() = json.get("type")?.asText()
    val format: String? get() = json.get("format")?.asText()
    val nullable: Boolean get() = json.get("nullable")?.asBoolean() ?: false

    fun isObject(): Boolean = type == "object" || json.has("properties")
    fun isArray(): Boolean = type == "array"
    fun enumValues(): List<JsonNode> = json.get("enum")?.toList() ?: emptyList()
    fun requiredProperties(): List<String> = json.get("required")?.map { it.asText() } ?: emptyList()
}

/** Параметр операции (path/query/header/cookie) */
data class ApiParameter(
    val name: String,
    val location: ParamLocation,
    val required: Boolean,
    val schema: SchemaNode?,
)

/** Тело запроса операции */
data class RequestBodySpec(
    val required: Boolean,
    val contentType: String,
    val schema: SchemaNode?,
)

/** Объявленный в спецификации ответ операции */
data class ResponseSpec(
    val statusCode: String,
    val contentType: String?,
    val schema: SchemaNode?,
    val headers: Set<String>,
) {
    val statusInt: Int? get() = statusCode.toIntOrNull()
    val isSuccess: Boolean get() = statusInt?.let { it in 200..299 } ?: false
}

/** Одна операция API: метод + путь + параметры + тело + объявленные ответы */
data class Endpoint(
    val operationId: String,
    val method: HttpMethod,
    val path: String,
    val summary: String?,
    val tags: List<String>,
    val parameters: List<ApiParameter>,
    val requestBody: RequestBodySpec?,
    val responses: List<ResponseSpec>,
) {
    /** Стабильный человекочитаемый идентификатор, например `GET /pets/{petId}` */
    val id: String get() = "${method.name} $path"

    fun pathParameters(): List<ApiParameter> = parameters.filter { it.location == ParamLocation.PATH }
    fun queryParameters(): List<ApiParameter> = parameters.filter { it.location == ParamLocation.QUERY }
    fun headerParameters(): List<ApiParameter> = parameters.filter { it.location == ParamLocation.HEADER }

    fun successResponses(): List<ResponseSpec> = responses.filter { it.isSuccess }
    fun responseFor(status: Int): ResponseSpec? =
        responses.firstOrNull { it.statusInt == status } ?: responses.firstOrNull { it.statusCode == "default" }

    fun primaryTag(): String = tags.firstOrNull() ?: "default"
}

/** Внутреннее представление всей OpenAPI-спецификации */
data class ApiSpec(
    val title: String,
    val version: String,
    val endpoints: List<Endpoint>,
) {
    fun endpoint(id: String): Endpoint =
        endpoints.firstOrNull { it.id == id } ?: error("Эндпоинт не найден: $id")

    fun endpointsByTag(): Map<String, List<Endpoint>> = endpoints.groupBy { it.primaryTag() }
}
