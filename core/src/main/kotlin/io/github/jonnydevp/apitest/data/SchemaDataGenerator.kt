package io.github.jonnydevp.apitest.data

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.jonnydevp.apitest.spec.model.SchemaNode
import io.github.jonnydevp.apitest.util.nodes

/** Построение детерминированного **валидного** примера данных по JSON-схеме */
object SchemaDataGenerator {

    fun generate(schema: SchemaNode): JsonNode = generate(schema.json)

    fun generate(schema: JsonNode?): JsonNode {
        if (schema == null || schema.isNull) return nodes.nullNode()

        schema.get("example")?.let { return it.deepCopy() }
        schema.get("default")?.let { return it.deepCopy() }
        schema.get("enum")?.firstOrNull()?.let { return it.deepCopy() }

        // композиция схем
        schema.get("allOf")?.let { return generateObject(mergeAllOf(it)) }
        (schema.get("oneOf") ?: schema.get("anyOf"))?.firstOrNull()?.let { return generate(it) }

        return when (effectiveType(schema)) {
            "object" -> generateObject(schema)
            "array" -> generateArray(schema)
            "string" -> nodes.textNode(stringValue(schema))
            "integer" -> nodes.numberNode(integerValue(schema))
            "number" -> nodes.numberNode(numberValue(schema))
            "boolean" -> nodes.booleanNode(true)
            else -> nodes.nullNode()
        }
    }

    private fun effectiveType(schema: JsonNode): String? {
        schema.get("type")?.let { return it.asText() }
        if (schema.has("properties")) return "object"
        if (schema.has("items")) return "array"
        return null
    }

    private fun generateObject(schema: JsonNode): ObjectNode {
        val obj = nodes.objectNode()
        val properties = schema.get("properties") ?: return obj
        val fields = properties.fields()
        while (fields.hasNext()) {
            val (name, propSchema) = fields.next()
            obj.set<JsonNode>(name, generate(propSchema))
        }
        return obj
    }

    private fun generateArray(schema: JsonNode): ArrayNode {
        val arr = nodes.arrayNode()
        val items = schema.get("items") ?: return arr
        val count = schema.get("minItems")?.asInt()?.coerceAtLeast(1) ?: 1
        repeat(count) { arr.add(generate(items)) }
        return arr
    }

    private fun stringValue(schema: JsonNode): String {
        when (schema.get("format")?.asText()) {
            "date" -> return "2024-01-01"
            "date-time" -> return "2024-01-01T00:00:00Z"
            "email" -> return "user@example.com"
            "uuid" -> return "00000000-0000-0000-0000-000000000000"
            "uri", "url" -> return "https://example.com"
            "byte" -> return "ZXhhbXBsZQ=="
        }
        val minLength = schema.get("minLength")?.asInt() ?: 0
        val base = "string"
        return if (base.length >= minLength) base else base.padEnd(minLength, 'x')
    }

    private fun integerValue(schema: JsonNode): Long {
        val min = schema.get("minimum")?.asLong()
        val max = schema.get("maximum")?.asLong()
        return when {
            min != null -> min
            max != null -> max
            else -> 1L
        }
    }

    private fun numberValue(schema: JsonNode): Double {
        val min = schema.get("minimum")?.asDouble()
        val max = schema.get("maximum")?.asDouble()
        return when {
            min != null -> min
            max != null -> max
            else -> 1.0
        }
    }

    /** Сливает свойства из `allOf` в единую схему-объект */
    private fun mergeAllOf(allOf: JsonNode): ObjectNode {
        val merged = nodes.objectNode()
        val properties = nodes.objectNode()
        allOf.forEach { sub ->
            sub.get("properties")?.fields()?.forEach { (name, value) ->
                properties.set<JsonNode>(name, value)
            }
        }
        merged.set<JsonNode>("type", nodes.textNode("object"))
        merged.set<JsonNode>("properties", properties)
        return merged
    }
}
