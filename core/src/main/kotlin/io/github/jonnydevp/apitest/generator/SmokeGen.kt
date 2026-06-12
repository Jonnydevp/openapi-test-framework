package io.github.jonnydevp.apitest.generator

import io.github.jonnydevp.apitest.execution.RequestDataFactory
import io.github.jonnydevp.apitest.spec.model.Endpoint

/** Smoke-генератор: на каждый эндпоинт - happy-path вызов с валидными данными */
object SmokeGen : TestGenerator {

    override fun generate(endpoints: List<Endpoint>): List<GeneratedTest> = endpoints.map { endpoint ->
        GeneratedTest(TestKind.SMOKE, "smoke: ${endpoint.id}", endpoint) { client ->
            val response = client.execute(endpoint, RequestDataFactory.valid(endpoint))
            check(response.statusCode in 200..299) {
                "Smoke ${endpoint.id}: ожидался успешный статус 2xx, получен ${response.statusCode}. " +
                    "Тело: ${response.rawBody.take(300)}"
            }
        }
    }
}
