package app.cashflow.core

import kotlinx.serialization.Serializable

@Serializable
enum class PlanTier(val label: String, val watchedApps: Int?, val monthlyAi: Int) {
    FREE("Free", 2, 0), LITE("Lite", 5, 300), PREMIUM("Premium", null, 3000);
}

@Serializable
data class SubscriptionState(
    val tier: PlanTier = PlanTier.FREE,
    val expiresAt: Long = 0,
    val used: Int = 0,
    val month: String = "",
    val available: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null,
    val offers: List<PlanOffer> = emptyList(),
) {
    fun effectiveTier(now: Long): PlanTier = if (expiresAt > now) tier else PlanTier.FREE
}

@Serializable
data class PlanOffer(val id: String, val tier: PlanTier, val price: String, val period: String)

@Serializable
data class AiSuggestion(
    val relevant: Boolean? = null,
    val direction: Direction? = null,
    val kind: Kind? = null,
    val model: String = "",
)
