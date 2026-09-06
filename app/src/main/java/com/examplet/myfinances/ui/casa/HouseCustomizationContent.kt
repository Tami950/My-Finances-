package com.examplet.myfinances.ui.casa

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.examplet.myfinances.R
import com.examplet.myfinances.domain.model.HouseCategory
import com.examplet.myfinances.domain.model.HouseCategoryBehavior
import com.examplet.myfinances.domain.model.HouseCategoryType
import com.examplet.myfinances.domain.model.HouseMonthStatus
import com.examplet.myfinances.domain.model.HousePlanDetails
import com.examplet.myfinances.domain.model.MoneyAccount
import com.examplet.myfinances.domain.model.MoneyAccountType
import com.examplet.myfinances.ui.components.AppContentCard
import com.examplet.myfinances.ui.components.AppModalBottomSheet
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun HouseCustomizationContent(
    state: CasaUiState,
    onEditUsualResources: () -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (HouseCategory) -> Unit,
    onArchiveCategory: (Long) -> Unit,
    onReactivateCategory: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onEditAccount: (MoneyAccount) -> Unit,
    onArchiveAccount: (Long) -> Unit,
    onReactivateAccount: (Long) -> Unit,
    onCompleteSetup: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "planning-defaults") {
            Text(stringResource(R.string.house_customization_planning_defaults), style = MaterialTheme.typography.titleLarge)
            AppContentCard(modifier = Modifier.clickable(onClick = onEditUsualResources)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.house_usual_monthly_resources), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.house_usual_monthly_resources_help), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(formatHouseCents(state.usualHouseMonthlyResourcesCents))
                }
            }
        }
        item(key = "defaults-divider") { HorizontalDivider() }

        item(key = "categories-title") {
            Text(stringResource(R.string.house_categories_title), style = MaterialTheme.typography.titleLarge)
        }
        if (state.categories.isEmpty()) item(key = "categories-empty") { Text(stringResource(R.string.house_categories_empty)) }
        items(state.categories, key = { "category-${it.id}" }) { category ->
            val subtitle = if (category.behavior == HouseCategoryBehavior.FIXED_EXPENSE) {
                "${stringResource(R.string.house_category_fixed_expense_badge)} · ${formatHouseCents(category.fixedExpenseDefaultCents ?: 0L)}"
            } else {
                when (category.type) {
                    HouseCategoryType.FLEXIBLE -> stringResource(R.string.house_category_type_flexible)
                    HouseCategoryType.TARGET -> stringResource(
                        R.string.house_category_type_target,
                        formatPlainCents(category.targetCents ?: 0)
                    )
                }
            }
            HouseManagerRow(
                title = category.name,
                subtitle = subtitle,
                isArchived = category.isArchived,
                onClick = { onEditCategory(category) },
                onArchive = { onArchiveCategory(category.id) },
                onReactivate = { onReactivateCategory(category.id) }
            )
        }
        item(key = "category-add") {
            OutlinedButton(onClick = onAddCategory) { Text(stringResource(R.string.house_add_category)) }
        }
        item(key = "manager-divider") { HorizontalDivider(); Spacer(Modifier.height(4.dp)) }
        item(key = "accounts-title") {
            Text(stringResource(R.string.house_accounts_title), style = MaterialTheme.typography.titleLarge)
        }
        if (state.moneyAccounts.isEmpty()) item(key = "accounts-empty") { Text(stringResource(R.string.house_accounts_empty)) }
        items(state.moneyAccounts, key = { "account-${it.id}" }) { account ->
            HouseManagerRow(
                title = account.name,
                subtitle = moneyAccountTypeLabel(account.type),
                isArchived = account.isArchived,
                onClick = { onEditAccount(account) },
                onArchive = { onArchiveAccount(account.id) },
                onReactivate = { onReactivateAccount(account.id) }
            )
        }
        item(key = "account-add") {
            OutlinedButton(onClick = onAddAccount) { Text(stringResource(R.string.house_add_account)) }
        }
        if (!state.isHouseSetupCompleted) {
            item(key = "setup-complete") {
                Spacer(Modifier.height(12.dp))
                if (!state.canCompleteSetup) {
                    Text(stringResource(R.string.house_setup_requirement), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }
                Button(onClick = onCompleteSetup, enabled = state.canCompleteSetup, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.house_setup_complete))
                }
            }
        }
    }
}

@Composable
private fun HouseManagerRow(
    title: String,
    subtitle: String,
    isArchived: Boolean,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onReactivate: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().alpha(if (isArchived) 0.5f else 1f)
            .clickable(enabled = !isArchived, onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                if (isArchived) "$subtitle · ${stringResource(R.string.house_archived)}" else subtitle,
                style = MaterialTheme.typography.bodySmall
            )
        }
        TextButton(onClick = if (isArchived) onReactivate else onArchive) {
            Text(stringResource(if (isArchived) R.string.house_reactivate else R.string.house_archive))
        }
    }
}

@Composable
internal fun HouseCategorySheet(
    draft: CategoryDraft,
    categories: List<HouseCategory>,
    currentPlanDetails: HousePlanDetails?,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onTypeChange: (HouseCategoryType) -> Unit,
    onTargetChange: (String) -> Unit,
    onFixedExpenseChange: (Boolean) -> Unit,
    onFixedExpenseDefaultChange: (String) -> Unit,
    onApplyToCurrentMonthChange: (Boolean) -> Unit,
    onUseAvailableChange: (Boolean) -> Unit,
    onReallocationCategoryChange: (Long) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentAllocation = currentPlanDetails?.allocations?.firstOrNull { it.categoryId == draft.id }
    val newFixedCents = parseCentsOrZeroLocal(draft.fixedExpenseDefaultText)
    val oldCurrentCents = currentAllocation?.let {
        if (it.categoryBehavior == HouseCategoryBehavior.FIXED_EXPENSE) {
            it.fixedExpensePlannedCents ?: it.allocatedCents
        } else it.openingBalanceCents + it.allocatedCents
    }
    val delta = if (draft.isFixedExpense && oldCurrentCents != null) newFixedCents - oldCurrentCents else 0L
    val budgetChoices = currentPlanDetails?.allocations.orEmpty().filter {
        it.categoryId != draft.id && it.categoryBehavior == HouseCategoryBehavior.BUDGET
    }

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (draft.id == null) R.string.house_add_category else R.string.house_edit_category), style = MaterialTheme.typography.titleLarge) },
        actions = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            Button(onClick = onSave) { Text(stringResource(R.string.action_save)) }
        }
    ) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.house_category_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth().clickable { onFixedExpenseChange(!draft.isFixedExpense) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = draft.isFixedExpense, onCheckedChange = onFixedExpenseChange)
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.house_category_fixed_expense))
                Text(stringResource(R.string.house_category_fixed_expense_help), style = MaterialTheme.typography.bodySmall)
            }
        }

        if (draft.isFixedExpense) {
            OutlinedTextField(
                value = draft.fixedExpenseDefaultText,
                onValueChange = onFixedExpenseDefaultChange,
                label = { Text(stringResource(R.string.house_fixed_expense_default_amount)) },
                supportingText = { Text(stringResource(R.string.house_fixed_expense_default_amount_help)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (draft.id != null && currentPlanDetails?.status == HouseMonthStatus.OPEN && currentAllocation != null) {
                Text(stringResource(R.string.house_apply_fixed_default_scope), style = MaterialTheme.typography.titleMedium)
                HouseChoiceRow(stringResource(R.string.house_apply_from_next_month), !draft.applyToCurrentMonth) {
                    onApplyToCurrentMonthChange(false)
                }
                HouseChoiceRow(stringResource(R.string.house_apply_to_current_month), draft.applyToCurrentMonth) {
                    onApplyToCurrentMonthChange(true)
                }

                if (draft.applyToCurrentMonth && delta != 0L) {
                    Text(
                        stringResource(
                            if (delta > 0) R.string.house_fixed_increase_source else R.string.house_fixed_decrease_destination,
                            formatHouseCents(kotlin.math.abs(delta))
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val available = currentPlanDetails.availableCents
                    HouseChoiceRow(
                        stringResource(R.string.house_use_available, formatHouseCents(available)),
                        draft.useAvailableForReallocation
                    ) { onUseAvailableChange(true) }
                    budgetChoices.forEach { allocation ->
                        val amount = allocation.openingBalanceCents + allocation.allocatedCents
                        HouseChoiceRow(
                            "${allocation.categoryName} (${formatHouseCents(amount)})",
                            !draft.useAvailableForReallocation && draft.reallocationCategoryId == allocation.categoryId
                        ) { onReallocationCategoryChange(allocation.categoryId) }
                    }
                }
            }
        } else {
            HouseChoiceRow(stringResource(R.string.house_category_flexible), draft.type == HouseCategoryType.FLEXIBLE) {
                onTypeChange(HouseCategoryType.FLEXIBLE)
            }
            HouseChoiceRow(stringResource(R.string.house_category_target), draft.type == HouseCategoryType.TARGET) {
                onTypeChange(HouseCategoryType.TARGET)
            }
            if (draft.type == HouseCategoryType.TARGET) {
                OutlinedTextField(
                    value = draft.targetText,
                    onValueChange = onTargetChange,
                    label = { Text(stringResource(R.string.house_category_target_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
internal fun HouseUsualResourcesSheet(
    value: String,
    errorMessage: String?,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.house_usual_monthly_resources), style = MaterialTheme.typography.titleLarge) },
        actions = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            Button(onClick = onSave) { Text(stringResource(R.string.action_save)) }
        }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.house_usual_monthly_resources)) },
            supportingText = { Text(stringResource(R.string.house_usual_monthly_resources_help)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
internal fun HouseMoneyAccountSheet(
    draft: MoneyAccountDraft,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onTypeChange: (MoneyAccountType) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (draft.id == null) R.string.house_add_account else R.string.house_edit_account), style = MaterialTheme.typography.titleLarge) },
        actions = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            Button(onClick = onSave) { Text(stringResource(R.string.action_save)) }
        }
    ) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.house_account_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        MoneyAccountType.entries.forEach { type ->
            HouseChoiceRow(moneyAccountTypeLabel(type), draft.type == type) { onTypeChange(type) }
        }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun HouseChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@Composable
private fun moneyAccountTypeLabel(type: MoneyAccountType): String = when (type) {
    MoneyAccountType.CASH -> stringResource(R.string.house_account_type_cash)
    MoneyAccountType.BANK_ACCOUNT -> stringResource(R.string.house_account_type_bank)
    MoneyAccountType.CARD -> stringResource(R.string.house_account_type_card)
    MoneyAccountType.OTHER -> stringResource(R.string.house_account_type_other)
}

private fun parseCentsOrZeroLocal(value: String): Long = runCatching {
    val normalized = value.trim().replace(',', '.')
    if (normalized.isEmpty()) return@runCatching 0L
    normalized.toBigDecimal().setScale(2).movePointRight(2).longValueExact()
}.getOrDefault(0L)

private fun formatPlainCents(cents: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.ITALY).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return formatter.format(BigDecimal(cents).movePointLeft(2))
}
