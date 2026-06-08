package io.github.jonnydevp.apitest.data

import io.github.jonnydevp.apitest.spec.SpecLoader
import io.github.jonnydevp.apitest.spec.SpecMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe

class SchemaDataGeneratorTest : StringSpec({

    val spec = SpecMapper.toApiSpec(SpecLoader.fromResource("openapi/petstore-mini.yaml"))
    val petSchema = spec.endpoint("GET /pets/{petId}").responseFor(200)!!.schema!!
    val newPetSchema = spec.endpoint("POST /pets").requestBody!!.schema!!

    "пример объекта содержит обязательные поля корректных типов" {
        val pet = SchemaDataGenerator.generate(petSchema)
        pet.isObject shouldBe true
        pet.get("id").isIntegralNumber shouldBe true
        pet.get("name").isTextual shouldBe true
    }

    "значение enum берётся из допустимого набора" {
        val pet = SchemaDataGenerator.generate(petSchema)
        pet.get("status").asText() shouldBeIn listOf("available", "pending", "sold")
    }

    "строка уважает minLength" {
        val newPet = SchemaDataGenerator.generate(newPetSchema)
        (newPet.get("name").asText().length >= 1) shouldBe true
    }

    "целое уважает minimum" {
        val limitSchema = spec.endpoint("GET /pets").queryParameters().single().schema!!
        SchemaDataGenerator.generate(limitSchema).asLong() shouldBe 1L
    }
})
