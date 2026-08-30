package com.examplet.myfinances.ui.casa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.examplet.myfinances.R
import com.examplet.myfinances.domain.model.HouseCategory
import com.examplet.myfinances.domain.model.HouseCategoryType
import com.examplet.myfinances.domain.model.MoneyAccount
import com.examplet.myfinances.domain.model.MoneyAccountType
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CasaScreen(viewModel: CasaViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = state.selectedTab.ordinal) {
            Tab(
                selected = state.selectedTab == CasaTab.PLANNING,
                onClick = { viewModel.selectTab(CasaTab.PLANNING) },
                text = { Text(stringResource(R.string.house_tab_planning)) }
            )
            Tab(
                selected = state.selectedTab == CasaTab.CUSTOMIZATION,
                onClick = { viewModel.selectTab(CasaTab.CUSTOMIZATION) },
                text = { Text(stringResource(R.string.house_tab_customization)) }
            )
        }

        when (state.selectedTab) {
            CasaTab.PLANNING -> PlanningContent(
                setupCompleted = state.isHouseSetupCompleted,
                onConfigure = viewModel::startHouseSetup
            )
            CasaTab.CUSTOMIZATION -> CustomizationContent(
                state = state,
                onAddCategory = viewModel::openNewCategory,
                onEditCategory = viewModel::openCategory,
                onArchiveCategory = viewModel::archiveCategory,
                onReactivateCategory = viewModel::reactivateCategory,
                onAddAccount = viewModel::openNewMoneyAccount,
                onEditAccount = viewModel::openMoneyAccount,
                onArchiveAccount = viewModel::archiveMoneyAccount,
                onReactivateAccount = viewModel::reactivateMoneyAccount,
                onCompleteSetup = viewModel::completeHouseSetup
            )
        }
    }

    state.categoryDraft?.let { draft ->
        CategoryDialog(
            draft = draft,
            errorMessage = state.errorMessage,
            onNameChange = viewModel::updateCategoryDraftName,
            onTypeChange = viewModel::updateCategoryDraftType,
            onTargetChange = viewModel::updateCategoryDraftTarget,
            onSave = viewModel::saveCategory,
            onDismiss = viewModel::dismissCategoryDialog
        )
    }

    state.moneyAccountDraft?.let { draft ->
        MoneyAccountDialog(
            draft = draft,
            errorMessage = state.errorMessage,
            onNameChange = viewModel::updateMoneyAccountDraftName,
            onTypeChange = viewModel::updateMoneyAccountDraftType,
            onSave = viewModel::saveMoneyAccount,
            onDismiss = viewModel::dismissMoneyAccountDialog
        )
    }
}

@Composable
private fun PlanningContent(
    setupCompleted: Boolean,
    onConfigure: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!setupCompleted) {
            Text(stringResource(R.string.house_setup_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.house_setup_description))
            Spacer(Modifier.height(24.dp))
            Button(onClick = onConfigure) { Text(stringResource(R.string.house_setup_action)) }
        } else {
            val month = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN))
            Text(
                stringResource(R.string.house_planning_empty_title, month.replaceFirstChar { it.uppercase() }),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.house_planning_empty_description))
            Spacer(Modifier.height(24.dp))
            Button(onClick = {}, enabled = false) { Text(stringResource(R.string.house_plan_month)) }
        }
    }
}

@Composable
private fun CustomizationContent(
    state: CasaUiState,
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "categories-title") {
            Text(stringResource(R.string.house_categories_title), style = MaterialTheme.typography.titleLarge)
        }
        if (state.categories.isEmpty()) {
            item(key = "categories-empty") { Text(stringResource(R.string.house_categories_empty)) }
        }
        items(state.categories, key = { "category-${it.id}" }) { category ->
            ManagerRow(
                title = category.name,
                subtitle = when (category.type) {
                    HouseCategoryType.FLEXIBLE -> stringResource(R.string.house_category_type_flexible)
                    HouseCategoryType.TARGET -> stringResource(
                        R.string.house_category_type_target,
                        formatCents(category.targetCents ?: 0)
                    )
                },
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
        if (state.moneyAccounts.isEmpty()) {
            item(key = "accounts-empty") { Text(stringResource(R.string.house_accounts_empty)) }
        }
        items(state.moneyAccounts, key = { "account-${it.id}" }) { account ->
            ManagerRow(
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
                Button(
                    onClick = onCompleteSetup,
                    enabled = state.canCompleteSetup,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.house_setup_complete))
                }
            }
        }
    }
}

@Composable
private fun ManagerRow(
    title: String,
    subtitle: String,
    isArchived: Boolean,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onReactivate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isArchived) 0.5f else 1f)
            .clickable(enabled = !isArchived, onClick = onClick)
            .padding(vertical = 8.dp),
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
private fun CategoryDialog(
    draft: CategoryDraft,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onTypeChange: (HouseCategoryType) -> Unit,
    onTargetChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (draft.id == null) R.string.house_add_category else R.string.house_edit_category)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.house_category_name)) },
                    singleLine = true
                )
                ChoiceRow(stringResource(R.string.house_category_flexible), draft.type == HouseCategoryType.FLEXIBLE) {
                    onTypeChange(HouseCategoryType.FLEXIBLE)
                }
                ChoiceRow(stringResource(R.string.house_category_target), draft.type == HouseCategoryType.TARGET) {
                    onTypeChange(HouseCategoryType.TARGET)
                }
                if (draft.type == HouseCategoryType.TARGET) {
                    OutlinedTextField(
                        value = draft.targetText,
                        onValueChange = onTargetChange,
                        label = { Text(stringResource(R.string.house_category_target_amount)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun MoneyAccountDialog(
    draft: MoneyAccountDraft,
    errorMessage: String?,
    onNameChange: (String) -> Unit,
    onTypeChange: (MoneyAccountType) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (draft.id == null) R.string.house_add_account else R.string.house_edit_account)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.house_account_name)) },
                    singleLine = true
                )
                MoneyAccountType.entries.forEach { type ->
                    ChoiceRow(moneyAccountTypeLabel(type), draft.type == type) { onTypeChange(type) }
                }
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
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

private fun formatCents(cents: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale.ITALY).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return formatter.format(BigDecimal(cents).movePointLeft(2))
}
