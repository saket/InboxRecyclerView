package me.saket.expand.sample

object EmailRepository {

  fun threads(): List<EmailThread> {
    val user = Person("me")

    return listOf(
        EmailThread(
            sender = Person("Google Express"),
            subject = "Package shipped!",
            emails = listOf(
                Email(
                    body = "Cucumber Mask Facial has shipped",
                    recipients = listOf(user),
                    attachments = listOf(Attachment.Pdf),
                    timestamp = "15 mins ago"))
        ),

        EmailThread(
            sender = Person("Ali Connors"),
            subject = "Brunch this weekend?",
            emails = listOf(
                Email(
                    body = "I'll be in your neighburhood doing errands",
                    recipients = listOf(user),
                    timestamp = "25 mins ago"))
        ),

        EmailThread(
            sender = Person("Sandra Adams"),
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
            sender = Person("Trevor Hansen"),
            subject = "High school reunion?",
            emails = listOf(
                Email(
                    recipients = listOf(
                        user,
                        Person("Rachel"),
                        Person("Zach")),
                    timestamp = "12 hrs ago"))
        ),

        EmailThread(
            sender = Person("Mom"),
            subject = "Visiting Town Next Thursday",
            emails = listOf(
                Email(
                    body = "Hey there, I wanted to let you know I'll be visiting Wakanda on 31st August.",
                    recipients = listOf(user),
                    timestamp = "16 hrs ago"))
        ),

        EmailThread(
            sender = Person("Mom"),
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
}
