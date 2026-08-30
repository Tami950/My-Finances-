package com.examplet.myfinances.ui.casa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examplet.myfinances.R
import com.examplet.myfinances.domain.model.HouseCategoryType
import com.examplet.myfinances.ui.components.AppModalBottomSheet
import com.examplet.myfinances.ui.components.AppScreen
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CreateHousePlanScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CreateHousePlanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    AppScreen {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.action_back))
                    }
                    Text(
                        text = stringResource(
                            R.string.house_create_plan_title,
                            monthLabel(state.month, state.year)
                        ),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            item(key = "resources") {
                OutlinedTextField(
                    value = state.totalResourcesText,
                    onValueChange = viewModel::updateTotalResources,
                    label = { Text(stringResource(R.string.house_month_resources)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item(key = "note") {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::updateNote,
                    label = { Text(stringResource(R.string.house_month_note)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item(key = "categories-title") {
                Text(
                    stringResource(R.string.house_plan_categories_title),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(state.categories, key = { "plan-category-${it.category.id}" }) { row ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openCategory(row.category.id) }
                        .padding(vertical = 8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(row.category.name, modifier = Modifier.weight(1f))
                        Text(formatCents(rowTotalCents(row)))
                    }
                    val categoryDescription = when (row.category.type) {
                        HouseCategoryType.FLEXIBLE -> stringResource(R.string.house_category_type_flexible)
                        HouseCategoryType.TARGET -> stringResource(
                            R.string.house_category_type_target,
                            formatCents(row.category.targetCents ?: 0)
                        )
                    }
                    Text(categoryDescription, style = MaterialTheme.typography.bodySmall)
                    if (row.openingBalanceText.isNotBlank() || row.allocatedText.isNotBlank()) {
                        Text(
                            stringResource(
                                R.string.house_plan_category_breakdown,
                                formatCents(parseCentsOrZeroLocal(row.openingBalanceText)),
                                formatCents(parseCentsOrZeroLocal(row.allocatedText))
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item(key = "summary-divider") { HorizontalDivider() }
            item(key = "summary") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.house_plan_summary), style = MaterialTheme.typography.titleMedium)
                    SummaryRow(stringResource(R.string.house_summary_resources), state.totalResourcesCents)
                    SummaryRow(stringResource(R.string.house_summary_allocated), state.allocatedCents)
                    SummaryRow(stringResource(R.string.house_summary_unallocated), state.unallocatedCents)
                    if (state.openingBalanceCents > 0) {
                        SummaryRow(stringResource(R.string.house_summary_opening), state.openingBalanceCents)
                    }
                    if (state.hasAllocationOverflow) {
                        Text(
                            stringResource(
                                R.string.house_allocation_overflow_error,
                                formatCents(state.allocationOverflowCents)
                            ),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item(key = "accounts-title") {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.house_plan_accounts_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    stringResource(R.string.house_positions_current_help),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            items(state.accounts, key = { "plan-account-${it.account.id}" }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.openAccount(row.account.id) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(row.account.name, modifier = Modifier.weight(1f))
                    Text(formatCents(parseCentsOrZeroLocal(row.amountText)))
                }
            }

            item(key = "position-summary") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SummaryRow(stringResource(R.string.house_summary_positioned), state.positionedCents)
                    SummaryRow(stringResource(R.string.house_summary_unpositioned), state.unpositionedCents)
                    if (state.hasPositionOverflow) {
                        Text(
                            stringResource(
                                R.string.house_position_overflow_error,
                                formatCents(state.positionOverflowCents)
                            ),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            state.errorMessage?.let { error ->
                item(key = "error") {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }

            item(key = "save") {
                Button(
                    onClick = viewModel::savePlan,
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(
                            if (state.isSaving) R.string.action_saving else R.string.house_save_plan
                        )
                    )
                }
            }
        }
    }

    val selectedCategory = state.categories.firstOrNull {
        it.category.id == state.selectedCategoryId
    }
    selectedCategory?.let { row ->
        AppModalBottomSheet(
            onDismissRequest = viewModel::dismissCategory,
            title = {
                Text(row.category.name, style = MaterialTheme.typography.titleLarge)
            },
            actions = {
                Button(onClick = viewModel::dismissCategory) {
                    Text(stringResource(R.string.action_done))
                }
            }
        ) {
            OutlinedTextField(
                value = row.openingBalanceText,
                onValueChange = { viewModel.updateOpeningBalance(row.category.id, it) },
                label = { Text(stringResource(R.string.house_opening_balance)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = row.allocatedText,
                onValueChange = { viewModel.updateAllocated(row.category.id, it) },
                label = { Text(stringResource(R.string.house_new_allocation)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            SummaryRow(
                stringResource(R.string.house_category_total),
                rowTotalCents(row)
            )
            if (state.hasAllocationOverflow) {
                Text(
                    stringResource(
                        R.string.house_allocation_overflow_error,
                        formatCents(state.allocationOverflowCents)
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    val selectedAccount = state.accounts.firstOrNull {
        it.account.id == state.selectedAccountId
    }
    selectedAccount?.let { row ->
        AppModalBottomSheet(
            onDismissRequest = viewModel::dismissAccount,
            title = {
                Text(row.account.name, style = MaterialTheme.typography.titleLarge)
            },
            actions = {
                Button(onClick = viewModel::dismissAccount) {
                    Text(stringResource(R.string.action_done))
                }
            }
        ) {
            OutlinedTextField(
                value = row.amountText,
                onValueChange = { viewModel.updateAccountAmount(row.account.id, it) },
                label = { Text(stringResource(R.string.house_account_month_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.hasPositionOverflow) {
                Text(
                    stringResource(
                        R.string.house_position_overflow_error,
                        formatCents(state.positionOverflowCents)
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
internal fun SummaryRow(label: String, cents: Long) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text(formatHouseCents(cents))
    }
}

internal fun monthLabel(month: Int, year: Int): String {
    val monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ITALIAN)
    return "${monthName.replaceFirstChar { it.uppercase() }} $year"
}

private fun rowTotalCents(row: HousePlanCategoryDraftUi): Long =
    parseCentsOrZeroLocal(row.openingBalanceText) + parseCentsOrZeroLocal(row.allocatedText)

private fun parseCentsOrZeroLocal(value: String): Long = runCatching {
    val normalized = value.trim().replace(',', '.')
    if (normalized.isEmpty()) return@runCatching 0L
    normalized.toBigDecimal()
        .setScale(2)
        .movePointRight(2)
        .longValueExact()
}.getOrDefault(0L)

internal fun formatHouseCents(cents: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.ITALY)
    return formatter.format(BigDecimal(cents).movePointLeft(2))
}

private fun formatCents(cents: Long): String = formatHouseCents(cents)
