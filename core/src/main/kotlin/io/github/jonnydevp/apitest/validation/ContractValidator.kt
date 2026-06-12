package io.github.jonnydevp.apitest.validation

import io.github.jonnydevp.apitest.execution.ApiResponse
import io.github.jonnydevp.apitest.spec.model.Endpoint
import io.github.jonnydevp.apitest.spec.model.ResponseSpec

/**
 * Детектор расхождений между OpenAPI-спецификацией и фактическим ответом API.
 *
 * Сверяет фактический [ApiResponse] с объявленным в спецификации [ResponseSpec]:
 * статус-код, content-type, схему тела и наличие объявленных заголовков.
 */
object ContractValidator {

    fun validate(endpoint: Endpoint, response: ApiResponse): List<ContractMismatch> {
        val mismatches = mutableListOf<ContractMismatch>()

        val declared = endpoint.responseFor(response.statusCode)
        if (declared == null) {
            val declaredCodes = endpoint.responses.mapNotNull { it.statusInt }.sorted()
            mismatches += ContractMismatch(
                MismatchType.UNDECLARED_STATUS, endpoint.id, response.statusCode,
                "статус не объявлен в спецификации; объявлены: $declaredCodes",
            )
            return mismatches
        }

        checkContentType(endpoint, response, declared, mismatches)
        checkBody(endpoint, response, declared, mismatches)
        checkHeaders(endpoint, response, declared, mismatches)
        return mismatches
    }

    private fun checkContentType(
        endpoint: Endpoint,
        response: ApiResponse,
        declared: ResponseSpec,
        out: MutableList<ContractMismatch>,
    ) {
        val expected = declared.contentType ?: return
        if (response.rawBody.isBlank()) return
        val actual = response.contentType
        if (actual == null || !actual.substringBefore(';').trim().equals(expected, ignoreCase = true)) {
            out += ContractMismatch(
                MismatchType.CONTENT_TYPE, endpoint.id, response.statusCode,
                "content-type '$actual' не совпадает с объявленным '$expected'",
            )
        }
    }

    private fun checkBody(
        endpoint: Endpoint,
        response: ApiResponse,
        declared: ResponseSpec,
        out: MutableList<ContractMismatch>,
    ) {
        val schema = declared.schema ?: return
        val body = response.body
        if (body == null) {
            out += ContractMismatch(
                MismatchType.BODY_SCHEMA, endpoint.id, response.statusCode,
                "ожидалось JSON-тело по схеме, но тело отсутствует или не является JSON",
            )
            return
        }
        SchemaValidator.validate(schema, body).forEach { error ->
            out += ContractMismatch(MismatchType.BODY_SCHEMA, endpoint.id, response.statusCode, error)
        }
    }

    private fun checkHeaders(
        endpoint: Endpoint,
        response: ApiResponse,
        declared: ResponseSpec,
        out: MutableList<ContractMismatch>,
    ) {
        val present = response.headers.keys.map { it.lowercase() }.toSet()
        declared.headers.filterNot { it.lowercase() in present }.forEach { missing ->
            out += ContractMismatch(
                MismatchType.MISSING_HEADER, endpoint.id, response.statusCode,
                "в ответе отсутствует объявленный заголовок '$missing'",
            )
        }
    }
}
