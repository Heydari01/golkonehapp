package com.example.greenhouse

import android.app.Application
import com.example.greenhouse.data.repository.GreenhouseRepository
import com.example.greenhouse.util.NetworkMonitor

class GreenhouseApp : Application() {
    lateinit var repository: GreenhouseRepository
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        repository = GreenhouseRepository(this)
        networkMonitor = NetworkMonitor(this)
    }
}
