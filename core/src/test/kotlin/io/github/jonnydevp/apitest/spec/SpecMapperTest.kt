package io.github.jonnydevp.apitest.spec

import io.github.jonnydevp.apitest.spec.model.ParamLocation
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class SpecMapperTest : StringSpec({

    val spec = SpecMapper.toApiSpec(SpecLoader.fromResource("openapi/petstore-mini.yaml"))

    "разбирает заголовок и версию спецификации" {
        spec.title shouldBe "Petstore Mini"
        spec.version shouldBe "1.0.0"
    }

    "находит все эндпоинты" {
        spec.endpoints shouldHaveSize 3
        spec.endpoints.map { it.id } shouldContainExactlyInAnyOrder
            listOf("GET /pets", "POST /pets", "GET /pets/{petId}")
    }

    "path-параметр помечен обязательным" {
        val getPet = spec.endpoint("GET /pets/{petId}")
        val petId = getPet.pathParameters().single()
        petId.name shouldBe "petId"
        petId.location shouldBe ParamLocation.PATH
        petId.required shouldBe true
    }

    "необязательный query-параметр распознан" {
        val listPets = spec.endpoint("GET /pets")
        val limit = listPets.queryParameters().single()
        limit.name shouldBe "limit"
        limit.required shouldBe false
    }

    "resolveFully подставляет \$ref внутрь схемы ответа" {
        val getPet = spec.endpoint("GET /pets/{petId}")
        val ok = getPet.responseFor(200).shouldNotBeNull()
        val schema = ok.schema.shouldNotBeNull()
        schema.isObject() shouldBe true
        schema.json.get("properties").has("name") shouldBe true
        schema.requiredProperties() shouldContainExactlyInAnyOrder listOf("id", "name")
    }

    "тело запроса считывается с обязательностью и content-type" {
        val createPet = spec.endpoint("POST /pets")
        val body = createPet.requestBody.shouldNotBeNull()
        body.required shouldBe true
        body.contentType shouldBe "application/json"
        body.schema.shouldNotBeNull().requiredProperties() shouldContainExactlyInAnyOrder listOf("name")
    }

    "ответ без тела не имеет content-type и схемы" {
        val createPet = spec.endpoint("POST /pets")
        val badRequest = createPet.responseFor(400).shouldNotBeNull()
        badRequest.contentType shouldBe null
        badRequest.schema shouldBe null
    }
})
