package me.saket.inboxrecyclerview.sample

import androidx.recyclerview.widget.DiffUtil

typealias EmailThreadId = Long

data class EmailThread(
    val id: EmailThreadId,
    val sender: Person,
    val subject: String,
    val emails: List<Email>
) {

  class ItemDiffer : DiffUtil.ItemCallback<EmailThread>() {
    override fun areItemsTheSame(oldItem: EmailThread, newItem: EmailThread) = oldItem.subject == newItem.subject
    override fun areContentsTheSame(oldItem: EmailThread, newItem: EmailThread) = oldItem == newItem
  }
}
