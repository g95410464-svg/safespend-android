package com.safespend.app

import android.app.Application
import android.content.Context
import com.safespend.app.data.AppDatabase
import com.safespend.app.data.FinanceRepository
import com.safespend.app.data.SettingsStore
import com.safespend.app.service.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Manual DI container (keeps the project free of Hilt/KSP extras). */
class AppContainer(context: Context) {
    val db = AppDatabase.build(context)
    val repository = FinanceRepository(db)
    val settings = SettingsStore(context)
    val notifications = NotificationHelper(context)
}

class FinanceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CoroutineScope(Dispatchers.IO).launch { container.repository.ensureAccounts() }
    }
}
