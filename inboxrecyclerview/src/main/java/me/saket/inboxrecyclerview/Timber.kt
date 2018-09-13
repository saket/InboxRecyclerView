package me.saket.inboxrecyclerview

import android.annotation.SuppressLint
import android.util.Log

/** This class exists because I keep typing Timber.i() everywhere. */
internal object Timber {

  @SuppressLint("LogNotTimber")
  fun i(message: String) {
    if (BuildConfig.DEBUG) {
      Log.i("IRV", message)
    }
  }

  @SuppressLint("LogNotTimber")
  fun w(message: String) {
    if (BuildConfig.DEBUG) {
      Log.w("IRV", message)
    }
  }
}
