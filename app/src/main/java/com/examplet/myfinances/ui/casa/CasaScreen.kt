package com.examplet.myfinances.ui.casa

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.examplet.myfinances.R
import com.examplet.myfinances.ui.components.AppScreen

@Composable
fun CasaScreen(
    onCreatePlan: () -> Unit,
    onEditPlan: (Long) -> Unit,
    onEditPositions: (Long) -> Unit,
    onCloseMonth: (Long) -> Unit,
    viewModel: CasaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.resetToPlanning()
    }

    AppScreen {
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
                CasaTab.PLANNING -> HousePlanningContent(
                    state = state,
                    onConfigure = viewModel::startHouseSetup,
                    onCreatePlan = onCreatePlan,
                    onEditPlan = onEditPlan,
                    onEditPositions = onEditPositions,
                    onCloseMonth = onCloseMonth,
                    onSetFixedExpensePaid = viewModel::setFixedExpensePaid,
                    onMarkPendingPaid = viewModel::markPendingFixedExpensePaid
                )

                CasaTab.CUSTOMIZATION -> HouseCustomizationContent(
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
    }

    state.categoryDraft?.let { draft ->
        HouseCategorySheet(
            draft = draft,
            errorMessage = state.errorMessage,
            onNameChange = viewModel::updateCategoryDraftName,
            onTypeChange = viewModel::updateCategoryDraftType,
            onTargetChange = viewModel::updateCategoryDraftTarget,
            onFixedExpenseChange = viewModel::updateCategoryDraftFixedExpense,
            onSave = viewModel::saveCategory,
            onDismiss = viewModel::dismissCategoryDialog
        )
    }

    state.moneyAccountDraft?.let { draft ->
        HouseMoneyAccountSheet(
            draft = draft,
            errorMessage = state.errorMessage,
            onNameChange = viewModel::updateMoneyAccountDraftName,
            onTypeChange = viewModel::updateMoneyAccountDraftType,
            onSave = viewModel::saveMoneyAccount,
            onDismiss = viewModel::dismissMoneyAccountDialog
        )
    }
}
