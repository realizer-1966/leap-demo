package ai.liquid.leapchat.tools

import android.content.Context
import org.json.JSONObject

/**
 * 도구 설정 저장소.
 * 내장 도구 활성화 여부와 HTTP 도구 정의를 SharedPreferences에 저장.
 */
class ToolStore(context: Context) {
    private val prefs = context.getSharedPreferences("tool_settings", Context.MODE_PRIVATE)

    /** 내장 도구 활성화 여부 */
    fun isBuiltinEnabled(name: String): Boolean =
        prefs.getBoolean("builtin_$name", name == "compute_sum") // compute_sum은 기본 활성화

    fun setBuiltinEnabled(name: String, enabled: Boolean) {
        prefs.edit().putBoolean("builtin_$name", enabled).apply()
    }

    /** 저장된 HTTP 도구 이름 목록 */
    fun httpToolNames(): List<String> =
        prefs.getString("http_tools_json", "{}")
            ?.let { raw -> runCatching { JSONObject(raw).keys().asSequence().toList() }.getOrDefault(emptyList()) }
            ?: emptyList()

    /** HTTP 도구 정의 조회 */
    fun getHttpTool(name: String): HttpTool? {
        val raw = prefs.getString("http_tools_json", "{}") ?: return null
        return try {
            val obj = JSONObject(raw)
            if (!obj.has(name)) return null
            val j = obj.getJSONObject(name)
            HttpTool(
                name = j.getString("name"),
                description = j.getString("description"),
                urlTemplate = j.getString("urlTemplate"),
                headersJson = j.optString("headersJson", ""),
                parameters = parseParams(j.optString("params", ""))
            )
        } catch (_: Exception) { null }
    }

    /** HTTP 도구 저장 */
    fun saveHttpTool(tool: HttpTool) {
        val raw = prefs.getString("http_tools_json", "{}") ?: "{}"
        val obj = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
        val j = JSONObject()
        j.put("name", tool.name)
        j.put("description", tool.description)
        j.put("urlTemplate", tool.urlTemplate)
        j.put("headersJson", tool.headersJson)
        j.put("params", encodeParams(tool.parameters))
        obj.put(tool.name, j)
        prefs.edit().putString("http_tools_json", obj.toString()).apply()
    }

    /** HTTP 도구 삭제 */
    fun removeHttpTool(name: String) {
        val raw = prefs.getString("http_tools_json", "{}") ?: "{}"
        val obj = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
        obj.remove(name)
        prefs.edit().putString("http_tools_json", obj.toString()).apply()
    }

    private fun parseParams(encoded: String): List<ToolParameter> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(";").mapNotNull { seg ->
            val parts = seg.split(":")
            if (parts.size < 2) return@mapNotNull null
            val type = when (parts[1].lowercase()) {
                "int" -> ToolParamType.INT
                "num", "number" -> ToolParamType.NUM
                "bool" -> ToolParamType.BOOL
                "arr", "arr_string" -> ToolParamType.ARR_STRING
                else -> ToolParamType.STRING
            }
            ToolParameter(parts[0], type, if (parts.size > 2) parts[2] else "", true)
        }
    }

    private fun encodeParams(params: List<ToolParameter>): String =
        params.joinToString(";") { p ->
            val typeStr = when (p.type) {
                ToolParamType.STRING -> "string"
                ToolParamType.INT -> "int"
                ToolParamType.NUM -> "num"
                ToolParamType.BOOL -> "bool"
                ToolParamType.ARR_STRING -> "arr"
            }
            "${p.name}:$typeStr:${p.description}"
        }
}
