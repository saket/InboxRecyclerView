package me.saket.expand.sample

import androidx.annotation.DrawableRes

sealed class Attachment {

  data class Image(@DrawableRes val drawableRes: Int) : Attachment()

  object Pdf : Attachment()

  object ShippingUpdate : Attachment()

  object CalendarEvent : Attachment()
}
