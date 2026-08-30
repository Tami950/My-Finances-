package com.examplet.myfinances.domain.repository

import com.examplet.myfinances.domain.model.HousePlanDraft
import com.examplet.myfinances.domain.model.HousePlanSummary
import kotlinx.coroutines.flow.Flow

interface HousePlanRepository {
    fun observeSummary(year: Int, month: Int): Flow<HousePlanSummary?>

    suspend fun createPlan(draft: HousePlanDraft): Long
}
