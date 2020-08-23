package edu.iu.iustudentgovernment.controllers

import edu.iu.iustudentgovernment.authentication.getUser
import edu.iu.iustudentgovernment.data.getMap
import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.http.HandlebarsContent
import edu.iu.iustudentgovernment.http.respondHbs
import io.ktor.application.call
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.contactRoutes() {
    get("/me") {
        val requestUser = call.getUser()
        if (requestUser == null) call.respondRedirect("/representatives")
        else call.respondRedirect("/representatives/${requestUser.username}")
    }

    get("/representatives/{username}") {
        val requestUser = call.getUser()

        val representative = database.getMember(call.parameters["username"]!!)
        if (representative == null) call.respondRedirect("/representatives")
        else {
            val map = getMap(call, "${representative.titles} ${representative.name}")
            map["member"] = representative
            map["isThisMember"] =
                requestUser != null && (requestUser.username == representative.username || requestUser.isAdministrator())
            map["editable"] = true

            val votes = database.getVotesContainingMember(representative.username)
                .sortedByDescending { it.start }
                .map { vote -> vote to vote.votes.first { it.username == representative.username }.vote.readable }

            map["votes"] = votes

            call.respondHbs(HandlebarsContent("representative.hbs", map))
        }
    }

    get("/representatives") {
        val map = getMap(call, "All Members of Congress")
        map["members"] = database.getMembers()

        call.respondHbs(HandlebarsContent("representatives.hbs", map))
    }

    val committees = database.getCommittees()
    committees.forEach { committee ->
        get("/committees/${committee.id}/members") {
            val map = getMap(call, "${committee.formalName} Committee Members")
            map["members"] = committee.members

            call.respondHbs(HandlebarsContent("representatives.hbs", map))
        }
    }

    get("/contact") {
        val map = getMap(call, "Contact Representatives")

        val committee = call.request.queryParameters["committee"]?.let { database.getCommittee(it) }
        val members =
            if (committee == null) database.getMembers() else database.getCommitteeMembersForCommittee(committee.id)
                .map { it.member }

        map["members"] = members
        map["emailList"] = members.joinToString(" ") { it.email }
        map["emailListSize"] = members.size

        call.respondHbs(HandlebarsContent("contact-representatives.hbs", map))
    }
}
