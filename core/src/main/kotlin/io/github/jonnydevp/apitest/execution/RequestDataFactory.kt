package io.github.jonnydevp.apitest.execution

import com.fasterxml.jackson.databind.JsonNode
import io.github.jonnydevp.apitest.data.SchemaDataGenerator
import io.github.jonnydevp.apitest.spec.model.ApiParameter
import io.github.jonnydevp.apitest.spec.model.Endpoint

/** Сборка валидных данных запроса для эндпоинта на основе схем его параметров и тела. */
object RequestDataFactory {

    /** Happy-path данные: все path-параметры, обязательные query/header и тело запроса. */
    fun valid(endpoint: Endpoint): RequestData = RequestData(
        pathParams = endpoint.pathParameters().associate { it.name to scalarFor(it) },
        queryParams = endpoint.queryParameters().filter { it.required }.associate { it.name to scalarFor(it) },
        headers = endpoint.headerParameters().filter { it.required }.associate { it.name to scalarFor(it).toString() },
        body = endpoint.requestBody?.schema?.let { SchemaDataGenerator.generate(it) },
    )

    private fun scalarFor(parameter: ApiParameter): Any {
        val schema = parameter.schema ?: return "1"
        return toScalar(SchemaDataGenerator.generate(schema))
    }

    private fun toScalar(node: JsonNode): Any = when {
        node.isTextual -> node.asText()
        node.isInt -> node.asInt()
        node.isNumber -> node.asLong()
        node.isBoolean -> node.asBoolean()
        node.isNull -> ""
        else -> node.asText()
    }
}