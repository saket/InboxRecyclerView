package me.saket.expand.sample

sealed class Attachment {

  data class UnsplashImage(val id: String) : Attachment() {
    fun url(width: Int): String = "https://images.unsplash.com/photo-$id?w=$width&q=80"
  }

  object Pdf : Attachment()
}
