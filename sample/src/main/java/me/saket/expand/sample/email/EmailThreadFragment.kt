package me.saket.expand.sample.email

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import me.saket.expand.page.ExpandablePageLayout
import me.saket.expand.page.InterceptResult
import me.saket.expand.page.SimplePageStateChangeCallbacks
import me.saket.expand.sample.Attachment.CalendarEvent
import me.saket.expand.sample.Attachment.Image
import me.saket.expand.sample.Attachment.Pdf
import me.saket.expand.sample.Attachment.ShippingUpdate
import me.saket.expand.sample.EmailRepository
import me.saket.expand.sample.EmailThread
import me.saket.expand.sample.EmailThreadId
import me.saket.expand.sample.R
import me.saket.expand.sample.exhaustive

class EmailThreadFragment : Fragment() {

  private val emailThreadPage by lazy { view!!.parent as ExpandablePageLayout }
  private val scrollableContainer by lazy { view!!.findViewById<ScrollView>(R.id.emailthread_scrollable_container) }
  private val subjectTextView by lazy { view!!.findViewById<TextView>(R.id.emailthread_subject) }
  private val byline1TextView by lazy { view!!.findViewById<TextView>(R.id.emailthread_byline1) }
  private val byline2TextView by lazy { view!!.findViewById<TextView>(R.id.emailthread_byline2) }
  private val avatarImageView by lazy { view!!.findViewById<ImageView>(R.id.emailthread_avatar) }
  private val bodyTextView by lazy { view!!.findViewById<TextView>(R.id.emailthread_body) }
  private val collapseButton by lazy { view!!.findViewById<ImageButton>(R.id.emailthread_collapse) }
  private val attachmentContainer by lazy { view!!.findViewById<ViewGroup>(R.id.emailthread_attachment_container) }

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

    emailThreadPage.pullToCollapseInterceptor = { downX, downY, upwardPull ->
      val directionInt = if (upwardPull) +1 else -1
      val canScrollFurther = scrollableContainer.canScrollVertically(directionInt)
      when {
        canScrollFurther -> InterceptResult.INTERCEPTED
        else -> InterceptResult.IGNORED
      }
    }

    emailThreadPage.addStateChangeCallbacks(object : SimplePageStateChangeCallbacks() {
      override fun onPageCollapsed() {
        scrollableContainer.scrollTo(0, 0)
      }
    })
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

  fun populate(threadId: EmailThreadId) {
    threadIds.accept(threadId)
  }

  @SuppressLint("SetTextI18n")
  private fun render(emailThread: EmailThread) {
    val latestEmail = emailThread.emails.last()

    subjectTextView.text = emailThread.subject
    byline1TextView.text = "${emailThread.sender.name} — ${latestEmail.timestamp}"

    val cmvRecipients = if (latestEmail.recipients.size > 1) {
      latestEmail.recipients
          .dropLast(1)
          .joinToString(transform = { it.name })
          .plus(" and ${latestEmail.recipients.last().name}")
    } else {
      latestEmail.recipients[0].name
    }
    byline2TextView.text = "To $cmvRecipients"

    bodyTextView.text = latestEmail.body
    avatarImageView.setImageResource(emailThread.sender.profileImageRes!!)

    renderAttachments(emailThread)
  }

  private fun renderAttachments(thread: EmailThread) {
    attachmentContainer.removeAllViews()

    val attachments = thread.emails.last().attachments

    val attachment = attachments.firstOrNull()
    when (attachment) {
      is Image -> renderImageAttachments()
      is Pdf -> renderPdfAttachment(attachment)
      is CalendarEvent -> renderCalendarEvent(attachment)
      is ShippingUpdate -> renderShippingUpdate(attachment)
      null -> {
      }
    }.exhaustive()
  }

  private fun renderImageAttachments() {
    View.inflate(context, R.layout.include_email_image_gallery, attachmentContainer)
  }

  private fun renderPdfAttachment(attachment: Pdf) {
    View.inflate(context, R.layout.include_email_pdf_attachment, attachmentContainer)
  }

  private fun renderCalendarEvent(attachment: CalendarEvent) {
    View.inflate(context, R.layout.include_email_calendar_invite, attachmentContainer)
  }

  private fun renderShippingUpdate(attachment: ShippingUpdate) {
    View.inflate(context, R.layout.include_email_shipping_update, attachmentContainer)
  }
}
