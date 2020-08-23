package edu.iu.iustudentgovernment.controllers

import edu.iu.iustudentgovernment.authentication.Role
import edu.iu.iustudentgovernment.authentication.getUser
import edu.iu.iustudentgovernment.data.getMap
import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.http.HandlebarsContent
import edu.iu.iustudentgovernment.http.respondHbs
import edu.iu.iustudentgovernment.models.CommitteeMembership
import io.ktor.application.call
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.committeesRoutes() {
    get("/committees") {
        val map = getMap(call, "Committees")

        call.respondHbs(HandlebarsContent("committees-home.hbs", map))
    }

    get("/committees/{committee}") {
        val committee = call.parameters["committee"]?.let { database.getCommittee(it) }
        val user = call.getUser()

        if (committee == null) call.respondRedirect("/committees")
        else {
            val map = getMap(call, "${committee.formalName} Committee Home")
            map["committee"] = committee

            if (user != null) {
                if (committee.id in user.committeeMemberships.map { it.committeeId }) map["partOfCommittee"] = true
                map["isPrivileged"] = committee.isPrivileged(user)
            }

            call.respondHbs(HandlebarsContent("committee.hbs", map))
        }
    }

    get("/committees/{committee}/leave") {
        val user = call.getUser()
        val membership = user?.committeeMemberships?.find { it.committeeId == call.parameters["committee"] }

        if (user != null && membership != null) {
            database.deleteCommitteeMembership(membership.id)
        }

        call.respondRedirect("/committees/${call.parameters["committee"]}")
    }

    get("/committees/{committee}/join") {
        val user = call.getUser()
        val membership = user?.committeeMemberships?.find { it.committeeId == call.parameters["committee"] }
        val committee = database.getCommittee(call.parameters["committee"]!!)

        if (committee != null && user != null && membership == null) {
            database.insertCommitteeMembership(
                CommitteeMembership(
                    user.username,
                    database.getUuid(),
                    committee.id,
                    Role.MEMBER
                )
            )
        }

        call.respondRedirect("/committees/${call.parameters["committee"]}")
    }
}