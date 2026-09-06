package com.examplet.myfinances

import android.app.Application
import com.examplet.myfinances.data.db.MyFinancesDatabase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BudgetApp : Application() {

    @Inject
    lateinit var database: MyFinancesDatabase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Open the database once at startup so Room creates/validates the local schema
        // even before the first feature screen starts using a DAO.
        applicationScope.launch {
            database.openHelper.writableDatabase
        }
    }
}
