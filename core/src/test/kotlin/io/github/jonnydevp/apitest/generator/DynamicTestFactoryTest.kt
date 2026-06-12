package io.github.jonnydevp.apitest.generator

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.jonnydevp.apitest.execution.ApiClient
import io.github.jonnydevp.apitest.generator.runtime.DynamicTestFactory
import io.github.jonnydevp.apitest.spec.SpecLoader
import io.github.jonnydevp.apitest.spec.SpecMapper
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import java.net.InetSocketAddress

class DynamicTestFactoryTest : StringSpec({

    val spec = SpecMapper.toApiSpec(SpecLoader.fromResource("openapi/petstore-mini.yaml"))

    fun startServer(handler: (HttpExchange) -> Unit): HttpServer =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/pets") { exchange ->
                handler(exchange)
                exchange.close()
            }
            start()
        }

    fun HttpExchange.reply(status: Int, body: String, contentType: String = "application/json") {
        val bytes = body.toByteArray()
        responseHeaders.add("Content-Type", contentType)
        sendResponseHeaders(status, bytes.size.toLong())
        responseBody.write(bytes)
    }

    "фабрика строит smoke + contract тесты по числу эндпоинтов" {
        val factory = DynamicTestFactory(spec, ApiClient("http://127.0.0.1:1"))
        factory.smokeTests() shouldHaveSize spec.endpoints.size
        factory.contractTests() shouldHaveSize spec.endpoints.size
        factory.allTests() shouldHaveSize spec.endpoints.size * 2
    }

    "против корректного мока все smoke и contract тесты проходят" {
        val pet = """{"id":1,"name":"Rex","status":"available"}"""
        val server = startServer { exchange ->
            val isCollection = exchange.requestURI.path.trimEnd('/') == "/pets"
            when {
                exchange.requestMethod == "POST" -> exchange.reply(201, pet)
                isCollection -> exchange.reply(200, "[$pet]")
                else -> exchange.reply(200, pet) // GET /pets/{id}
            }
        }
        try {
            val client = ApiClient("http://127.0.0.1:${server.address.port}")
            val factory = DynamicTestFactory(spec, client)
            factory.allTests().forEach { test ->
                shouldNotThrowAny { test.executable.execute() }
            }
        } finally {
            server.stop(0)
        }
    }

    "smoke падает, когда сервис отвечает 5xx" {
        val server = startServer { exchange -> exchange.reply(500, """{"error":"boom"}""") }
        try {
            val client = ApiClient("http://127.0.0.1:${server.address.port}")
            val smoke = SmokeGen.generate(spec.endpoints).first { it.endpoint.id == "GET /pets" }
            shouldThrowAny { smoke.execute(client) }
        } finally {
            server.stop(0)
        }
    }
})
