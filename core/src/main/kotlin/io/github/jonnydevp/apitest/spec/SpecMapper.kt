package io.github.jonnydevp.apitest.spec

import io.github.jonnydevp.apitest.spec.model.ApiParameter
import io.github.jonnydevp.apitest.spec.model.ApiSpec
import io.github.jonnydevp.apitest.spec.model.Endpoint
import io.github.jonnydevp.apitest.spec.model.HttpMethod
import io.github.jonnydevp.apitest.spec.model.ParamLocation
import io.github.jonnydevp.apitest.spec.model.RequestBodySpec
import io.github.jonnydevp.apitest.spec.model.ResponseSpec
import io.github.jonnydevp.apitest.spec.model.SchemaNode
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter

/** Преобразование модели swagger-core в компактную внутреннюю модель [ApiSpec] */
object SpecMapper {

    fun toApiSpec(openApi: OpenAPI): ApiSpec {
        val endpoints = buildList {
            for ((path, item) in openApi.paths ?: emptyMap()) {
                for ((method, operation) in operationsOf(item)) {
                    add(toEndpoint(path, method, operation))
                }
            }
        }
        val info = openApi.info
        return ApiSpec(
            title = info?.title ?: "API",
            version = info?.version ?: "0.0.0",
            endpoints = endpoints,
        )
    }

    private fun operationsOf(item: PathItem): List<Pair<HttpMethod, Operation>> = buildList {
        item.get?.let { add(HttpMethod.GET to it) }
        item.post?.let { add(HttpMethod.POST to it) }
        item.put?.let { add(HttpMethod.PUT to it) }
        item.patch?.let { add(HttpMethod.PATCH to it) }
        item.delete?.let { add(HttpMethod.DELETE to it) }
        item.head?.let { add(HttpMethod.HEAD to it) }
        item.options?.let { add(HttpMethod.OPTIONS to it) }
    }

    private fun toEndpoint(path: String, method: HttpMethod, op: Operation): Endpoint {
        val parameters = (op.parameters ?: emptyList()).mapNotNull(::toParameter)

        val requestBody = op.requestBody?.let { rb ->
            val (contentType, schema) = firstJsonContent(rb.content)
            RequestBodySpec(required = rb.required ?: false, contentType = contentType, schema = schema)
        }

        val responses = (op.responses ?: emptyMap()).map { (code, resp) ->
            val (contentType, schema) = firstJsonContent(resp.content)
            ResponseSpec(
                statusCode = code,
                contentType = if (resp.content.isNullOrEmpty()) null else contentType,
                schema = schema,
                headers = resp.headers?.keys?.toSet() ?: emptySet(),
            )
        }

        return Endpoint(
            operationId = op.operationId ?: fallbackOperationId(method, path),
            method = method,
            path = path,
            summary = op.summary,
            tags = op.tags ?: emptyList(),
            parameters = parameters,
            requestBody = requestBody,
            responses = responses,
        )
    }

    private fun toParameter(p: Parameter): ApiParameter? {
        val location = when (p.`in`) {
            "path" -> ParamLocation.PATH
            "query" -> ParamLocation.QUERY
            "header" -> ParamLocation.HEADER
            "cookie" -> ParamLocation.COOKIE
            else -> return null
        }
        return ApiParameter(
            name = p.name,
            location = location,
            required = p.required ?: (location == ParamLocation.PATH),
            schema = p.schema?.let(::toSchemaNode),
        )
    }

    private fun firstJsonContent(content: Content?): Pair<String, SchemaNode?> {
        if (content.isNullOrEmpty()) return "application/json" to null
        val entry = content.entries.firstOrNull { it.key.contains("json", ignoreCase = true) }
            ?: content.entries.first()
        return entry.key to entry.value?.schema?.let(::toSchemaNode)
    }

    private fun toSchemaNode(schema: Schema<*>): SchemaNode = SchemaNode(Json.mapper().valueToTree(schema))

    private fun fallbackOperationId(method: HttpMethod, path: String): String {
        val slug = path.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_')
        return "${method.name.lowercase()}_$slug"
    }
}
