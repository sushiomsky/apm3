package com.example.proxmark

import android.app.Application

class ProxmarkApplication : Application() {
    val container: AppContainer by lazy {
        AppContainer(this)
    }
}
