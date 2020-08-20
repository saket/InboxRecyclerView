package me.saket.inboxrecyclerview.animation

import android.view.View
import android.view.ViewGroup
import java.lang.reflect.Method
import kotlin.LazyThreadSafetyMode.NONE

/**
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-master-dev:transition/transition/src/main/java/androidx/transition/TransitionUtils.java
 * @param forOverlayOf ViewGroup where the view copy be overlayed on.
 */
internal fun View.captureImage(forOverlayOf: ViewGroup): View {
  return Lazy.copyViewImage.invoke(null, forOverlayOf, this, parent as View) as View
}

private object Lazy {
  val copyViewImage: Method by lazy(NONE) {
    val utils = Class.forName("androidx.transition.TransitionUtils")
    utils.getDeclaredMethod(
        "copyViewImage",
        ViewGroup::class.java, View::class.java, View::class.java
    ).apply { isAccessible = true }
  }
}