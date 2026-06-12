package io.github.jonnydevp.apitest.data

import io.github.jonnydevp.apitest.spec.SpecLoader
import io.github.jonnydevp.apitest.spec.SpecMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll

class SchemaArbTest : StringSpec({

    val spec = SpecMapper.toApiSpec(SpecLoader.fromResource("openapi/petstore-mini.yaml"))
    val petSchema = spec.endpoint("GET /pets/{petId}").responseFor(200)!!.schema!!

    "сгенерированные объекты структурно валидны на множестве итераций" {
        val arb = SchemaArb.fromSchema(petSchema)
        checkAll(100, arb) { node ->
            node.isObject shouldBe true
            node.get("id").isNumber shouldBe true
            node.get("name").isTextual shouldBe true
            node.get("status").asText() shouldBeIn listOf("available", "pending", "sold")
        }
    }

    "генератор целых уважает границы minimum/maximum" {
        val limitSchema = spec.endpoint("GET /pets").queryParameters().single().schema!!
        val arb = SchemaArb.fromSchema(limitSchema)
        checkAll(100, arb) { node ->
            val v = node.asLong()
            (v in 1..100) shouldBe true
        }
    }
})
