package me.saket.expand.sample.inbox

import android.annotation.SuppressLint
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotterknife.bindView
import me.saket.expand.sample.EmailThread
import me.saket.expand.sample.R

class ThreadsAdapter(
    private val clickListener: (EmailThread) -> Unit
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
    clickListener: (EmailThread) -> Unit
) : RecyclerView.ViewHolder(itemView) {

  private val bylineTextView by bindView<TextView>(R.id.emailthread_byline)
  private val subjectTextView by bindView<TextView>(R.id.emailthread_subject)
  private val bodyTextView by bindView<TextView>(R.id.emailthread_body)

  lateinit var emailThread: EmailThread

  init {
    itemView.setOnClickListener { clickListener(emailThread) }
  }

  @SuppressLint("SetTextI18n")
  fun render() {
    val latestEmail = emailThread.emails.last()
    bylineTextView.text = "${emailThread.sender.name} \u2014 ${latestEmail.timestamp}"

    subjectTextView.text = emailThread.subject
    bodyTextView.text = latestEmail.body
    bodyTextView.visibility = if (latestEmail.body.isNullOrBlank()) View.GONE else View.VISIBLE
  }
}
