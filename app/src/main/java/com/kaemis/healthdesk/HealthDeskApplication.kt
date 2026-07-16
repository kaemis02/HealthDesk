package com.kaemis.healthdesk

import android.app.Application
import com.kaemis.healthdesk.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HealthDeskApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        applicationScope.launch { appContainer.initializeDefaults() }
    }
}
