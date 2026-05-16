package com.edgellm

import android.app.Application
import com.edgellm.data.ChatRepository
import com.edgellm.data.db.AppDatabase

class MyApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ChatRepository(database.chatDao()) }
    
    override fun onCreate() {
        super.onCreate()
    }
}
