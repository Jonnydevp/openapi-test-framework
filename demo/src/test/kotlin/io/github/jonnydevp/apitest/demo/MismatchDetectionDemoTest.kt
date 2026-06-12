package io.github.jonnydevp.apitest.demo

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.github.jonnydevp.apitest.ApiTestFramework
import io.github.jonnydevp.apitest.execution.ApiClient
import io.github.jonnydevp.apitest.execution.RequestDataFactory
import io.github.jonnydevp.apitest.validation.ContractValidator
import io.github.jonnydevp.apitest.validation.MismatchReport
import io.github.jonnydevp.apitest.validation.MismatchType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Демонстрация ключевой возможности — выявления расхождений между документацией и поведением.
 *
 * Мок намеренно отдаёт по `GET /pets/{petId}` тело без обязательного поля `name`.
 * Тест проверяет, что детектор это расхождение находит (тест зелёный — расхождение *ожидается*).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MismatchDetectionDemoTest {

    private lateinit var server: WireMockServer
    private lateinit var framework: ApiTestFramework
    private lateinit var baseUrl: String

    @BeforeAll
    fun setUp() {
        server = WireMockServer(options().dynamicPort())
        server.start()
        PetstoreMock.stubValid(server)
        PetstoreMock.stubBrokenGetById(server) // переопределяет GET /pets/{id} на «сломанный»
        baseUrl = "http://localhost:${server.port()}"
        framework = ApiTestFramework.fromResource("openapi/petstore.yaml")
    }

    @AfterAll
    fun tearDown() {
        server.stop()
    }

    @Test
    fun `детектор находит расхождение тела ответа со схемой`() {
        val client = ApiClient(baseUrl)
        val getById = framework.spec.endpoint("GET /pets/{petId}")

        val response = client.execute(getById, RequestDataFactory.valid(getById))
        val report = MismatchReport(ContractValidator.validate(getById, response))

        assertTrue(report.hasMismatches, "ожидалось обнаруженное расхождение")
        assertTrue(
            report.mismatches.any { it.type == MismatchType.BODY_SCHEMA },
            "ожидалось расхождение BODY_SCHEMA, получено: ${report.render()}",
        )
    }
}
