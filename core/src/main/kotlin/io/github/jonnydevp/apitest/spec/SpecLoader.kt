package io.github.jonnydevp.apitest.spec

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Загрузка и разбор OpenAPI/Swagger-спецификации (JSON или YAML) через swagger-parser.
 *
 * Используется `resolveFully`, чтобы все ссылки `$ref` были подставлены — это упрощает
 * последующую генерацию данных и валидацию: каждая схема самодостаточна.
 */
object SpecLoader {

    private fun parseOptions(): ParseOptions = ParseOptions().apply {
        isResolve = true
        isResolveFully = true
    }

    fun fromContent(content: String): OpenAPI {
        val result = OpenAPIV3Parser().readContents(content, null, parseOptions())
        return result.openAPI
            ?: error("Не удалось разобрать OpenAPI-спецификацию: ${result.messages.joinToString("; ")}")
    }

    fun fromPath(path: Path): OpenAPI = fromContent(path.readText())

    fun fromResource(resourceName: String): OpenAPI {
        val stream = SpecLoader::class.java.classLoader.getResourceAsStream(resourceName)
            ?: error("Ресурс спецификации не найден на classpath: $resourceName")
        return stream.bufferedReader().use { fromContent(it.readText()) }
    }
}
