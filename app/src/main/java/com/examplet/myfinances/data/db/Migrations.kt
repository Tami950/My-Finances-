package com.examplet.myfinances.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE house_months ADD COLUMN openingAvailableCents INTEGER NOT NULL DEFAULT 0"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `house_month_closings` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `houseMonthId` INTEGER NOT NULL,
                `calculatedAvailableCents` INTEGER NOT NULL,
                `confirmedAvailableCents` INTEGER NOT NULL,
                `availableAdjustmentCents` INTEGER NOT NULL,
                `availableAdjustmentNote` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`houseMonthId`) REFERENCES `house_months`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_house_month_closings_houseMonthId` ON `house_month_closings` (`houseMonthId`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `house_month_category_closings` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `houseMonthId` INTEGER NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `calculatedBalanceCents` INTEGER NOT NULL,
                `confirmedBalanceCents` INTEGER NOT NULL,
                `adjustmentCents` INTEGER NOT NULL,
                `adjustmentNote` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`houseMonthId`) REFERENCES `house_months`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`categoryId`) REFERENCES `house_categories`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_house_month_category_closings_houseMonthId_categoryId` ON `house_month_category_closings` (`houseMonthId`, `categoryId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_house_month_category_closings_categoryId` ON `house_month_category_closings` (`categoryId`)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `house_month_closing_transfers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `houseMonthId` INTEGER NOT NULL,
                `sourceCategoryId` INTEGER NOT NULL,
                `destinationType` TEXT NOT NULL,
                `destinationCategoryId` INTEGER,
                `amountCents` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`houseMonthId`) REFERENCES `house_months`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`sourceCategoryId`) REFERENCES `house_categories`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
                FOREIGN KEY(`destinationCategoryId`) REFERENCES `house_categories`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_house_month_closing_transfers_houseMonthId` ON `house_month_closing_transfers` (`houseMonthId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_house_month_closing_transfers_sourceCategoryId` ON `house_month_closing_transfers` (`sourceCategoryId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_house_month_closing_transfers_destinationCategoryId` ON `house_month_closing_transfers` (`destinationCategoryId`)"
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `house_month_available_closing_transfers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `houseMonthId` INTEGER NOT NULL,
                `destinationCategoryId` INTEGER NOT NULL,
                `amountCents` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`houseMonthId`) REFERENCES `house_months`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`destinationCategoryId`) REFERENCES `house_categories`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_house_month_available_closing_transfers_houseMonthId_destinationCategoryId` ON `house_month_available_closing_transfers` (`houseMonthId`, `destinationCategoryId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_house_month_available_closing_transfers_destinationCategoryId` ON `house_month_available_closing_transfers` (`destinationCategoryId`)"
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE house_categories ADD COLUMN behavior TEXT NOT NULL DEFAULT 'BUDGET'"
        )
        db.execSQL(
            "ALTER TABLE house_monthly_allocations ADD COLUMN categoryBehavior TEXT NOT NULL DEFAULT 'BUDGET'"
        )
        db.execSQL(
            "ALTER TABLE house_monthly_allocations ADD COLUMN fixedExpensePaymentStatus TEXT"
        )
        db.execSQL(
            "ALTER TABLE house_month_category_closings ADD COLUMN categoryBehavior TEXT NOT NULL DEFAULT 'BUDGET'"
        )
        db.execSQL(
            "ALTER TABLE house_month_category_closings ADD COLUMN fixedExpenseClosingAction TEXT"
        )
        db.execSQL(
            "ALTER TABLE house_month_category_closings ADD COLUMN fixedExpensePendingNote TEXT"
        )
        db.execSQL(
            "ALTER TABLE house_month_closings ADD COLUMN unreconciledFixedExpenseDeficitCents INTEGER NOT NULL DEFAULT 0"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `house_fixed_expense_pendings` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sourceHouseMonthId` INTEGER NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `amountCents` INTEGER NOT NULL,
                `note` TEXT,
                `status` TEXT NOT NULL,
                `resolvedAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`sourceHouseMonthId`) REFERENCES `house_months`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`categoryId`) REFERENCES `house_categories`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_house_fixed_expense_pendings_sourceHouseMonthId` ON `house_fixed_expense_pendings` (`sourceHouseMonthId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_house_fixed_expense_pendings_categoryId` ON `house_fixed_expense_pendings` (`categoryId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_house_fixed_expense_pendings_status` ON `house_fixed_expense_pendings` (`status`)"
        )
    }
}
