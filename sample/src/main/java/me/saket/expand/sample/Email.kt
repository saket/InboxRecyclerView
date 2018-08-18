package me.saket.expand.sample

data class Email(
    val body: String? = null,
    val recipients: List<Person>,
    val attachments: List<Attachment> = emptyList(),
    val timestamp: String
)
