package edu.iu.iustudentgovernment.models

import edu.iu.iustudentgovernment.database
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

data class Whitcomb(val week: Long, val username: String):Idable {
    val winner get() = database.getMember(username)
    val date get(): String {
        val localDate = Instant.ofEpochMilli(week).atZone(ZoneId.systemDefault()).toLocalDate()
        return "Week of ${localDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${localDate.dayOfMonth}, ${localDate.year}"
    }

    override fun getPermanentId() = week
}

data class Award(val name: String, val id: String, val description: String)