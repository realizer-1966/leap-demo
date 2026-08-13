package ai.liquid.leapchat.tools

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 내장(오프라인) 도구 모음.
 * 인터넷 없이 동작하는 도구들.
 */
object BuiltinTools {

    /** 사용 가능한 내장 도구 목록 */
    val all: List<Tool> = listOf(
        ComputeSumTool,
        CurrentTimeTool,
        TodayDateTool,
        UnitConvertTool,
        RandomNumberTool,
        CountWordsTool,
    )

    /** 이름으로 조회 */
    fun byName(name: String): Tool? = all.find { it.name == name }
}

/** 숫자 합계 */
object ComputeSumTool : Tool() {
    override val name = "compute_sum"
    override val description = "Compute the sum of a series of numbers."
    override val parameters = listOf(
        ToolParameter("values", ToolParamType.ARR_STRING, "Numbers to compute sum, represented as strings.", true)
    )
    override suspend fun execute(args: Map<String, Any?>): String {
        val values = (args["values"] as? List<*>) ?: emptyList<Any?>()
        val sum = values.fold(0.0) { acc, v -> acc + (v?.toString()?.toDoubleOrNull() ?: 0.0) }
        return "Sum = $sum"
    }
}

/** 현재 시간 */
object CurrentTimeTool : Tool() {
    override val name = "current_time"
    override val description = "Get the current time in the local timezone."
    override val parameters = emptyList<ToolParameter>()
    override suspend fun execute(args: Map<String, Any?>): String {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return "Current local time: $now"
    }
}

/** 오늘 날짜 */
object TodayDateTool : Tool() {
    override val name = "today_date"
    override val description = "Get today's date."
    override val parameters = emptyList<ToolParameter>()
    override suspend fun execute(args: Map<String, Any?>): String {
        return "Today's date: ${LocalDate.now()}"
    }
}

/** 단위 변환 (길이/온도/무게) */
object UnitConvertTool : Tool() {
    override val name = "unit_convert"
    override val description = "Convert between units. Supported: length(m,km,cm,mm,mile), temperature(c,f), weight(kg,g,lb)."
    override val parameters = listOf(
        ToolParameter("value", ToolParamType.NUM, "Numeric value to convert.", true),
        ToolParameter("from", ToolParamType.STRING, "Source unit (m, km, cm, mm, mile, c, f, kg, g, lb).", true),
        ToolParameter("to", ToolParamType.STRING, "Target unit.", true),
    )
    override suspend fun execute(args: Map<String, Any?>): String {
        val value = (args["value"] as? Number)?.toDouble() ?: (args["value"]?.toString()?.toDoubleOrNull())
            ?: return "Error: invalid value"
        val from = (args["from"] as? String)?.lowercase() ?: return "Error: missing from unit"
        val to = (args["to"] as? String)?.lowercase() ?: return "Error: missing to unit"

        // 모든 값을 기준 단위(m, C, kg)로 변환
        val toBase = mapOf(
            "m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001, "mile" to 1609.344,
            "kg" to 1.0, "g" to 0.001, "lb" to 0.45359237
        )
        if (from in toBase && to in toBase) {
            val base = value * (toBase[from] ?: 1.0)
            return "$value $from = ${base / (toBase[to] ?: 1.0)} $to"
        }
        // 온도
        if (from == "c" && to == "f") return "$value C = ${value * 9 / 5 + 32} F"
        if (from == "f" && to == "c") return "$value F = ${(value - 32) * 5 / 9} C"
        return "Error: unsupported conversion $from -> $to"
    }
}

/** 난수 생성 */
object RandomNumberTool : Tool() {
    override val name = "random_number"
    override val description = "Generate a random integer within a range (inclusive)."
    override val parameters = listOf(
        ToolParameter("min", ToolParamType.INT, "Minimum value (default 0).", false),
        ToolParameter("max", ToolParamType.INT, "Maximum value (default 100).", false),
    )
    override suspend fun execute(args: Map<String, Any?>): String {
        val min = (args["min"] as? Number)?.toInt() ?: 0
        val max = (args["max"] as? Number)?.toInt() ?: 100
        val effectiveMax = if (max <= min) min + 1 else max
        return "Random number: ${Random.nextInt(min, effectiveMax + 1)}"
    }
}

/** 단어 수 세기 */
object CountWordsTool : Tool() {
    override val name = "count_words"
    override val description = "Count the number of words in a given text."
    override val parameters = listOf(
        ToolParameter("text", ToolParamType.STRING, "The text to count words in.", true),
    )
    override suspend fun execute(args: Map<String, Any?>): String {
        val text = (args["text"] as? String)?.trim() ?: return "0 words"
        val words = if (text.isEmpty()) 0 else text.split(Regex("\\s+")).size
        return "Word count: $words"
    }
}
