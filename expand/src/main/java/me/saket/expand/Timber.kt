package me.saket.expand

import android.annotation.SuppressLint
import android.util.Log

/** This class exists because I keep typing Timber.i() everywhere. */
internal object Timber {

  @SuppressLint("LogNotTimber")
  fun i(message: String) {
    Log.i("IRV", message)
  }
}
