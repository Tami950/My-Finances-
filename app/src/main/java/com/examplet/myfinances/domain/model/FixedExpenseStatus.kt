package com.examplet.myfinances.domain.model

enum class FixedExpensePaymentStatus {
    PLANNED,
    PAID
}

enum class FixedExpenseClosingAction {
    MARK_PAID,
    KEEP_PENDING,
    CANCELLED
}

enum class FixedExpensePendingStatus {
    PENDING,
    PAID
}
