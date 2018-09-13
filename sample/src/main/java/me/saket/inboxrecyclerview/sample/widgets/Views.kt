package me.saket.inboxrecyclerview.sample.widgets

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

fun TextView.setDrawableStart(@DrawableRes drawableRes: Int) {
  setDrawableStart(ContextCompat.getDrawable(context, drawableRes))
}

fun TextView.setDrawableStart(drawable: Drawable?) {
  setCompoundDrawablesRelativeWithIntrinsicBounds(
      drawable,
      compoundDrawablesRelative[1],
      compoundDrawablesRelative[2],
      compoundDrawablesRelative[3]
  )
}

/**
 * Execute a runnable when a [view]'s dimensions get measured and is laid out on the screen.
 */
@SuppressLint("LogNotTimber")
fun View.executeOnMeasure(listener: () -> Unit) {
  if (isInEditMode || isLaidOut) {
    listener()
    return
  }

  viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
    override fun onPreDraw(): Boolean {
      if (isLaidOut) {
        viewTreeObserver.removeOnPreDrawListener(this)
        listener()

      } else if (visibility == View.GONE) {
        Log.w("Views", "View's visibility is set to Gone. It'll never be measured: ${resources.getResourceEntryName(id)}")
        viewTreeObserver.removeOnPreDrawListener(this)
      }
      return true
    }
  })
}
