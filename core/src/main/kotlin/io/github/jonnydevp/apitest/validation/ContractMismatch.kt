package io.github.jonnydevp.apitest.validation

/** Категория расхождения между спецификацией и фактическим поведением API. */
enum class MismatchType {
    /** Сервис вернул статус-код, не объявленный в спецификации. */
    UNDECLARED_STATUS,

    /** Content-Type ответа не совпадает с объявленным. */
    CONTENT_TYPE,

    /** Тело ответа не соответствует JSON-схеме из спецификации. */
    BODY_SCHEMA,

    /** В ответе отсутствует объявленный в спецификации заголовок. */
    MISSING_HEADER,
}

/** Одно конкретное расхождение док↔поведение, найденное для эндпоинта. */
data class ContractMismatch(
    val type: MismatchType,
    val endpointId: String,
    val statusCode: Int,
    val detail: String,
) {
    override fun toString(): String = "[$type] $endpointId (HTTP $statusCode): $detail"
}
