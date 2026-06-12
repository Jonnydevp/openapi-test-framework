package io.github.jonnydevp.apitest.data

import com.fasterxml.jackson.databind.JsonNode
import io.github.jonnydevp.apitest.spec.model.SchemaNode
import io.github.jonnydevp.apitest.util.nodes
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string

/** Построение генератора [Arb] случайных **валидных** значений по JSON-схеме - основа fuzz-тестов (property-based) */
object SchemaArb {

    fun fromSchema(schema: SchemaNode): Arb<JsonNode> = build(schema.json)

    private fun build(schema: JsonNode?): Arb<JsonNode> {
        if (schema == null || schema.isNull) return Arb.constant(nodes.nullNode())

        enumArb(schema)?.let { return it }

        schema.get("allOf")?.let { return objectArb(mergedProperties(it)) }
        (schema.get("oneOf") ?: schema.get("anyOf"))?.firstOrNull()?.let { return build(it) }

        return when (effectiveType(schema)) {
            "object" -> objectArb(schema.get("properties"))
            "array" -> arrayArb(schema)
            "string" -> stringArb(schema)
            "integer" -> integerArb(schema)
            "number" -> numberArb(schema)
            "boolean" -> Arb.boolean().map { nodes.booleanNode(it) }
            else -> Arb.constant(nodes.nullNode())
        }
    }

    private fun effectiveType(schema: JsonNode): String? {
        schema.get("type")?.let { return it.asText() }
        if (schema.has("properties")) return "object"
        if (schema.has("items")) return "array"
        return null
    }

    private fun enumArb(schema: JsonNode): Arb<JsonNode>? {
        val values = schema.get("enum")?.toList()?.takeIf { it.isNotEmpty() } ?: return null
        return Arb.element(values).map { it.deepCopy() }
    }

    private fun objectArb(properties: JsonNode?): Arb<JsonNode> {
        val childArbs = LinkedHashMap<String, Arb<JsonNode>>()
        properties?.fields()?.forEach { (name, propSchema) -> childArbs[name] = build(propSchema) }
        return arbitrary { rs ->
            val obj = nodes.objectNode()
            for ((name, arb) in childArbs) obj.set<JsonNode>(name, arb.next(rs))
            obj
        }
    }

    private fun arrayArb(schema: JsonNode): Arb<JsonNode> {
        val itemArb = build(schema.get("items"))
        val min = schema.get("minItems")?.asInt() ?: 0
        val max = schema.get("maxItems")?.asInt() ?: (min + 3)
        val sizeArb = Arb.int(min..maxOf(min, max))
        return arbitrary { rs ->
            val arr = nodes.arrayNode()
            repeat(sizeArb.next(rs)) { arr.add(itemArb.next(rs)) }
            arr
        }
    }

    private fun stringArb(schema: JsonNode): Arb<JsonNode> {
        schema.get("format")?.asText()?.let { format ->
            if (format in KNOWN_FORMATS) return Arb.constant(nodes.textNode(SchemaDataGenerator.generate(schema).asText()))
        }
        val min = schema.get("minLength")?.asInt() ?: 0
        val max = schema.get("maxLength")?.asInt() ?: maxOf(min + 8, 8)
        return Arb.string(min, maxOf(min, max)).map { nodes.textNode(it) }
    }

    private fun integerArb(schema: JsonNode): Arb<JsonNode> {
        val min = schema.get("minimum")?.asLong() ?: -1_000_000L
        val max = schema.get("maximum")?.asLong() ?: 1_000_000L
        return Arb.long(min..maxOf(min, max)).map { nodes.numberNode(it) }
    }

    private fun numberArb(schema: JsonNode): Arb<JsonNode> {
        val min = schema.get("minimum")?.asDouble() ?: -1_000_000.0
        val max = schema.get("maximum")?.asDouble() ?: 1_000_000.0
        return Arb.double(min, maxOf(min, max)).map { nodes.numberNode(it) }
    }

    private fun mergedProperties(allOf: JsonNode): JsonNode {
        val properties = nodes.objectNode()
        allOf.forEach { sub ->
            sub.get("properties")?.fields()?.forEach { (name, value) -> properties.set<JsonNode>(name, value) }
        }
        return properties
    }

    private val KNOWN_FORMATS = setOf("date", "date-time", "email", "uuid", "uri", "url", "byte")
}
