package io.github.jonnydevp.apitest.generator

import io.github.jonnydevp.apitest.execution.ApiClient
import io.github.jonnydevp.apitest.spec.model.Endpoint

/** Вид сгенерированного теста */
enum class TestKind { SMOKE, CONTRACT, FUZZ }

/** Сгенерированный исполняемый тест-кейс (runtime-режим) */
class GeneratedTest(
    val kind: TestKind,
    val name: String,
    val endpoint: Endpoint,
    val execute: (ApiClient) -> Unit,
)

/** Общий интерфейс генератора тест-кейсов одного вида */
fun interface TestGenerator {
    fun generate(endpoints: List<Endpoint>): List<GeneratedTest>
}
