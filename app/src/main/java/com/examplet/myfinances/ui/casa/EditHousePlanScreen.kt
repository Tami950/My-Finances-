package com.examplet.myfinances.ui.casa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.examplet.myfinances.ui.components.AppContentCard
import com.examplet.myfinances.ui.components.AppModalBottomSheet
import com.examplet.myfinances.ui.components.AppScreen

@Composable
fun EditHousePlanScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditHousePlanViewModel = hiltViewModel()
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
                        text = when (state.mode) {
                            HousePlanEditMode.PLAN -> stringResource(R.string.house_edit_plan_title)
                            HousePlanEditMode.POSITIONS -> stringResource(R.string.house_edit_positions_title)
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            if (state.isLoading) {
                item(key = "loading") {
                    Text(stringResource(R.string.house_loading_plan))
                }
            } else {
                item(key = "month") {
                    Text(
                        monthLabel(state.month, state.year),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                if (state.isClosed) {
                    item(key = "closed") {
                        Text(
                            stringResource(R.string.house_month_closed_edit_blocked),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                when (state.mode) {
                    HousePlanEditMode.PLAN -> {
                        item(key = "resources") {
                            OutlinedTextField(
                                value = state.totalResourcesText,
                                onValueChange = viewModel::updateTotalResources,
                                label = { Text(stringResource(R.string.house_month_resources)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                enabled = !state.isClosed,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item(key = "note") {
                            OutlinedTextField(
                                value = state.note,
                                onValueChange = viewModel::updateNote,
                                label = { Text(stringResource(R.string.house_month_note)) },
                                enabled = !state.isClosed,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item(key = "category-heading") {
                            Text(
                                stringResource(R.string.house_plan_categories_title),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        items(
                            state.categories,
                            key = { "edit-plan-category-${it.categoryId}" }
                        ) { row ->
                            AppContentCard(
                                modifier = Modifier.clickable(enabled = !state.isClosed) {
                                    viewModel.openCategory(row.categoryId)
                                }
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        row.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        formatHouseCents(
                                            parseCentsOrZero(row.openingBalanceText) +
                                                parseCentsOrZero(row.allocatedText)
                                        )
                                    )
                                }
                                Text(
                                    stringResource(
                                        R.string.house_plan_category_breakdown,
                                        formatHouseCents(parseCentsOrZero(row.openingBalanceText)),
                                        formatHouseCents(parseCentsOrZero(row.allocatedText))
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        item(key = "plan-summary") {
                            AppContentCard {
                                SummaryRow(
                                    stringResource(R.string.house_summary_resources),
                                    state.totalResourcesCents
                                )
                                SummaryRow(
                                    stringResource(R.string.house_summary_allocated),
                                    state.allocatedCents
                                )
                                SummaryRow(
                                    stringResource(R.string.house_summary_unallocated),
                                    state.unallocatedCents
                                )
                                if (state.allocationOverflowCents > 0) {
                                    Text(
                                        stringResource(
                                            R.string.house_allocation_overflow_error,
                                            formatHouseCents(state.allocationOverflowCents)
                                        ),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (state.existingPositionOverflowCents > 0) {
                                    Text(
                                        stringResource(R.string.house_resources_below_positions_error),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    HousePlanEditMode.POSITIONS -> {
                        item(key = "position-help") {
                            AppContentCard {
                                Text(
                                    stringResource(R.string.house_positions_current_help),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                SummaryRow(
                                    stringResource(R.string.house_summary_resources),
                                    state.totalResourcesCents
                                )
                            }
                        }

                        items(
                            state.positions,
                            key = { "edit-position-${it.account.id}" }
                        ) { row ->
                            AppContentCard(
                                modifier = Modifier.clickable(enabled = !state.isClosed) {
                                    viewModel.openAccount(row.account.id)
                                }
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        row.account.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(formatHouseCents(parseCentsOrZero(row.amountText)))
                                }
                            }
                        }

                        item(key = "positions-summary") {
                            AppContentCard {
                                SummaryRow(
                                    stringResource(R.string.house_summary_positioned),
                                    state.positionedCents
                                )
                                SummaryRow(
                                    stringResource(R.string.house_summary_unpositioned),
                                    state.unpositionedCents
                                )
                                if (state.positionOverflowCents > 0) {
                                    Text(
                                        stringResource(
                                            R.string.house_position_overflow_error,
                                            formatHouseCents(state.positionOverflowCents)
                                        ),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
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
                        onClick = viewModel::save,
                        enabled = state.canSave,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(
                                if (state.isSaving) R.string.action_saving
                                else R.string.action_save
                            )
                        )
                    }
                }
            }
        }
    }

    if (state.mode == HousePlanEditMode.PLAN) {
        val selected = state.categories.firstOrNull {
            it.categoryId == state.selectedCategoryId
        }
        selected?.let { row ->
            AppModalBottomSheet(
                onDismissRequest = viewModel::dismissCategory,
                title = { Text(row.name, style = MaterialTheme.typography.titleLarge) },
                actions = {
                    Button(onClick = viewModel::dismissCategory) {
                        Text(stringResource(R.string.action_done))
                    }
                }
            ) {
                OutlinedTextField(
                    value = row.openingBalanceText,
                    onValueChange = { viewModel.updateOpeningBalance(row.categoryId, it) },
                    label = { Text(stringResource(R.string.house_opening_balance)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = row.allocatedText,
                    onValueChange = { viewModel.updateAllocated(row.categoryId, it) },
                    label = { Text(stringResource(R.string.house_new_allocation)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SummaryRow(
                    stringResource(R.string.house_category_total),
                    parseCentsOrZero(row.openingBalanceText) + parseCentsOrZero(row.allocatedText)
                )
                if (state.allocationOverflowCents > 0) {
                    Text(
                        stringResource(
                            R.string.house_allocation_overflow_error,
                            formatHouseCents(state.allocationOverflowCents)
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (state.mode == HousePlanEditMode.POSITIONS) {
        val selected = state.positions.firstOrNull {
            it.account.id == state.selectedAccountId
        }
        selected?.let { row ->
            AppModalBottomSheet(
                onDismissRequest = viewModel::dismissAccount,
                title = { Text(row.account.name, style = MaterialTheme.typography.titleLarge) },
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
                    trailingIcon = {
                        if (row.amountText.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    viewModel.updateAccountAmount(row.account.id, "")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.action_clear_amount)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.positionOverflowCents > 0) {
                    Text(
                        stringResource(
                            R.string.house_position_overflow_error,
                            formatHouseCents(state.positionOverflowCents)
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
