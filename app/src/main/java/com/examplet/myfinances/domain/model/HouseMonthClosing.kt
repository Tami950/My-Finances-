package com.examplet.myfinances.domain.model

data class HouseMonthCarryover(
    val categoryOpeningCents: Map<Long, Long> = emptyMap(),
    val availableCents: Long = 0
)

data class HouseMonthClosingDraft(
    val houseMonthId: Long,
    val confirmedAvailableCents: Long,
    val availableAdjustmentNote: String? = null,
    val availableTransfers: List<HouseAvailableClosingTransferDraft> = emptyList(),
    val categories: List<HouseCategoryClosingDraft>
)

data class HouseAvailableClosingTransferDraft(
    val destinationCategoryId: Long,
    val amountCents: Long
)

data class HouseCategoryClosingDraft(
    val categoryId: Long,
    val confirmedBalanceCents: Long,
    val adjustmentNote: String? = null,
    val transfers: List<HouseClosingTransferDraft>
)

data class HouseClosingTransferDraft(
    val destinationType: HouseClosingDestinationType,
    val destinationCategoryId: Long? = null,
    val amountCents: Long
)
