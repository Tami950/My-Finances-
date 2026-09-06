package com.examplet.myfinances.ui.casa

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.examplet.myfinances.R
import com.examplet.myfinances.domain.model.FixedExpensePaymentStatus
import com.examplet.myfinances.domain.model.HouseCategoryBehavior
import com.examplet.myfinances.domain.model.HouseCategoryType
import com.examplet.myfinances.domain.model.HouseMonthStatus
import com.examplet.myfinances.ui.components.AppContentCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun HousePlanningContent(
    state: CasaUiState,
    onConfigure: () -> Unit,
    onCreatePlan: () -> Unit,
    onEditPlan: (Long) -> Unit,
    onEditPositions: (Long) -> Unit,
    onCloseMonth: (Long) -> Unit,
    onSetFixedExpensePaid: (Long, Boolean) -> Unit,
    onMarkPendingPaid: (Long) -> Unit
) {
    if (!state.isHouseSetupCompleted) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.house_setup_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.house_setup_description))
            Spacer(Modifier.height(24.dp))
            Button(onClick = onConfigure) { Text(stringResource(R.string.house_setup_action)) }
        }
        return
    }

    val currentMonth = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN))
        .replaceFirstChar { it.uppercase() }

    if (state.currentPlan == null) {
        val previousOpen = state.previousPlan?.takeIf { it.status == HouseMonthStatus.OPEN }
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                previousOpen != null -> {
                    Text(stringResource(R.string.house_previous_month_must_close_title), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(
                            R.string.house_previous_month_must_close_description,
                            monthLabel(previousOpen.month, previousOpen.year),
                            currentMonth
                        )
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { onCloseMonth(previousOpen.id) }) {
                        Text(stringResource(R.string.house_close_named_month, monthLabel(previousOpen.month, previousOpen.year)))
                    }
                }

                !state.isPlanningReady -> {
                    Text(stringResource(R.string.house_planning_not_ready_title), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.house_planning_not_ready_description))
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onConfigure) { Text(stringResource(R.string.house_setup_action)) }
                }

                else -> {
                    Text(stringResource(R.string.house_planning_empty_title, currentMonth), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.house_planning_empty_description))
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onCreatePlan) { Text(stringResource(R.string.house_plan_month)) }
                }
            }
        }
        return
    }

    val details = state.currentPlanDetails
    if (details == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.house_loading_plan))
        }
        return
    }

    val isOpen = details.status == HouseMonthStatus.OPEN

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "month-header") {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    monthLabel(details.month, details.year),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(if (isOpen) R.string.house_month_status_open else R.string.house_month_status_closed),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item(key = "resources-card") {
            AppContentCard {
                Text(stringResource(R.string.house_plan_summary), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                HousePlanningSummaryRow(stringResource(R.string.house_summary_resources), details.totalResourcesCents)
                if (details.openingAvailableCents > 0) {
                    HousePlanningSummaryRow(stringResource(R.string.house_summary_opening_available), details.openingAvailableCents)
                }
                HousePlanningSummaryRow(stringResource(R.string.house_summary_allocated), details.allocatedCents)
                HousePlanningSummaryRow(stringResource(R.string.house_summary_unallocated), details.availableCents)
                if (details.openingBalanceCents > 0) {
                    HousePlanningSummaryRow(stringResource(R.string.house_summary_opening), details.openingBalanceCents)
                }
                if (details.prefundedFixedExpensesCents > 0) {
                    HousePlanningSummaryRow(
                        stringResource(R.string.house_summary_prefunded_fixed),
                        details.prefundedFixedExpensesCents
                    )
                }
                if (details.pendingFixedExpensesCents > 0) {
                    HousePlanningSummaryRow(
                        stringResource(R.string.house_summary_pending_fixed),
                        details.pendingFixedExpensesCents
                    )
                }
                HousePlanningSummaryRow(stringResource(R.string.house_summary_total_funds), details.totalHouseFundsCents)
                details.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Spacer(Modifier.height(8.dp))
                    Text(note, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (state.pendingFixedExpenses.isNotEmpty()) {
            item(key = "pending-fixed-heading") {
                Text(stringResource(R.string.house_pending_fixed_expenses_title), style = MaterialTheme.typography.titleLarge)
            }
            items(state.pendingFixedExpenses, key = { "pending-fixed-${it.id}" }) { pending ->
                AppContentCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(pending.categoryName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(formatHouseCents(pending.amountCents), fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        stringResource(
                            R.string.house_pending_fixed_expense_inherited,
                            monthLabel(pending.sourceMonth, pending.sourceYear)
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    pending.note?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = { onMarkPendingPaid(pending.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.house_fixed_expense_mark_paid))
                    }
                }
            }
        }

        item(key = "categories-heading") {
            Text(stringResource(R.string.house_plan_categories_title), style = MaterialTheme.typography.titleLarge)
        }

        items(details.allocations, key = { "current-category-${it.categoryId}" }) { allocation ->
            AppContentCard {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(allocation.categoryName, style = MaterialTheme.typography.titleMedium)
                        if (allocation.categoryBehavior == HouseCategoryBehavior.FIXED_EXPENSE) {
                            Text(
                                stringResource(R.string.house_category_fixed_expense_badge),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Text(formatHouseCents(allocation.totalAvailableCents), fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))

                if (allocation.categoryBehavior == HouseCategoryBehavior.FIXED_EXPENSE) {
                    HousePlanningSummaryRow(
                        stringResource(R.string.house_fixed_expense_planned),
                        allocation.fixedExpensePlannedCents ?: allocation.allocatedCents
                    )
                    HousePlanningSummaryRow(
                        stringResource(R.string.house_fixed_expense_prefunded),
                        allocation.fixedExpensePrefundedCents
                    )
                    HousePlanningSummaryRow(
                        stringResource(R.string.house_fixed_expense_from_new_resources),
                        allocation.allocatedCents
                    )
                    val paid = allocation.fixedExpensePaymentStatus == FixedExpensePaymentStatus.PAID
                    Text(
                        stringResource(if (paid) R.string.house_fixed_expense_paid else R.string.house_fixed_expense_unpaid),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (isOpen) {
                        OutlinedButton(
                            onClick = { onSetFixedExpensePaid(allocation.categoryId, !paid) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(
                                    if (paid) R.string.house_fixed_expense_mark_unpaid
                                    else R.string.house_fixed_expense_mark_paid
                                )
                            )
                        }
                    }
                } else {
                    HousePlanningSummaryRow(stringResource(R.string.house_opening_balance), allocation.openingBalanceCents)
                    HousePlanningSummaryRow(stringResource(R.string.house_new_allocation), allocation.allocatedCents)
                    if (allocation.categoryType == HouseCategoryType.TARGET) {
                        allocation.targetCents?.let { target ->
                            HousePlanningSummaryRow(stringResource(R.string.house_category_target_amount), target)
                        }
                    }
                }
            }
        }

        if (isOpen) {
            item(key = "edit-plan") {
                OutlinedButton(onClick = { onEditPlan(details.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.house_edit_plan_action))
                }
            }
        }

        item(key = "positions-divider") { HorizontalDivider() }
        item(key = "positions-heading") {
            Text(stringResource(R.string.house_current_positions_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.house_positions_current_help), style = MaterialTheme.typography.bodySmall)
        }

        if (details.accountBalances.isEmpty()) {
            item(key = "positions-empty") {
                AppContentCard { Text(stringResource(R.string.house_positions_empty_current)) }
            }
        } else {
            items(details.accountBalances, key = { "current-account-${it.moneyAccountId}" }) { balance ->
                AppContentCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(balance.accountName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(formatHouseCents(balance.amountCents), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item(key = "positions-summary") {
            AppContentCard {
                HousePlanningSummaryRow(stringResource(R.string.house_summary_positioned), details.positionedCents)
                HousePlanningSummaryRow(stringResource(R.string.house_summary_unpositioned), details.unpositionedCents)
            }
        }

        if (isOpen) {
            item(key = "edit-positions") {
                OutlinedButton(onClick = { onEditPositions(details.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.house_edit_positions_action))
                }
            }
            item(key = "close-month") {
                Button(onClick = { onCloseMonth(details.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.house_close_month_action))
                }
            }
        }
    }
}

@Composable
private fun HousePlanningSummaryRow(label: String, cents: Long) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text(formatHouseCents(cents))
    }
}
