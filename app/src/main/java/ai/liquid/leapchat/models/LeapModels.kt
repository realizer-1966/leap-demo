package ai.liquid.leapchat.models

/**
 * 사용 가능한 LEAP 모델 목록.
 * LEAP SDK 모델 매니페스트 API로 검증된 모델만 포함.
 */
data class LeapModelOption(
    val name: String,          // 모델명 (MODEL_NAME)
    val quantization: String,  // 양자화 (QUANTIZATION_SLUG)
    val displayName: String,   // UI 표시 이름
    val description: String,   // 설명
)

object LeapModels {
    val options: List<LeapModelOption> = listOf(
        LeapModelOption(
            name = "LFM2-350M",
            quantization = "Q8_0",
            displayName = "LFM2-350M (기본)",
            description = "가볍고 빠른 기본 모델. 350MB, 저사양 기기 최적",
        ),
        LeapModelOption(
            name = "LFM2.5-350M",
            quantization = "Q8_0",
            displayName = "LFM2.5-350M",
            description = "최신 350M 모델. 28T 토큰 학습, 더 나은 품질",
        ),
        LeapModelOption(
            name = "LFM2-1.2B",
            quantization = "Q8_0",
            displayName = "LFM2-1.2B",
            description = "중간 크기 모델. 더 나은 추론 품질 (1.2GB)",
        ),
        LeapModelOption(
            name = "LFM2-1.2B-Tool",
            quantization = "Q8_0",
            displayName = "LFM2-1.2B-Tool",
            description = "함수 호출(도구)에 최적화된 모델. 연결 패널 도구와 잘 동작",
        ),
    )

    /** 이름으로 모델 찾기 */
    fun byName(name: String): LeapModelOption? = options.find { it.name == name }

    /** 기본 모델 */
    val default: LeapModelOption = options.first()
}
