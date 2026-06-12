package io.github.jonnydevp.apitest.generator.runtime

import io.github.jonnydevp.apitest.execution.ApiClient
import io.github.jonnydevp.apitest.generator.ContractGen
import io.github.jonnydevp.apitest.generator.FuzzGen
import io.github.jonnydevp.apitest.generator.GeneratedTest
import io.github.jonnydevp.apitest.generator.SmokeGen
import io.github.jonnydevp.apitest.spec.model.ApiSpec
import org.junit.jupiter.api.DynamicTest

/** Runtime-режим генерации: превращает сгенерированные тест-кейсы в JUnit 5 `DynamicTest`-ы, которые отдаются из `@TestFactory`-метода */
class DynamicTestFactory(
    private val spec: ApiSpec,
    private val client: ApiClient,
) {
    fun smokeTests(): List<DynamicTest> = SmokeGen.generate(spec.endpoints).toDynamicTests()

    fun contractTests(): List<DynamicTest> = ContractGen.generate(spec.endpoints).toDynamicTests()

    fun fuzzTests(iterations: Int = FuzzGen.DEFAULT_ITERATIONS): List<DynamicTest> =
        FuzzGen.generate(spec.endpoints, iterations).toDynamicTests()

    fun allTests(): List<DynamicTest> = smokeTests() + contractTests() + fuzzTests()

    private fun List<GeneratedTest>.toDynamicTests(): List<DynamicTest> =
        map { test -> DynamicTest.dynamicTest(test.name) { test.execute(client) } }
}
