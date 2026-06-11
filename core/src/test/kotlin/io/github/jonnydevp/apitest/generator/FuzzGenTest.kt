package io.github.jonnydevp.apitest.generator

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.jonnydevp.apitest.execution.ApiClient
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import java.net.InetSocketAddress

class FuzzGenTest : StringSpec({

    val spec = io.github.jonnydevp.apitest.spec.SpecMapper.toApiSpec(
        io.github.jonnydevp.apitest.spec.SpecLoader.fromResource("openapi/petstore-mini.yaml"),
    )

    fun startServer(handler: (HttpExchange) -> Unit): HttpServer =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/pets") { exchange ->
                handler(exchange)
                exchange.close()
            }
            start()
        }

    fun HttpExchange.reply(status: Int, body: String) {
        val bytes = body.toByteArray()
        responseHeaders.add("Content-Type", "application/json")
        sendResponseHeaders(status, bytes.size.toLong())
        responseBody.write(bytes)
    }

    "генерирует по одному fuzz-тесту на эндпоинт" {
        FuzzGen.generate(spec.endpoints) shouldHaveSize spec.endpoints.size
    }

    "устойчивый сервис проходит все fuzz-итерации" {
        val server = startServer { exchange ->
            val ok = """{"id":1,"name":"Rex","status":"available"}"""
            if (exchange.requestMethod == "POST") exchange.reply(201, ok) else exchange.reply(200, ok)
        }
        try {
            val client = ApiClient("http://127.0.0.1:${server.address.port}")
            FuzzGen.generate(spec.endpoints, iterations = 20).forEach { test ->
                shouldNotThrowAny { test.execute(client) }
            }
        } finally {
            server.stop(0)
        }
    }

    "fuzz обнаруживает 5xx на валидном входе" {
        val server = startServer { exchange -> exchange.reply(500, """{"error":"boom"}""") }
        try {
            val client = ApiClient("http://127.0.0.1:${server.address.port}")
            val fuzz = FuzzGen.generate(spec.endpoints, iterations = 5).first { it.endpoint.id == "POST /pets" }
            shouldThrowAny { fuzz.execute(client) }
        } finally {
            server.stop(0)
        }
    }
})
