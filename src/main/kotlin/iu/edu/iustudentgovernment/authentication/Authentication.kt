package iu.edu.iustudentgovernment.authentication

import iu.edu.iustudentgovernment.database
import spark.Request

enum class Role(val readable: String){
    MEMBER("Member"), PRIVILEGED_MEMBER("Member"), COMMITTEE_CHAIR("Chair")
}

enum class Title(val readable: String, val rank: Int, val note: String? = null) {
    MEMBER("Member",0), COMMITTEE_CHAIR("Committee Chair", 1),
    PARLIAMENTARIAN("Parliamentarian", 2),
    GRAMMARIAN("Grammarian", 2),
    PRESS_SECRETARY("Press Secretary", 2),
    SPEAKER("Speaker", 3),
    SITE_ADMINISTRATOR("Site Administrator", 4)
}

data class Member(
    val username: String,
    val constituency: String,
    val name: String,
    val email: String,
    val phoneNumber: String?,
    val title: List<Title>,
    val bio: String? = null,
    val active: Boolean = true
) {
    val titles get() = title.sortedBy { it.rank }.joinToString(", ") { it.readable }
    val committeeMemberships get() = database.getCommitteeMembershipsForMember(username)
    val readableCommitteeMemberships get() = committeeMemberships.joinToString(", ") {
        "${it.role.readable} of <a href='/committees/${it.committee.id}'>${it.committee.formalName} Committee</a>"
    }

    val asLink get() = asLink()

    fun isAdministrator() = title.map { it.rank }.max()!! >= 3
    fun asLink() = "<a href='/representatives/$username'>$name</a>"
}

fun Request.getUser() = session().attribute<String?>("user")?.let { database.getMember(it) }

fun List<Title>.hasTitlePermission(title: Title): Boolean = maxBy { it.rank }!!.rank >= title.rank