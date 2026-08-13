package ai.liquid.leapchat.tools

import ai.liquid.leap.Conversation
import ai.liquid.leap.function.LeapFunction
import ai.liquid.leap.function.LeapFunctionParameter
import ai.liquid.leap.function.LeapFunctionParameterType

/**
 * 도구 레지스트리.
 * 활성화된 도구를 Conversation에 LeapFunction으로 등록하고,
 * 모델의 함수 호출을 실제 도구 실행으로 연결한다.
 */
class ToolRegistry(private val store: ToolStore) {

    /** 현재 활성화된 모든 도구 (내장 + HTTP) */
    fun activeTools(): List<Tool> {
        val result = mutableListOf<Tool>()
        // 내장 도구
        BuiltinTools.all.forEach { t ->
            if (store.isBuiltinEnabled(t.name)) result.add(t)
        }
        // HTTP 도구
        store.httpToolNames().forEach { name ->
            store.getHttpTool(name)?.let { result.add(it) }
        }
        return result
    }

    /** Conversation에 활성화된 도구를 LeapFunction으로 등록 */
    fun registerAll(conversation: Conversation) {
        activeTools().forEach { t ->
            conversation.registerFunction(toLeapFunction(t))
        }
    }

    /** 도구 호출 실행. 결과 문자열 반환 */
    suspend fun execute(name: String, args: Map<String, Any?>): String {
        val tool = activeTools().find { it.name == name }
            ?: BuiltinTools.byName(name)
            ?: store.getHttpTool(name)
        return tool?.execute(args) ?: "Tool: $name is not available"
    }

    private fun toLeapFunction(tool: Tool): LeapFunction {
        val params = tool.parameters.map { p ->
            LeapFunctionParameter(
                name = p.name,
                type = toLeapType(p.type),
                description = p.description
            )
        }
        return LeapFunction(tool.name, tool.description, params)
    }

    private fun toLeapType(type: ToolParamType): LeapFunctionParameterType {
        return when (type) {
            ToolParamType.STRING -> LeapFunctionParameterType.LeapStr()
            ToolParamType.INT -> LeapFunctionParameterType.LeapInt()
            ToolParamType.NUM -> LeapFunctionParameterType.LeapNum.DoubleNum()
            ToolParamType.BOOL -> LeapFunctionParameterType.LeapBool()
            ToolParamType.ARR_STRING -> LeapFunctionParameterType.LeapArr(
                itemType = LeapFunctionParameterType.LeapStr()
            )
        }
    }
}
