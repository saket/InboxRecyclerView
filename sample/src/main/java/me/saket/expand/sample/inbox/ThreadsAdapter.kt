package me.saket.expand.sample.inbox

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.saket.expand.sample.EmailThread
import me.saket.expand.sample.R

typealias ItemId = Long
typealias ItemPosition = Int
typealias ItemClickListener = (EmailThread, ItemPosition, ItemId) -> Unit

class ThreadsAdapter(
    private val clickListener: ItemClickListener
) : ListAdapter<EmailThread, EmailViewHolder>(EmailThread.ItemDiffer()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmailViewHolder {
    val threadLayout = LayoutInflater.from(parent.context).inflate(R.layout.list_email_thread, parent, false)
    return EmailViewHolder(threadLayout, clickListener)
  }

  override fun onBindViewHolder(holder: EmailViewHolder, position: Int) {
    holder.emailThread = getItem(position)
    holder.render()
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }
}

class EmailViewHolder(
    itemView: View,
    clickListener: ItemClickListener
) : RecyclerView.ViewHolder(itemView) {

  private val bylineTextView = itemView.findViewById<TextView>(R.id.emailthread_byline)
  private val subjectTextView = itemView.findViewById<TextView>(R.id.emailthread_subject)
  private val bodyTextView = itemView.findViewById<TextView>(R.id.emailthread_body)

  lateinit var emailThread: EmailThread

  init {
    itemView.setOnClickListener {
      clickListener(emailThread, adapterPosition, itemId)
    }
  }

  @SuppressLint("SetTextI18n")
  fun render() {
    val latestEmail = emailThread.emails.last()
    bylineTextView.text = "${emailThread.sender.name} \u2014 ${latestEmail.timestamp}"

    subjectTextView.text = emailThread.subject
    val subjectTextSize = subjectTextView.resources.getDimensionPixelSize(when {
      latestEmail.hasImageAttachments -> R.dimen.emailthread_subject_textize_with_photo_attachments
      else -> R.dimen.emailthread_subject_textize_without_photo_attachments
    })
    subjectTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, subjectTextSize.toFloat())

    bodyTextView.text = latestEmail.body
    bodyTextView.visibility = if (latestEmail.body.isNullOrBlank()) View.GONE else View.VISIBLE
  }
}
