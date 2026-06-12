package io.github.jonnydevp.apitest.demo

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.github.jonnydevp.apitest.ApiTestFramework
import io.github.jonnydevp.apitest.execution.ApiClient
import io.qameta.allure.restassured.AllureRestAssured
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Сквозной прогон сгенерированных тестов против корректного WireMock-мока Petstore.
 * Демонстрирует runtime-режим (smoke/contract/fuzz через `@TestFactory`) и codegen-режим.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GeneratedSuiteE2ETest {

    private lateinit var server: WireMockServer
    private lateinit var framework: ApiTestFramework
    private lateinit var baseUrl: String

    @BeforeAll
    fun setUp() {
        server = WireMockServer(options().dynamicPort())
        server.start()
        PetstoreMock.stubValid(server)
        baseUrl = "http://localhost:${server.port()}"
        framework = ApiTestFramework.fromResource("openapi/petstore.yaml")
    }

    @AfterAll
    fun tearDown() {
        server.stop()
    }

    /** Клиент с Allure-фильтром: каждый запрос/ответ попадает в отчёт как attachment. */
    private fun client(): ApiClient = ApiClient(baseUrl, listOf(AllureRestAssured()))

    @TestFactory
    fun smoke(): List<DynamicTest> = framework.runtime(client()).smokeTests()

    @TestFactory
    fun contract(): List<DynamicTest> = framework.runtime(client()).contractTests()

    @TestFactory
    fun fuzz(): List<DynamicTest> = framework.runtime(client()).fuzzTests(iterations = 20)

    @Test
    fun `codegen выпускает kt тесты в build generated-tests`() {
        val files = framework.emit(Path.of("build/generated-tests"))
        assertTrue(files.isNotEmpty(), "ожидался хотя бы один сгенерированный файл")
        assertTrue(
            files.first().readText().contains("class PetsSmokeTest"),
            "сгенерированный файл должен содержать тест-класс",
        )
    }
}
