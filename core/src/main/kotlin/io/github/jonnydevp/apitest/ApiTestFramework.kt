package io.github.jonnydevp.apitest

import io.github.jonnydevp.apitest.execution.ApiClient
import io.github.jonnydevp.apitest.generator.codegen.KotlinPoetEmitter
import io.github.jonnydevp.apitest.generator.runtime.DynamicTestFactory
import io.github.jonnydevp.apitest.spec.SpecLoader
import io.github.jonnydevp.apitest.spec.SpecMapper
import io.github.jonnydevp.apitest.spec.model.ApiSpec
import java.nio.file.Path

/** Единая точка входа во фреймворк */
class ApiTestFramework private constructor(val spec: ApiSpec) {

    fun runtime(baseUrl: String): DynamicTestFactory = DynamicTestFactory(spec, ApiClient(baseUrl))

    fun runtime(client: ApiClient): DynamicTestFactory = DynamicTestFactory(spec, client)

    fun emit(outputDir: Path, packageName: String = "generated.apitests"): List<Path> =
        KotlinPoetEmitter.emit(spec, outputDir, packageName)

    companion object {
        fun fromResource(resourceName: String): ApiTestFramework =
            ApiTestFramework(SpecMapper.toApiSpec(SpecLoader.fromResource(resourceName)))

        fun fromPath(path: Path): ApiTestFramework =
            ApiTestFramework(SpecMapper.toApiSpec(SpecLoader.fromPath(path)))

        fun fromContent(content: String): ApiTestFramework =
            ApiTestFramework(SpecMapper.toApiSpec(SpecLoader.fromContent(content)))
    }
}
