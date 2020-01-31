package edu.iu.iustudentgovernment.models

import edu.iu.iustudentgovernment.database
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class Statement(
    val id: String,
    val createdAt: Long,
    val createdByUsername: String,
    val lastEditTime: Long?,
    val lastEditedByUsername: String?,
    val title: String,
    val paragraphs: List<Paragraph>
) {
    val author get() = database.getMember(createdByUsername)!!
    val date get() = createdAt.getAsDate()

    val lastEditDate get() = lastEditTime?.getAsDate()
    val lastEditAuthor get() = lastEditedByUsername?.let { database.getMember(it) }
}

data class Paragraph(val text: String)

private fun Long.getAsDate() = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
    .format(DateTimeFormatter.ISO_LOCAL_DATE)