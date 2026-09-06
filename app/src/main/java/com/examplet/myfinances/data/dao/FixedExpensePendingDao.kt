package com.examplet.myfinances.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.examplet.myfinances.data.entity.FixedExpensePendingEntity
import com.examplet.myfinances.domain.model.FixedExpensePendingStatus
import kotlinx.coroutines.flow.Flow

data class FixedExpensePendingRow(
    val id: Long,
    val sourceHouseMonthId: Long,
    val sourceYear: Int,
    val sourceMonth: Int,
    val categoryId: Long,
    val categoryName: String,
    val amountCents: Long,
    val note: String?,
    val status: FixedExpensePendingStatus
)

@Dao
interface FixedExpensePendingDao {
    @Query(
        """
        SELECT
            p.id AS id,
            p.sourceHouseMonthId AS sourceHouseMonthId,
            m.year AS sourceYear,
            m.month AS sourceMonth,
            p.categoryId AS categoryId,
            c.name AS categoryName,
            p.amountCents AS amountCents,
            p.note AS note,
            p.status AS status
        FROM house_fixed_expense_pendings p
        INNER JOIN house_months m ON m.id = p.sourceHouseMonthId
        INNER JOIN house_categories c ON c.id = p.categoryId
        WHERE p.status = 'PENDING'
        ORDER BY m.year ASC, m.month ASC, p.id ASC
        """
    )
    fun observePending(): Flow<List<FixedExpensePendingRow>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: FixedExpensePendingEntity): Long

    @Query(
        """
        UPDATE house_fixed_expense_pendings
        SET status = 'PAID', resolvedAt = :resolvedAt, updatedAt = :resolvedAt
        WHERE id = :id AND status = 'PENDING'
        """
    )
    suspend fun markPaid(id: Long, resolvedAt: Long)
}
