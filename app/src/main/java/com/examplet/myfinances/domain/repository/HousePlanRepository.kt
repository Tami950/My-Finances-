package com.examplet.myfinances.domain.repository

import com.examplet.myfinances.domain.model.HousePlanAccountBalanceDraft
import com.examplet.myfinances.domain.model.HousePlanDetails
import com.examplet.myfinances.domain.model.HousePlanDraft
import com.examplet.myfinances.domain.model.HousePlanSummary
import kotlinx.coroutines.flow.Flow

interface HousePlanRepository {
    fun observeSummary(year: Int, month: Int): Flow<HousePlanSummary?>

    fun observeDetails(houseMonthId: Long): Flow<HousePlanDetails?>

    suspend fun createPlan(draft: HousePlanDraft): Long

    suspend fun updatePlan(houseMonthId: Long, draft: HousePlanDraft)

    suspend fun updatePositions(
        houseMonthId: Long,
        accountBalances: List<HousePlanAccountBalanceDraft>
    )
}
