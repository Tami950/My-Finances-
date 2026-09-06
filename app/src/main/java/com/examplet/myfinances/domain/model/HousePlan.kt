package com.examplet.myfinances.domain.model

data class HousePlanSummary(
    val id: Long,
    val year: Int,
    val month: Int,
    val totalResourcesCents: Long,
    val openingAvailableCents: Long,
    val openingBalanceCents: Long,
    val allocatedCents: Long,
    val positionedCents: Long,
    val status: HouseMonthStatus
) {
    val availableCents: Long
        get() = openingAvailableCents + totalResourcesCents - allocatedCents

    val totalHouseFundsCents: Long
        get() = totalResourcesCents + openingAvailableCents + openingBalanceCents

    val unpositionedCents: Long
        get() = totalHouseFundsCents - positionedCents
}

data class HousePlanDetails(
    val id: Long,
    val year: Int,
    val month: Int,
    val totalResourcesCents: Long,
    val openingAvailableCents: Long,
    val pendingFixedExpensesCents: Long = 0,
    val note: String?,
    val status: HouseMonthStatus,
    val closedAt: Long?,
    val allocations: List<HousePlanAllocation>,
    val accountBalances: List<HousePlanAccountBalance>
) {
    val allocatedCents: Long get() = allocations.sumOf { it.allocatedCents }
    val openingBalanceCents: Long get() = allocations.sumOf { it.openingBalanceCents }
    val prefundedFixedExpensesCents: Long get() = allocations.sumOf { it.fixedExpensePrefundedCents }
    val positionedCents: Long get() = accountBalances.sumOf { it.amountCents }
    val availableCents: Long get() = openingAvailableCents + totalResourcesCents - allocatedCents
    val totalHouseFundsCents: Long
        get() = totalResourcesCents + openingAvailableCents + openingBalanceCents +
            prefundedFixedExpensesCents + pendingFixedExpensesCents
    val unpositionedCents: Long get() = totalHouseFundsCents - positionedCents
}

data class HousePlanAllocation(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryType: HouseCategoryType,
    val targetCents: Long?,
    val categoryBehavior: HouseCategoryBehavior = HouseCategoryBehavior.BUDGET,
    val fixedExpensePaymentStatus: FixedExpensePaymentStatus? = null,
    val fixedExpensePlannedCents: Long? = null,
    val fixedExpensePrefundedCents: Long = 0,
    val openingBalanceCents: Long,
    val allocatedCents: Long
) {
    val totalAvailableCents: Long
        get() = if (categoryBehavior == HouseCategoryBehavior.FIXED_EXPENSE) {
            fixedExpensePlannedCents ?: allocatedCents
        } else openingBalanceCents + allocatedCents
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
    val openingAvailableCents: Long = 0,
    val note: String? = null,
    val allocations: List<HousePlanAllocationDraft>,
    val accountBalances: List<HousePlanAccountBalanceDraft>
)

data class HousePlanAllocationDraft(
    val categoryId: Long,
    val openingBalanceCents: Long,
    val allocatedCents: Long,
    val categoryBehavior: HouseCategoryBehavior = HouseCategoryBehavior.BUDGET,
    val fixedExpensePaymentStatus: FixedExpensePaymentStatus? = null,
    val fixedExpensePlannedCents: Long? = null,
    val fixedExpensePrefundedCents: Long = 0
)

data class HousePlanAccountBalanceDraft(
    val moneyAccountId: Long,
    val amountCents: Long
)
