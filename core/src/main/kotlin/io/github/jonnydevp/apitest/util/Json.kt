package io.github.jonnydevp.apitest.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/** Единый Jackson-маппер фреймворка (сериализация тел запросов, разбор ответов) */
val jackson: ObjectMapper = jacksonObjectMapper()

/** Фабрика JSON-узлов для построения примеров и сгенерированных данных */
val nodes: JsonNodeFactory = JsonNodeFactory.instance
