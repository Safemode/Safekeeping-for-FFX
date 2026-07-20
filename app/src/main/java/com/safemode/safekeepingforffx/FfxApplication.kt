package com.safemode.safekeepingforffx

import android.app.Application
import com.safemode.safekeepingforffx.di.AppContainer

class FfxApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
