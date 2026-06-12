package io.github.jonnydevp.apitest.execution

import com.fasterxml.jackson.databind.JsonNode

/** Данные одного HTTP-запроса: значения параметров и тело */
data class RequestData(
    val pathParams: Map<String, Any> = emptyMap(),
    val queryParams: Map<String, Any> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val body: JsonNode? = null,
)
