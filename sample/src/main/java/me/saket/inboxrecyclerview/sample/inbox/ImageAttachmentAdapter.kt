package me.saket.inboxrecyclerview.sample.inbox

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.RecyclerView
import me.saket.inboxrecyclerview.sample.Attachment
import me.saket.inboxrecyclerview.sample.R

class ImageAttachmentAdapter(
    private val touchListener: (MotionEvent) -> Boolean
) : RecyclerView.Adapter<ImageViewHolder>() {

  var images: List<Attachment.Image> = emptyList()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
    val imageLayout = LayoutInflater.from(parent.context).inflate(R.layout.list_item_image_attachment, parent, false)
    return ImageViewHolder(imageLayout)
  }

  override fun getItemCount() = images.size

  override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
    holder.render(images[position], touchListener)
  }
}

@SuppressLint("ClickableViewAccessibility")
class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
  fun render(attachment: Attachment.Image, touchListener: (MotionEvent) -> Boolean) {
    val imageView = itemView as ImageView
    val image = ContextCompat.getDrawable(imageView.context, attachment.drawableRes)!!
    imageView.setImageDrawable(image)

    imageView.alpha = 0f
    imageView.animate().alpha(1f).setDuration(150)

    imageView.post {
      val resizedWidth = image.intrinsicWidth / (image.intrinsicHeight.toFloat() / imageView.height)
      val params = imageView.layoutParams
      params.width = resizedWidth.toInt()
      imageView.layoutParams = params
    }

    itemView.setOnTouchListener { _, event -> touchListener(event) }
  }
}
