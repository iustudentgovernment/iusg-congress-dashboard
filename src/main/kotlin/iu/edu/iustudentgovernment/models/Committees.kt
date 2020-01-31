package iu.edu.iustudentgovernment.models

import iu.edu.iustudentgovernment.authentication.Member
import iu.edu.iustudentgovernment.authentication.Role
import iu.edu.iustudentgovernment.database
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

data class Committee(
    val formalName: String,
    val id: String,
    val chairUsername: String,
    val permissionLevelForEntry: Int,
    val description: String
) {
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
) {
    val date
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("hh:mm, MM/dd/YYYY"))

    val committee get() = database.getCommittee(committeeId)
    val ledByString get() = ledBy.joinToString(", ") { database.getMember(it)!!.asLink }
    val notesString get() = notes.joinToString("\n") {it.text}
    val ledByNoLinkString get() = ledBy.joinToString(", ")
    val jsTime get() = SimpleDateFormat("yyyy-MM-dd'T'hh:mm").format(Date.from(Instant.ofEpochMilli(time)))
}

data class CommitteeMembership(val username: String, val id: String, val committeeId: String, val role: Role) {
    val committee get() = database.getCommittee(committeeId)!!
    val member get() = database.getMember(username)!!
}