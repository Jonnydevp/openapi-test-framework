package io.github.jonnydevp.apitest.generator

import io.github.jonnydevp.apitest.execution.RequestDataFactory
import io.github.jonnydevp.apitest.spec.model.Endpoint
import io.kotest.property.RandomSource

/**
 * Fuzz-генератор (property-based): на каждый эндпоинт прогоняет [iterations] итераций со
 * случайными валидными данными из [io.github.jonnydevp.apitest.data.SchemaArb].
 *
 * Проверяемое свойство: на корректном (по схеме) входе сервис **не отвечает 5xx** —
 * то есть не падает с внутренней ошибкой. Сид [RandomSource] детерминирован по `endpoint.id`,
 * поэтому падения воспроизводимы.
 */
object FuzzGen : TestGenerator {

    const val DEFAULT_ITERATIONS: Int = 30

    override fun generate(endpoints: List<Endpoint>): List<GeneratedTest> = generate(endpoints, DEFAULT_ITERATIONS)

    fun generate(endpoints: List<Endpoint>, iterations: Int): List<GeneratedTest> = endpoints.map { endpoint ->
        GeneratedTest(TestKind.FUZZ, "fuzz: ${endpoint.id}", endpoint) { client ->
            val rs = RandomSource.seeded(endpoint.id.hashCode().toLong())
            repeat(iterations) { iteration ->
                val data = RequestDataFactory.fuzzed(endpoint, rs)
                val response = client.execute(endpoint, data)
                check(response.statusCode < 500) {
                    "Fuzz ${endpoint.id}: сервис вернул ${response.statusCode} на валидном по схеме входе " +
                        "(итерация ${iteration + 1}/$iterations). Тело запроса: ${data.body}"
                }
            }
        }
    }
}
