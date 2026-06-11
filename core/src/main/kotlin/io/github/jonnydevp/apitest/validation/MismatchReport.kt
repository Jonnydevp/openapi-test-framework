package io.github.jonnydevp.apitest.validation

/** Сводка найденных расхождений по результатам прогона. */
data class MismatchReport(val mismatches: List<ContractMismatch>) {

    val hasMismatches: Boolean get() = mismatches.isNotEmpty()

    fun byType(): Map<MismatchType, List<ContractMismatch>> = mismatches.groupBy { it.type }

    fun byEndpoint(): Map<String, List<ContractMismatch>> = mismatches.groupBy { it.endpointId }

    /** Человекочитаемая сводка для лога/отчёта. */
    fun render(): String {
        if (mismatches.isEmpty()) return "Расхождений док↔поведение не обнаружено."
        return buildString {
            appendLine("Обнаружено расхождений: ${mismatches.size}")
            byEndpoint().forEach { (endpoint, items) ->
                appendLine("• $endpoint")
                items.forEach { appendLine("    - [${it.type}] ${it.detail}") }
            }
        }.trimEnd()
    }
}
