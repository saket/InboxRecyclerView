package me.saket.expand.sample.inbox

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.saket.expand.sample.EmailThread
import me.saket.expand.sample.R

class ThreadsAdapter : ListAdapter<EmailThread, EmailViewHolder>(EmailThread.ItemDiffer()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmailViewHolder {
    val threadLayout = LayoutInflater.from(parent.context).inflate(R.layout.list_email_thread, parent, false)
    return EmailViewHolder(threadLayout)
  }

  override fun onBindViewHolder(holder: EmailViewHolder, position: Int) {
    holder.render(getItem(position))
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }
}

class EmailViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {

  fun render(item: EmailThread) {

  }
}
