package com.examplet.myfinances.domain.model

data class HousePlanSummary(
    val id: Long,
    val year: Int,
    val month: Int,
    val totalResourcesCents: Long,
    val allocatedCents: Long,
    val positionedCents: Long,
    val status: HouseMonthStatus
) {
    val unallocatedCents: Long
        get() = totalResourcesCents - allocatedCents

    val unpositionedCents: Long
        get() = totalResourcesCents - positionedCents
}

data class HousePlanDetails(
    val id: Long,
    val year: Int,
    val month: Int,
    val totalResourcesCents: Long,
    val note: String?,
    val status: HouseMonthStatus,
    val closedAt: Long?,
    val allocations: List<HousePlanAllocation>,
    val accountBalances: List<HousePlanAccountBalance>
) {
    val allocatedCents: Long
        get() = allocations.sumOf { it.allocatedCents }

    val openingBalanceCents: Long
        get() = allocations.sumOf { it.openingBalanceCents }

    val positionedCents: Long
        get() = accountBalances.sumOf { it.amountCents }

    val unallocatedCents: Long
        get() = totalResourcesCents - allocatedCents

    val unpositionedCents: Long
        get() = totalResourcesCents - positionedCents
}

data class HousePlanAllocation(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryType: HouseCategoryType,
    val targetCents: Long?,
    val openingBalanceCents: Long,
    val allocatedCents: Long
) {
    val totalAvailableCents: Long
        get() = openingBalanceCents + allocatedCents
}

data class HousePlanAccountBalance(
    val id: Long?,
    val moneyAccountId: Long,
    val accountName: String,
    val accountType: MoneyAccountType,
    val amountCents: Long
)

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
