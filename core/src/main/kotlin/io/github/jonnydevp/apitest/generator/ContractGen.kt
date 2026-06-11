package io.github.jonnydevp.apitest.generator

import io.github.jonnydevp.apitest.execution.ApiResponse
import io.github.jonnydevp.apitest.execution.RequestDataFactory
import io.github.jonnydevp.apitest.spec.model.Endpoint

/**
 * Контрактный генератор: валидный запрос и проверка соответствия ответа спецификации на
 * «поверхностном» уровне — статус-код объявлен в спеке и content-type совпадает с ожидаемым.
 *
 * Глубокая валидация тела по JSON-схеме подключается отдельно (см. модуль `validation`).
 */
object ContractGen : TestGenerator {

    override fun generate(endpoints: List<Endpoint>): List<GeneratedTest> = endpoints.map { endpoint ->
        GeneratedTest(TestKind.CONTRACT, "contract: ${endpoint.id}", endpoint) { client ->
            val response = client.execute(endpoint, RequestDataFactory.valid(endpoint))
            assertStatusDeclared(endpoint, response)
            assertContentType(endpoint, response)
        }
    }

    private fun assertStatusDeclared(endpoint: Endpoint, response: ApiResponse) {
        val declared = endpoint.responses.mapNotNull { it.statusInt }
        val hasDefault = endpoint.responses.any { it.statusCode == "default" }
        check(hasDefault || response.statusCode in declared) {
            "Contract ${endpoint.id}: статус ${response.statusCode} не объявлен в спецификации. " +
                "Объявлены: ${declared.sorted()}"
        }
    }

    private fun assertContentType(endpoint: Endpoint, response: ApiResponse) {
        val expected = endpoint.responseFor(response.statusCode)?.contentType ?: return
        if (response.rawBody.isBlank()) return
        val actual = response.contentType ?: return
        check(actual.substringBefore(';').trim().equals(expected, ignoreCase = true)) {
            "Contract ${endpoint.id}: content-type '$actual' не совпадает с объявленным '$expected' " +
                "для статуса ${response.statusCode}"
        }
    }
}
