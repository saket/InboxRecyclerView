package me.saket.expand.sample

data class Email(
    val body: String,
    val showBodyInThreads: Boolean = true,
    val recipients: List<Person>,
    val attachments: List<Attachment> = emptyList(),
    val timestamp: String
) {

  val hasImageAttachments = attachments.any { it is Attachment.Image }

  val hasNonImageAttachments = attachments.any { it is Attachment.Pdf }
}
