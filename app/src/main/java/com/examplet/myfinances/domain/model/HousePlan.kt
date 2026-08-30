package com.examplet.myfinances.domain.model

data class HousePlanSummary(
    val id: Long,
    val year: Int,
    val month: Int,
    val totalResourcesCents: Long,
    val allocatedCents: Long,
    val positionedCents: Long
) {
    val unallocatedCents: Long
        get() = totalResourcesCents - allocatedCents

    val unpositionedCents: Long
        get() = totalResourcesCents - positionedCents
}

data class HousePlanDraft(
    val year: Int,
    val month: Int,
    val totalResourcesCents: Long,
    val note: String? = null,
    val allocations: List<HousePlanAllocationDraft>,
    val accountBalances: List<HousePlanAccountBalanceDraft>
)

data class HousePlanAllocationDraft(
    val categoryId: Long,
    val openingBalanceCents: Long,
    val allocatedCents: Long
)

data class HousePlanAccountBalanceDraft(
    val moneyAccountId: Long,
    val amountCents: Long
)
