package io.github.jonnydevp.apitest.generator

import io.github.jonnydevp.apitest.execution.RequestDataFactory
import io.github.jonnydevp.apitest.validation.ContractValidator
import io.github.jonnydevp.apitest.validation.MismatchReport

/** Контрактный генератор: на каждый эндпоинт - валидный запрос и полная сверка ответа со спецификацией через [ContractValidator] (статус, content-type, JSON-схема тела, заголовки) */
object ContractGen : TestGenerator {

    override fun generate(endpoints: List<io.github.jonnydevp.apitest.spec.model.Endpoint>): List<GeneratedTest> =
        endpoints.map { endpoint ->
            GeneratedTest(TestKind.CONTRACT, "contract: ${endpoint.id}", endpoint) { client ->
                val response = client.execute(endpoint, RequestDataFactory.valid(endpoint))
                val report = MismatchReport(ContractValidator.validate(endpoint, response))
                check(!report.hasMismatches) {
                    "Contract ${endpoint.id}: нарушение контракта.\n${report.render()}"
                }
            }
        }
}
