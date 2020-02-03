package edu.iu.iustudentgovernment.models

import edu.iu.iustudentgovernment.authentication.Member
import edu.iu.iustudentgovernment.authentication.Role
import edu.iu.iustudentgovernment.database
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

data class Committee(
    val formalName: String,
    val id: String,
    var chairUsername: String,
    val permissionLevelForEntry: Int,
    var descriptionId: String = "committee_description_$id"
) :Idable{
    override fun getPermanentId() = id

    fun isPrivileged(user: Member) =
        chairUsername == user.username || user.isAdministrator() || database.getCommitteeMembershipsForMember(user.username).first { it.committeeId == id }
            .let { membership ->
                membership.role != Role.MEMBER
            }

    val chair get() = database.getMember(chairUsername)!!
    val members get() = database.getCommitteeMembersForCommittee(id).map { it.member }
    val membersString get() = members.joinToString(", ") { it.asLink() }
    val upcomingMeetings get() = database.getFutureMeetings().filter { it.committeeId == id }.take(3)
    val pastMeetings get() = database.getPastMeetings().filter { it.committeeId == id }.take(3)
    val activeLegislation get() = database.getActiveLegislation().filter { it.committeeId == id }
    val enactedLegislation get() = database.getEnactedLegislation().filter { it.committeeId == id }
    val failedLegislation get() = database.getFailedLegislation().filter { it.committeeId == id }
    val asLink get() = "<a href='/committees/$id'>$formalName</a>"
    val description get() = database.getMessage(descriptionId)!!.value.toString()
}

data class Meeting(
    val name: String,
    val meetingId: String,
    val time: Long,
    val location: String,
    val committeeId: String,
    var agendaFileUrl: String?,
    val ledBy: List<String>,
    val notes: List<Note>,
    val minutesUrl: String? = null
) :Idable {
    val date
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("hh:mm, MM/dd/YYYY"))

    val committee get() = database.getCommittee(committeeId)
    val ledByString get() = ledBy.filter { it.isNotBlank() }.joinToString(", ") { database.getMember(it)!!.asLink }
    val notesString get() = notes.joinToString("\n") {it.text}
    val ledByNoLinkString get() = ledBy.filter { it.isNotBlank() }.joinToString(", ")
    val jsTime get() = SimpleDateFormat("yyyy-MM-dd'T'hh:mm").format(Date.from(Instant.ofEpochMilli(time)))

    override fun getPermanentId() = meetingId

}

data class CommitteeMembership(val username: String, val id: String, val committeeId: String, val role: Role): Idable {
    val committee get() = database.getCommittee(committeeId)!!
    val member get() = database.getMember(username)!!

    override fun getPermanentId() = id
}