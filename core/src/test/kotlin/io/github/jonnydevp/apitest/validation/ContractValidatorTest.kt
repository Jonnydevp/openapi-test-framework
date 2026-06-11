package io.github.jonnydevp.apitest.validation

import io.github.jonnydevp.apitest.execution.ApiResponse
import io.github.jonnydevp.apitest.spec.SpecLoader
import io.github.jonnydevp.apitest.spec.SpecMapper
import io.github.jonnydevp.apitest.util.jackson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class ContractValidatorTest : StringSpec({

    val spec = SpecMapper.toApiSpec(SpecLoader.fromResource("openapi/petstore-mini.yaml"))
    val getPet = spec.endpoint("GET /pets/{petId}")

    fun response(status: Int, json: String?, contentType: String? = "application/json"): ApiResponse {
        val body = json?.let { jackson.readTree(it) }
        return ApiResponse(status, contentType, body, json.orEmpty(), emptyMap())
    }

    "корректный ответ по схеме не даёт расхождений" {
        val ok = response(200, """{"id":1,"name":"Rex","status":"available"}""")
        ContractValidator.validate(getPet, ok) shouldBe emptyList()
    }

    "тело без обязательного поля даёт BODY_SCHEMA" {
        val bad = response(200, """{"id":1}""") // нет обязательного name
        val mismatches = ContractValidator.validate(getPet, bad)
        mismatches.map { it.type } shouldContain MismatchType.BODY_SCHEMA
    }

    "неверный тип поля даёт BODY_SCHEMA" {
        val bad = response(200, """{"id":"not-a-number","name":"Rex"}""")
        val mismatches = ContractValidator.validate(getPet, bad)
        mismatches.map { it.type } shouldContain MismatchType.BODY_SCHEMA
    }

    "необъявленный статус даёт UNDECLARED_STATUS" {
        val teapot = response(418, """{"x":1}""")
        val mismatches = ContractValidator.validate(getPet, teapot)
        mismatches.single().type shouldBe MismatchType.UNDECLARED_STATUS
    }

    "несовпадение content-type фиксируется" {
        val wrong = response(200, """{"id":1,"name":"Rex"}""", contentType = "text/plain")
        val mismatches = ContractValidator.validate(getPet, wrong)
        mismatches.map { it.type } shouldContain MismatchType.CONTENT_TYPE
    }

    "отчёт корректно рендерится и агрегирует" {
        val bad = response(200, """{"id":"x"}""")
        val report = MismatchReport(ContractValidator.validate(getPet, bad))
        report.hasMismatches.shouldBeTrue()
        report.byEndpoint().keys shouldContain "GET /pets/{petId}"
    }
})
