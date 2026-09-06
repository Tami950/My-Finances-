package com.examplet.myfinances.ui.casa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examplet.myfinances.R
import com.examplet.myfinances.domain.model.HouseClosingDestinationType
import com.examplet.myfinances.ui.components.AppContentCard
import com.examplet.myfinances.ui.components.AppModalBottomSheet
import com.examplet.myfinances.ui.components.AppScreen

@Composable
fun CloseHouseMonthScreen(
    onBack: () -> Unit,
    onClosed: () -> Unit,
    viewModel: CloseHouseMonthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isClosedSuccessfully) {
        if (state.isClosedSuccessfully) onClosed()
    }

    AppScreen {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.action_back))
                    }
                    Text(
                        text = if (state.month in 1..12 && state.year > 0) {
                            stringResource(
                                R.string.house_close_month_title,
                                monthLabel(state.month, state.year)
                            )
                        } else {
                            stringResource(R.string.house_close_month_action)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            if (state.isLoading) {
                item(key = "loading") {
                    Text(stringResource(R.string.house_loading_plan))
                }
            } else {
                item(key = "intro") {
                    AppContentCard {
                        Text(
                            stringResource(R.string.house_closing_intro_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            stringResource(R.string.house_closing_intro_description),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                item(key = "available") {
                    AppContentCard {
                        Text(
                            stringResource(R.string.house_summary_unallocated),
                            style = MaterialTheme.typography.titleMedium
                        )
                        ClosingValueRow(
                            stringResource(R.string.house_closing_calculated_balance),
                            state.calculatedAvailableCents
                        )
                        OutlinedTextField(
                            value = state.confirmedAvailableText,
                            onValueChange = viewModel::updateConfirmedAvailable,
                            label = { Text(stringResource(R.string.house_closing_confirmed_balance)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        state.availableAdjustmentCents?.takeIf { it != 0L }?.let { adjustment ->
                            ClosingAdjustment(adjustment)
                            OutlinedTextField(
                                value = state.availableAdjustmentNote,
                                onValueChange = viewModel::updateAvailableAdjustmentNote,
                                label = { Text(stringResource(R.string.house_closing_adjustment_note)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Text(
                            stringResource(R.string.house_closing_available_rollover_help),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                item(key = "categories-title") {
                    Text(
                        stringResource(R.string.house_closing_categories_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                items(
                    state.categories,
                    key = { "closing-category-${it.categoryId}" }
                ) { row ->
                    AppContentCard(
                        modifier = Modifier.clickable { viewModel.openCategory(row.categoryId) }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                row.categoryName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                row.confirmedBalanceCents?.let(::formatHouseCents) ?: "—",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        ClosingValueRow(
                            stringResource(R.string.house_closing_calculated_balance),
                            row.calculatedBalanceCents
                        )
                        ClosingValueRow(
                            stringResource(R.string.house_closing_distributed),
                            row.distributedCents ?: 0
                        )
                        row.adjustmentCents?.takeIf { it != 0L }?.let { adjustment ->
                            ClosingAdjustment(adjustment)
                        }
                        val remaining = row.remainingCents
                        if (remaining != null && remaining != 0L) {
                            Text(
                                text = if (remaining > 0) {
                                    stringResource(
                                        R.string.house_closing_still_to_distribute,
                                        formatHouseCents(remaining)
                                    )
                                } else {
                                    stringResource(
                                        R.string.house_closing_over_distributed,
                                        formatHouseCents(-remaining)
                                    )
                                },
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else if (row.isValid) {
                            Text(
                                stringResource(R.string.house_closing_distribution_ok),
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

                item(key = "close") {
                    Button(
                        onClick = viewModel::requestClose,
                        enabled = state.canClose,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(
                                if (state.isClosing) R.string.house_closing_in_progress
                                else R.string.house_close_month_action
                            )
                        )
                    }
                }
            }
        }
    }

    val selected = state.categories.firstOrNull {
        it.categoryId == state.selectedCategoryId
    }
    selected?.let { row ->
        AppModalBottomSheet(
            onDismissRequest = viewModel::dismissCategory,
            title = {
                Text(row.categoryName, style = MaterialTheme.typography.titleLarge)
            },
            actions = {
                Button(onClick = viewModel::dismissCategory) {
                    Text(stringResource(R.string.action_done))
                }
            }
        ) {
            ClosingValueRow(
                stringResource(R.string.house_closing_calculated_balance),
                row.calculatedBalanceCents
            )
            OutlinedTextField(
                value = row.confirmedBalanceText,
                onValueChange = { viewModel.updateConfirmedBalance(row.categoryId, it) },
                label = { Text(stringResource(R.string.house_closing_confirmed_balance)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            row.adjustmentCents?.takeIf { it != 0L }?.let { adjustment ->
                ClosingAdjustment(adjustment)
                OutlinedTextField(
                    value = row.adjustmentNote,
                    onValueChange = { viewModel.updateCategoryAdjustmentNote(row.categoryId, it) },
                    label = { Text(stringResource(R.string.house_closing_adjustment_note)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                stringResource(R.string.house_closing_destinations_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.house_closing_destinations_help),
                style = MaterialTheme.typography.bodySmall
            )

            row.destinations.forEach { destination ->
                val destinationLabel = when {
                    destination.type == HouseClosingDestinationType.AVAILABLE ->
                        stringResource(R.string.house_summary_unallocated)

                    destination.categoryId == row.categoryId ->
                        stringResource(R.string.house_closing_keep_in_category, destination.label)

                    else -> destination.label
                }
                OutlinedTextField(
                    value = destination.amountText,
                    onValueChange = {
                        viewModel.updateDestinationAmount(
                            sourceCategoryId = row.categoryId,
                            destinationType = destination.type,
                            destinationCategoryId = destination.categoryId,
                            value = it
                        )
                    },
                    label = { Text(destinationLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val remaining = row.remainingCents
            when {
                remaining == null -> Text(
                    stringResource(R.string.house_closing_invalid_amount),
                    color = MaterialTheme.colorScheme.error
                )

                remaining > 0 -> Text(
                    stringResource(
                        R.string.house_closing_still_to_distribute,
                        formatHouseCents(remaining)
                    ),
                    color = MaterialTheme.colorScheme.error
                )

                remaining < 0 -> Text(
                    stringResource(
                        R.string.house_closing_over_distributed,
                        formatHouseCents(-remaining)
                    ),
                    color = MaterialTheme.colorScheme.error
                )

                else -> Text(stringResource(R.string.house_closing_distribution_ok))
            }
        }
    }

    if (state.showConfirmation) {
        AppModalBottomSheet(
            onDismissRequest = viewModel::dismissConfirmation,
            title = {
                Text(
                    stringResource(R.string.house_closing_confirm_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            actions = {
                TextButton(onClick = viewModel::dismissConfirmation) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(onClick = viewModel::confirmClose) {
                    Text(stringResource(R.string.house_close_month_action))
                }
            }
        ) {
            Text(stringResource(R.string.house_closing_confirm_description))
            state.availableAdjustmentCents?.takeIf { it != 0L }?.let { adjustment ->
                ClosingAdjustment(adjustment)
            }
            val categoryAdjustments = state.categories.count { (it.adjustmentCents ?: 0) != 0L }
            if (categoryAdjustments > 0) {
                Text(
                    stringResource(
                        R.string.house_closing_adjusted_categories_count,
                        categoryAdjustments
                    )
                )
            }
        }
    }
}

@Composable
private fun ClosingValueRow(label: String, cents: Long) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text(formatHouseCents(cents))
    }
}

@Composable
private fun ClosingAdjustment(adjustmentCents: Long) {
    Text(
        stringResource(
            R.string.house_closing_adjustment_value,
            if (adjustmentCents > 0) "+${formatHouseCents(adjustmentCents)}"
            else formatHouseCents(adjustmentCents)
        ),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodySmall
    )
}
