package me.saket.inboxrecyclerview.sample.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotterknife.bindView
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import me.saket.inboxrecyclerview.PullCollapsibleActivity
import me.saket.inboxrecyclerview.globalVisibleRect
import me.saket.inboxrecyclerview.page.InterceptResult
import me.saket.inboxrecyclerview.sample.R

class AboutActivity : PullCollapsibleActivity() {

  private val navigationUpButton by bindView<ImageButton>(R.id.about_navigation_up)
  private val bodyTextView by bindView<TextView>(R.id.about_body)
  private val githubLinkView by bindView<View>(R.id.about_github)
  private val scrollView by bindView<ScrollView>(R.id.scroll_view)
  private val linearLayout by bindView<LinearLayout>(R.id.linear_layout)

  companion object {
    fun intent(context: Context): Intent {
      return Intent(context, AboutActivity::class.java)
    }
  }

  @Suppress("DEPRECATION")
  override fun onCreate(savedInstanceState: Bundle?) {

    pullToCollapseInterceptor = { downX, downY, upwardPull ->
            if (scrollView.globalVisibleRect().contains(downX, downY).not()) {
                InterceptResult.IGNORED
            }

            val directionInt = if (upwardPull) +1 else -1
            val canScrollFurther = scrollView.canScrollVertically(directionInt)
            when {
                canScrollFurther -> InterceptResult.INTERCEPTED
                else -> InterceptResult.IGNORED
            }
        }

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_about)
    expandFromTop()

    navigationUpButton.setOnClickListener {
      finish()
    }

    bodyTextView.text = resources.getText(R.string.about_body)
    BetterLinkMovementMethod.linkifyHtml(bodyTextView)

    githubLinkView.setOnClickListener {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/saket/inboxrecyclerview")))
    }

      createList()
    }

  private fun createList() {
    val titles = arrayOf("sample","scrollview","with","listview","...","lorem","ipsum","solet","tidor","sid","amet")
    titles.forEach { title->
      linearLayout.addView(createItem(title))
    }
  }

  private fun createItem(title:String): View {
    val layout = LayoutInflater.from(this).inflate(R.layout.list_email_thread, linearLayout, false)
    layout.findViewById<TextView>(R.id.emailthread_item_subject).text = title
    return layout
  }
}
