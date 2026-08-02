package com.example.drift

import android.app.Application

class DriftApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UsageCollectionWorker.schedule(this)
    }
}
