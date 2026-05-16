package com.edgellm

import android.app.Application
import com.edgellm.data.db.AppDatabase

class MyApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
    }
}
