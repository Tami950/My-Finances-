package com.examplet.myfinances.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.examplet.myfinances.data.entity.HouseMonthlyAllocationEntity
import com.examplet.myfinances.domain.model.FixedExpensePaymentStatus
import com.examplet.myfinances.domain.model.HouseCategoryBehavior
import com.examplet.myfinances.domain.model.HouseCategoryType
import kotlinx.coroutines.flow.Flow

data class HouseAllocationDetailsRow(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryType: HouseCategoryType,
    val targetCents: Long?,
    val categoryBehavior: HouseCategoryBehavior,
    val fixedExpensePaymentStatus: FixedExpensePaymentStatus?,
    val fixedExpensePlannedCents: Long?,
    val fixedExpensePrefundedCents: Long,
    val openingBalanceCents: Long,
    val allocatedCents: Long
)

@Dao
interface HouseMonthlyAllocationDao {
    @Query("SELECT * FROM house_monthly_allocations WHERE houseMonthId = :houseMonthId ORDER BY id ASC")
    fun observeForMonth(houseMonthId: Long): Flow<List<HouseMonthlyAllocationEntity>>

    @Query("SELECT * FROM house_monthly_allocations WHERE houseMonthId = :houseMonthId ORDER BY id ASC")
    suspend fun getForMonth(houseMonthId: Long): List<HouseMonthlyAllocationEntity>

    @Query(
        """
        SELECT
            a.id AS id,
            a.categoryId AS categoryId,
            c.name AS categoryName,
            c.type AS categoryType,
            c.targetCents AS targetCents,
            a.categoryBehavior AS categoryBehavior,
            a.fixedExpensePaymentStatus AS fixedExpensePaymentStatus,
            a.fixedExpensePlannedCents AS fixedExpensePlannedCents,
            a.fixedExpensePrefundedCents AS fixedExpensePrefundedCents,
            a.openingBalanceCents AS openingBalanceCents,
            a.allocatedCents AS allocatedCents
        FROM house_monthly_allocations a
        INNER JOIN house_categories c ON c.id = a.categoryId
        WHERE a.houseMonthId = :houseMonthId
        ORDER BY c.sortOrder ASC, c.name COLLATE NOCASE ASC
        """
    )
    fun observeDetailsForMonth(houseMonthId: Long): Flow<List<HouseAllocationDetailsRow>>

    @Query("SELECT * FROM house_monthly_allocations WHERE houseMonthId = :houseMonthId AND categoryId = :categoryId LIMIT 1")
    suspend fun getByMonthAndCategory(houseMonthId: Long, categoryId: Long): HouseMonthlyAllocationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(allocation: HouseMonthlyAllocationEntity): Long

    @Update
    suspend fun update(allocation: HouseMonthlyAllocationEntity)

    @Query(
        """
        UPDATE house_monthly_allocations
        SET categoryBehavior = :behavior,
            fixedExpensePaymentStatus = :paymentStatus,
            fixedExpensePlannedCents = CASE
                WHEN :behavior = 'FIXED_EXPENSE' THEN COALESCE(:fixedExpensePlannedCents, openingBalanceCents + allocatedCents)
                ELSE NULL
            END,
            fixedExpensePrefundedCents = CASE WHEN :behavior = 'FIXED_EXPENSE' THEN fixedExpensePrefundedCents ELSE 0 END,
            openingBalanceCents = CASE WHEN :behavior = 'FIXED_EXPENSE' THEN 0 ELSE openingBalanceCents END,
            updatedAt = :updatedAt
        WHERE categoryId = :categoryId
          AND houseMonthId IN (SELECT id FROM house_months WHERE status = 'OPEN')
        """
    )
    suspend fun updateBehaviorForOpenMonths(
        categoryId: Long,
        behavior: HouseCategoryBehavior,
        paymentStatus: FixedExpensePaymentStatus?,
        fixedExpensePlannedCents: Long?,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE house_monthly_allocations
        SET fixedExpensePlannedCents = :plannedCents,
            allocatedCents = CASE
                WHEN :plannedCents > fixedExpensePrefundedCents THEN :plannedCents - fixedExpensePrefundedCents
                ELSE 0
            END,
            updatedAt = :updatedAt
        WHERE categoryId = :categoryId
          AND categoryBehavior = 'FIXED_EXPENSE'
          AND houseMonthId IN (SELECT id FROM house_months WHERE status = 'OPEN')
        """
    )
    suspend fun updateFixedExpensePlanForOpenMonths(
        categoryId: Long,
        plannedCents: Long,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE house_monthly_allocations
        SET fixedExpensePaymentStatus = :status, updatedAt = :updatedAt
        WHERE houseMonthId = :houseMonthId AND categoryId = :categoryId
        """
    )
    suspend fun updateFixedExpensePaymentStatus(
        houseMonthId: Long,
        categoryId: Long,
        status: FixedExpensePaymentStatus,
        updatedAt: Long
    )
}
