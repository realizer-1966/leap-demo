package ai.liquid.leapchat.tools

/**
 * 도구 정의 모델.
 * 각 도구는 이름/설명/파라미터 스키마 + 실행 로직으로 구성된다.
 * 내장 도구(BuiltinTools)와 HTTP 도구(HttpTool)가 이 모델을 구현한다.
 */
sealed class Tool {

    /** 도구 고유 이름 (예: "compute_sum") */
    abstract val name: String

    /** 모델에게 보여줄 설명 */
    abstract val description: String

    /** 파라미터 정의 목록 */
    abstract val parameters: List<ToolParameter>

    /** 모델이 도구를 호출했을 때 실행. 결과 문자열을 반환한다. */
    abstract suspend fun execute(args: Map<String, Any?>): String
}

data class ToolParameter(
    val name: String,
    val type: ToolParamType,
    val description: String,
    val required: Boolean = true,
)

enum class ToolParamType { STRING, INT, NUM, BOOL, ARR_STRING }

/** LeapFunction 타입 변환 헬퍼 */
object ToolParamTypeMapper {
    fun leapType(type: ToolParamType): Any {
        return when (type) {
            ToolParamType.STRING -> "string"
            ToolParamType.INT -> "integer"
            ToolParamType.NUM -> "number"
            ToolParamType.BOOL -> "boolean"
            ToolParamType.ARR_STRING -> "array"
        }
    }
}
