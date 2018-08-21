package me.saket.expand.sample.widgets

import android.graphics.drawable.AnimatedVectorDrawable
import java.lang.reflect.Method

class ReversibleAnimatedVectorDrawable(private val drawable: AnimatedVectorDrawable) {

  private val reverseMethod: Method by lazy {
    drawable.javaClass.getMethod("reverse")
  }

  fun play() {
    drawable.start()
  }

  fun reverse() {
    reverseMethod.invoke(drawable)
  }
}
