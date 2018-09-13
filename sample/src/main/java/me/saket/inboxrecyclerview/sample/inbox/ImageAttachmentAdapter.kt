package me.saket.inboxrecyclerview.sample.inbox

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import me.saket.inboxrecyclerview.sample.Attachment
import me.saket.inboxrecyclerview.sample.R
import me.saket.inboxrecyclerview.sample.widgets.executeOnMeasure

class ImageAttachmentAdapter(
    private val clickListener: (MotionEvent) -> Boolean
) : RecyclerView.Adapter<ImageViewHolder>() {

  var images: List<Attachment.Image> = emptyList()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
    val imageLayout = LayoutInflater.from(parent.context).inflate(R.layout.list_item_image_attachment, parent, false)
    return ImageViewHolder(imageLayout)
  }

  override fun getItemCount() = images.size

  override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
    holder.render(images[position], clickListener)
  }
}

class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

  fun render(attachment: Attachment.Image, clickListener: (MotionEvent) -> Boolean) {
    val imageView = itemView as ImageView
    val image = ContextCompat.getDrawable(imageView.context, attachment.drawableRes)!!
    imageView.setImageDrawable(image)

    imageView.executeOnMeasure {
      val resizedWidth = image.intrinsicWidth / (image.intrinsicHeight.toFloat() / imageView.height)
      val params = imageView.layoutParams
      params.width = resizedWidth.toInt()
      imageView.layoutParams = params
    }

    itemView.setOnTouchListener { _, event -> clickListener(event) }
  }
}
