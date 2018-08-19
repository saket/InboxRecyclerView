package me.saket.expand.sample.inbox

import android.graphics.drawable.Drawable
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
