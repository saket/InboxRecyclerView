package me.saket.baseandroidproject

import android.app.Application
import timber.log.Timber
import timber.log.Timber.DebugTree

class App : Application() {

  override fun onCreate() {
    super.onCreate()
    Timber.plant(DebugTree())
  }
}
