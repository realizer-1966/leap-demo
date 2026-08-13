package ai.liquid.leapchat.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * HTTP API 도구.
 * 사용자가 설정한 외부 API 엔드포인트를 호출해 결과를 반환한다.
 * MCP 서버 대신, HTTP API를 함수로 감싸는 방식 (LEAP SDK에는 MCP 네이티브 미지원).
 *
 * 사용자 설정 형태:
 *   name        : 도구 이름 (예: weather)
 *   description : 도구 설명
 *   urlTemplate : URL 템플릿. {param} 형태로 파라미터를 치환. 예: https://api.weatherapi.com/v1/current.json?key=KEY&q={city}
 *   headers     : HTTP 헤더 (JSON 문자열, 예: {"Authorization":"Bearer x"})
 *   params      : 파라미터 정의 (쉼표 구분 name:type:description)
 */
class HttpTool(
    override val name: String,
    override val description: String,
    val urlTemplate: String,
    val headersJson: String,
    override val parameters: List<ToolParameter>,
    private val timeoutMs: Int = 10000,
) : Tool() {

    override suspend fun execute(args: Map<String, Any?>): String = withContext(Dispatchers.IO) {
        try {
            // URL 템플릿에서 {param} 치환
            var url = urlTemplate
            for ((k, v) in args) {
                url = url.replace("{$k}", URLEncoder.encode(v?.toString() ?: "", "UTF-8"))
            }

            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs

            // 헤더 적용
            if (headersJson.isNotBlank()) {
                try {
                    val orgJson = org.json.JSONObject(headersJson)
                    val keys = orgJson.keys()
                    while (keys.hasNext()) {
                        val key = keys.next() as String
                        conn.setRequestProperty(key, orgJson.getString(key))
                    }
                } catch (_: Exception) {
                    // 헤더 파싱 실패시 무시
                }
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.let {
                BufferedReader(InputStreamReader(it)).use { r -> r.readText() }
            } ?: ""
            conn.disconnect()

            // 응답을 최대 4000자로 제한
            val truncated = if (body.length > 4000) body.take(4000) + "..." else body
            "HTTP $code\n$truncated"
        } catch (e: Exception) {
            "HTTP Error: ${e.message}"
        }
    }
}
