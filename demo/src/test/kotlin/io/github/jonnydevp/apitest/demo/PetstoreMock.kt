package io.github.jonnydevp.apitest.demo

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

/**
 * WireMock-стабы для демо-Petstore: корректные ответы по спецификации и один «сломанный»
 * для демонстрации детектора расхождений.
 */
object PetstoreMock {

    private val PET = """{"id":1,"name":"Rex","tag":"dog","status":"available"}"""

    /** Корректные ответы, полностью соответствующие спецификации. */
    fun stubValid(server: WireMockServer) {
        server.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/pets"))
                .willReturn(WireMock.okJson("[$PET]")),
        )
        server.stubFor(
            WireMock.post(WireMock.urlPathEqualTo("/pets"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(PET),
                ),
        )
        server.stubFor(
            WireMock.get(WireMock.urlPathMatching("/pets/[^/]+"))
                .willReturn(WireMock.okJson(PET)),
        )
    }

    /**
     * Намеренное расхождение: `GET /pets/{petId}` возвращает тело **без обязательного поля
     * `name`** — спецификация требует его, а сервис не отдаёт. Детектор обязан это поймать.
     */
    fun stubBrokenGetById(server: WireMockServer) {
        server.stubFor(
            WireMock.get(WireMock.urlPathMatching("/pets/[^/]+"))
                .willReturn(WireMock.okJson("""{"id":1,"tag":"dog"}""")),
        )
    }
}
