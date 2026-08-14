package ai.liquid.leapchat.views

import ai.liquid.leapchat.models.LeapModels
import ai.liquid.leapchat.tools.BuiltinTools
import ai.liquid.leapchat.tools.HttpTool
import ai.liquid.leapchat.tools.ToolParameter
import ai.liquid.leapchat.tools.ToolParamType
import ai.liquid.leapchat.tools.ToolStore
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * '연결하기' 패널.
 * 하단시트로 내장 도구 토글 + HTTP API 도구 추가/삭제를 제공.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectPanel(
    store: ToolStore,
    onDismiss: () -> Unit,
) {
    // 상태 갱신용 트리거 (패널 열림/닫힘마다 최신 상태 반영)
    var refreshKey by remember { mutableStateOf(0) }
    val tools = remember(store, refreshKey) {
        // 내장 도구 목록 + 활성화 상태
        BuiltinTools.all.map { t -> t to store.isBuiltinEnabled(t.name) }
    }
    val httpNames = remember(store, refreshKey) { store.httpToolNames() }

    var showAddHttp by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("연결하기", style = MaterialTheme.typography.titleLarge)
            Text(
                "외부 도구를 활성화하거나 HTTP API를 연결하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // --- 모델 선택 섹션 ---
            Text("모델 선택", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            ModelSelector(store = store, refreshKey = refreshKey)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // --- 내장 도구 섹션 ---
            Text("내장 도구 (오프라인)", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            tools.forEach { (tool, enabled) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tool.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            tool.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Checkbox(
                        checked = enabled,
                        onCheckedChange = {
                            store.setBuiltinEnabled(tool.name, it)
                            refreshKey++
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            // --- HTTP 도구 섹션 ---
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("HTTP API 도구", style = MaterialTheme.typography.titleSmall)
                OutlinedButton(onClick = { showAddHttp = true }) {
                    Text("+ 추가")
                }
            }
            Spacer(Modifier.height(4.dp))
            if (httpNames.isEmpty()) {
                Text(
                    "연결된 HTTP 도구 없음. 예: 날씨/뉴스/검색 API",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            httpNames.forEach { name ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.bodyMedium)
                        store.getHttpTool(name)?.let { t ->
                            Text(
                                t.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(onClick = {
                        store.removeHttpTool(name)
                        refreshKey++
                    }) {
                        Text("삭제", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("닫기")
            }
        }
    }

    if (showAddHttp) {
        AddHttpToolSheet(
            onDismiss = { showAddHttp = false },
            onSave = { tool ->
                store.saveHttpTool(tool)
                showAddHttp = false
                refreshKey++
            }
        )
    }
}

/** HTTP 도구 추가 폼 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHttpToolSheet(
    onDismiss: () -> Unit,
    onSave: (HttpTool) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var urlTemplate by remember { mutableStateOf("") }
    var headersJson by remember { mutableStateOf("") }
    var params by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("HTTP API 도구 추가", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("도구 이름 (예: weather)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("설명 (예: Get weather for a city)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = urlTemplate,
                onValueChange = { urlTemplate = it },
                label = { Text("URL 템플릿 ({param} 사용)") },
                placeholder = { Text("https://api.example.com/v1/data?q={query}") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = headersJson,
                onValueChange = { headersJson = it },
                label = { Text("HTTP 헤더 (JSON, 선택)") },
                placeholder = { Text("{\"Authorization\":\"Bearer x\"}") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = params,
                onValueChange = { params = it },
                label = { Text("파라미터 (선택, ; 구분)") },
                placeholder = { Text("query:string:검색어;count:int:개수") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("취소") }
                Button(
                    onClick = {
                        if (name.isNotBlank() && urlTemplate.isNotBlank()) {
                            onSave(
                                HttpTool(
                                    name = name.trim(),
                                    description = description.ifBlank { name.trim() },
                                    urlTemplate = urlTemplate.trim(),
                                    headersJson = headersJson.trim(),
                                    parameters = parseHttpParams(params)
                                )
                            )
                        }
                    },
                    enabled = name.isNotBlank() && urlTemplate.isNotBlank()
                ) {
                    Text("저장")
                }
            }
        }
    }
}

private fun parseHttpParams(encoded: String): List<ToolParameter> {
    if (encoded.isBlank()) return emptyList()
    return encoded.split(";").mapNotNull { seg ->
        val parts = seg.trim().split(":")
        if (parts.isEmpty() || parts[0].isBlank()) return@mapNotNull null
        val type = when (parts.getOrNull(1)?.lowercase()) {
            "int" -> ToolParamType.INT
            "num", "number" -> ToolParamType.NUM
            "bool" -> ToolParamType.BOOL
            "arr", "arr_string" -> ToolParamType.ARR_STRING
            else -> ToolParamType.STRING
        }
        ToolParameter(parts[0].trim(), type, parts.getOrNull(2)?.trim() ?: "", true)
    }
}

/**
 * 모델 선택 드롭다운.
 * 선택한 모델을 ToolStore에 저장. (재시작 시 적용)
 */
@Composable
private fun ModelSelector(store: ToolStore, refreshKey: Int) {
    var expanded by remember { mutableStateOf(false) }
    val currentName = remember(store, refreshKey) { store.getModelName() }
    val currentModel = LeapModels.byName(currentName) ?: LeapModels.default

    Column(modifier = Modifier.fillMaxWidth()) {
        // 현재 선택된 모델 표시 + 드롭다운 트리거
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    currentModel.displayName,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    currentModel.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("▾", style = MaterialTheme.typography.titleMedium)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            LeapModels.options.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(model.displayName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                model.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        store.setModel(model.name, model.quantization)
                        expanded = false
                    }
                )
            }
        }

        Text(
            "모델 변경은 앱 재시작 후 적용됩니다.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
