package io.github.jonnydevp.apitest.validation

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import io.github.jonnydevp.apitest.spec.model.SchemaNode

/** Валидация JSON-значения против JSON-схемы (через networknt json-schema-validator) */
object SchemaValidator {

    private val factory: JsonSchemaFactory =
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)

    /** Возвращает список сообщений об ошибках валидации; пустой список - значение валидно */
    fun validate(schema: SchemaNode, value: JsonNode): List<String> {
        val jsonSchema = factory.getSchema(schema.json)
        return jsonSchema.validate(value).map { it.message }
    }

    fun isValid(schema: SchemaNode, value: JsonNode): Boolean = validate(schema, value).isEmpty()
}
