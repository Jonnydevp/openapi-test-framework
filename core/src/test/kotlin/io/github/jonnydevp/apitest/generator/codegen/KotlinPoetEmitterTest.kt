package io.github.jonnydevp.apitest.generator.codegen

import io.github.jonnydevp.apitest.ApiTestFramework
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import kotlin.io.path.readText

class KotlinPoetEmitterTest : StringSpec({

    val framework = ApiTestFramework.fromResource("openapi/petstore-mini.yaml")

    "эмитит компилируемый .kt тест-класс по тегу" {
        val dir = Files.createTempDirectory("generated-tests")
        val files = framework.emit(dir, "generated.petstore")

        files shouldHaveSize 1 // все эндпоинты тега pets -> один класс
        val content = files.first().readText()

        content shouldContain "package generated.petstore"
        content shouldContain "class PetsSmokeTest"
        content shouldContain "import io.restassured.RestAssured"
        content shouldContain "@Test"
        content shouldContain "/pets/1"          // подставлен конкретный path-параметр
        content shouldContain "\"POST\""         // smoke для создания
        content shouldContain ".statusCode(200)" // ожидаемый успешный статус
    }
})
