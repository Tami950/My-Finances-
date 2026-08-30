package com.examplet.myfinances.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.examplet.myfinances.data.entity.HouseMonthEntity
import com.examplet.myfinances.domain.model.HouseMonthStatus
import kotlinx.coroutines.flow.Flow

data class HousePlanSummaryRow(
    val id: Long,
    val year: Int,
    val month: Int,
    val totalResourcesCents: Long,
    val allocatedCents: Long,
    val positionedCents: Long,
    val status: HouseMonthStatus
)

@Dao
interface HouseMonthDao {
    @Query("SELECT * FROM house_months WHERE year = :year AND month = :month LIMIT 1")
    fun observeMonth(year: Int, month: Int): Flow<HouseMonthEntity?>

    @Query("SELECT * FROM house_months WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<HouseMonthEntity?>

    @Query("SELECT * FROM house_months WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HouseMonthEntity?

    @Query("SELECT * FROM house_months WHERE year = :year AND month = :month LIMIT 1")
    suspend fun getByYearMonth(year: Int, month: Int): HouseMonthEntity?

    @Query(
        """
        SELECT
            hm.id AS id,
            hm.year AS year,
            hm.month AS month,
            hm.totalResourcesCents AS totalResourcesCents,
            COALESCE((
                SELECT SUM(a.allocatedCents)
                FROM house_monthly_allocations a
                WHERE a.houseMonthId = hm.id
            ), 0) AS allocatedCents,
            COALESCE((
                SELECT SUM(b.amountCents)
                FROM house_month_account_balances b
                WHERE b.houseMonthId = hm.id
            ), 0) AS positionedCents,
            hm.status AS status
        FROM house_months hm
        WHERE hm.year = :year AND hm.month = :month
        LIMIT 1
        """
    )
    fun observeSummary(year: Int, month: Int): Flow<HousePlanSummaryRow?>

    @Query("SELECT * FROM house_months ORDER BY year DESC, month DESC")
    fun observeAll(): Flow<List<HouseMonthEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(month: HouseMonthEntity): Long

    @Update
    suspend fun update(month: HouseMonthEntity)
}
