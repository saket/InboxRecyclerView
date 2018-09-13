package me.saket.inboxrecyclerview.sample.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import kotterknife.bindView
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import me.saket.inboxrecyclerview.PullCollapsibleActivity
import me.saket.inboxrecyclerview.sample.R

class AboutActivity : PullCollapsibleActivity() {

  private val navigationUpButton by bindView<ImageButton>(R.id.about_navigation_up)
  private val bodyTextView by bindView<TextView>(R.id.about_body)
  private val githubLinkView by bindView<View>(R.id.about_github)

  companion object {
    fun intent(context: Context): Intent {
      return Intent(context, AboutActivity::class.java)
    }
  }

  @Suppress("DEPRECATION")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_about)
    expandFromTop()

    navigationUpButton.setOnClickListener {
      finish()
    }

    bodyTextView.text = resources.getText(R.string.about_body)
    BetterLinkMovementMethod.linkifyHtml(bodyTextView)

    githubLinkView.setOnClickListener {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/saket/expand")))
    }
  }
}
