package me.saket.expand.sample.email

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.functions.Consumer
import me.saket.expand.sample.EmailRepository
import me.saket.expand.sample.EmailThread
import me.saket.expand.sample.EmailThreadId
import me.saket.expand.sample.R

class EmailThreadFragment : Fragment(), Consumer<EmailThreadId> {

  private val subjectTextView by lazy { view!!.findViewById<TextView>(R.id.emailthread_subject) }
  private val byline1TextView by lazy { view!!.findViewById<TextView>(R.id.emailthread_byline1) }
  private val byline2TextView by lazy { view!!.findViewById<TextView>(R.id.emailthread_byline2) }
  private val bodyTextView by lazy { view!!.findViewById<TextView>(R.id.emailthread_body) }
  private val collapseButton by lazy { view!!.findViewById<ImageButton>(R.id.emailthread_collapse) }

  private val threadIds = BehaviorRelay.create<EmailThreadId>()
  private val onDestroys = PublishRelay.create<Any>()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_email_thread, container, false)
  }

  override fun onViewCreated(view: View, savedState: Bundle?) {
    super.onViewCreated(view, savedState)

    if (savedState != null) {
      onRestoreInstanceState(savedState)
    }

    threadIds
        .map { EmailRepository.thread(id = it) }
        .takeUntil(onDestroys)
        .subscribe { render(it) }

    collapseButton.setOnClickListener {
      requireActivity().onBackPressed()
    }
  }

  override fun onDestroyView() {
    onDestroys.accept(Any())
    super.onDestroyView()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    if (threadIds.hasValue()) {
      outState.putLong("thread_id", threadIds.value)
    }
    super.onSaveInstanceState(outState)
  }

  private fun onRestoreInstanceState(savedState: Bundle) {
    val retainedThreadId: Long? = savedState.getLong("thread_id")
    if (retainedThreadId != null) {
      threadIds.accept(retainedThreadId)
    }
  }

  override fun accept(threadId: EmailThreadId) {
    threadIds.accept(threadId)
  }

  @SuppressLint("SetTextI18n")
  private fun render(emailThread: EmailThread) {
    val latestEmail = emailThread.emails.last()

    subjectTextView.text = emailThread.subject
    byline1TextView.text = "${emailThread.sender.name} — ${latestEmail.timestamp}"

    val cmvRecipients = latestEmail.recipients
        .dropLast(1)
        .joinToString(transform = { it.name })
        .plus(" and ${latestEmail.recipients.last().name}")
    byline2TextView.text = "To $cmvRecipients"

    bodyTextView.text = latestEmail.body
  }
}
