package me.saket.expand.sample

object EmailRepository {

  fun threads(): List<EmailThread> {
    val user = Person("me")

    return listOf(
        EmailThread(
            id = 0L,
            sender = Person("Google Express", R.drawable.avatar_googleexpress),
            subject = "Package shipped!",
            emails = listOf(
                Email(
                    body = "Cucumber Mask Facial has shipped",
                    recipients = listOf(user),
                    attachments = listOf(Attachment.Pdf),
                    timestamp = "15 mins ago"))
        ),

        EmailThread(
            id = 1L,
            sender = Person("Ali Connors", R.drawable.avatar_ali_connors),
            subject = "Brunch this weekend?",
            emails = listOf(
                Email(
                    body = "I'll be in your neighburhood doing errands",
                    recipients = listOf(user),
                    timestamp = "25 mins ago"))
        ),

        EmailThread(
            id = 2L,
            sender = Person("Sandra Adams", R.drawable.avatar_sandra),
            subject = "Bonjour from Paris",
            emails = listOf(
                Email(
                    body = "Here are some great shots from my trip",
                    attachments = listOf(
                        Attachment.UnsplashImage(id = "1476224203421-9ac39bcb3327"),
                        Attachment.UnsplashImage(id = "1508247687564-2289326346a2"),
                        // Eiffel Tower: 1527142253992-b076b9d7e90b, 1533294631096-58bc8d971a68, 1528989791086-a13167c02428
                        Attachment.UnsplashImage(id = "1527142253992-b076b9d7e90b"),
                        Attachment.UnsplashImage(id = "1513870931673-fa0ab3de2e09")
                    ),
                    recipients = listOf(user),
                    timestamp = "6 hrs ago"))
        ),

        EmailThread(
            id = 3L,
            sender = Person("Trevor Hansen", R.drawable.avatar_trevor),
            subject = "High school reunion?",
            emails = listOf(
                Email(
                    body = """
                      Hi friends,

                      I was at the grocery store on Sunday
                      night... when I ran into Genie Williams!
                      I almost didn't recognize her after 20 years!

                      Anyway, it turns out she is on
                      the organizing committee for the high school
                      reuinion this fall. I don't know if you were
                      planning on going or not, but she could
                      definitely use our help in trying to track
                      down lots of missing alums. If you can make
                      it, we're doing a llittle phone-tree party
                      at her place next Saturday, hoping that if
                      we can find one person, a few more will
                      emerge. What do you say?
                    """.trimIndent(),
                    showBodyInThreads = false,
                    recipients = listOf(
                        user,
                        Person("Rachel"),
                        Person("Zach")),
                    timestamp = "12 hrs ago"))
        ),

        EmailThread(
            id = 4L,
            sender = Person("Jerry Chang", R.drawable.avatar_jerry_chang),
            subject = "Visiting Town Next Thursday",
            emails = listOf(
                Email(
                    body = "Hey there, I wanted to let you know I'll be visiting Wakanda on 31st August.",
                    recipients = listOf(user),
                    timestamp = "16 hrs ago"))
        ),

        EmailThread(
            id = 5L,
            sender = Person("Mom", R.drawable.avatar_mom),
            subject = "Fwd: Article on Workplace Zen",
            emails = listOf(
                Email(
                    body = "Hi sweetie, I saw this and thought you would find this useful.",
                    recipients = listOf(user),
                    attachments = listOf(Attachment.Pdf),
                    timestamp = "Yesterday"))
        )
    )
  }

  fun thread(id: EmailThreadId): EmailThread {
    return threads().first { it.id == id }
  }
}
