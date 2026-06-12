package io.github.jonnydevp.apitest.generator.codegen

import com.fasterxml.jackson.databind.JsonNode
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.jonnydevp.apitest.data.SchemaDataGenerator
import io.github.jonnydevp.apitest.spec.model.ApiSpec
import io.github.jonnydevp.apitest.spec.model.Endpoint
import io.github.jonnydevp.apitest.util.jackson
import java.nio.file.Path
import kotlin.io.path.createDirectories

/**
 * Кодогенерация: из [ApiSpec] эмитит человекочитаемые `.kt`-файлы с JUnit 5 + REST Assured
 * smoke-тестами — по одному тест-классу на тег API. В отличие от runtime-режима, здесь
 * получаются обычные исходники, которые можно открыть, прочитать и закоммитить.
 */
object KotlinPoetEmitter {

    private val restAssured = ClassName("io.restassured", "RestAssured")
    private val testAnnotation = ClassName("org.junit.jupiter.api", "Test")

    fun emit(spec: ApiSpec, outputDir: Path, packageName: String = "generated.apitests"): List<Path> {
        outputDir.createDirectories()
        return spec.endpointsByTag().map { (tag, endpoints) ->
            val typeName = "${pascalCase(tag)}SmokeTest"
            val type = TypeSpec.classBuilder(typeName)
                .addKdoc(
                    "Сгенерировано автоматически из OpenAPI-спецификации «%L» (v%L).\nНе редактировать вручную.",
                    spec.title, spec.version,
                )
                .addProperty(baseUrlProperty())
                .apply { endpoints.forEach { addFunction(smokeFunction(it)) } }
                .build()

            FileSpec.builder(packageName, typeName)
                .addType(type)
                .build()
                .writeTo(outputDir)

            outputDir.resolve(packageName.replace('.', '/')).resolve("$typeName.kt")
        }
    }

    private fun baseUrlProperty(): PropertySpec =
        PropertySpec.builder("baseUrl", String::class)
            .addModifiers(KModifier.PRIVATE)
            .initializer("System.getProperty(%S, %S)", "api.baseUrl", "http://localhost:8080")
            .build()

    private fun smokeFunction(endpoint: Endpoint): FunSpec {
        val body = CodeBlock.builder()
            .add("%T.given()\n", restAssured)
            .indent()
            .add(".baseUri(baseUrl)\n")

        endpoint.requestBody?.let { rb ->
            val example = jackson.writeValueAsString(SchemaDataGenerator.generate(rb.schema?.json))
            body.add(".contentType(%S)\n", "application/json")
            body.add(".body(%S)\n", example)
        }

        body.add(".`when`()\n")
            .add(".request(%S, %S)\n", endpoint.method.name, concretePath(endpoint))
            .add(".then()\n")

        endpoint.successResponses().firstOrNull()?.statusInt?.let { code ->
            body.add(".statusCode(%L)\n", code)
        }
        body.unindent()

        return FunSpec.builder(functionName(endpoint))
            .addAnnotation(AnnotationSpec.builder(testAnnotation).build())
            .addCode(body.build())
            .build()
    }

    /** Имя тест-функции без недопустимых для идентификатора символов (`/`, `{`, `}`). */
    private fun functionName(endpoint: Endpoint): String {
        val readablePath = endpoint.path
            .replace(Regex("[/{}]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
        return "smoke ${endpoint.method} $readablePath".trim()
    }

    /** Подставляет конкретные примеры значений вместо `{param}` в пути. */
    private fun concretePath(endpoint: Endpoint): String {
        var path = endpoint.path
        endpoint.pathParameters().forEach { param ->
            val value = param.schema?.let { scalarString(SchemaDataGenerator.generate(it)) } ?: "1"
            path = path.replace("{${param.name}}", value)
        }
        return path.replace(Regex("\\{[^}]+}"), "1")
    }

    private fun scalarString(node: JsonNode): String = if (node.isValueNode) node.asText() else "1"

    private fun pascalCase(raw: String): String =
        raw.split(Regex("[^A-Za-z0-9]+"))
            .filter { it.isNotBlank() }
            .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
            .ifBlank { "Api" }
}
