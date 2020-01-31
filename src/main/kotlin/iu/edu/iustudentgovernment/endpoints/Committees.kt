package iu.edu.iustudentgovernment.endpoints

import iu.edu.iustudentgovernment.authentication.Role
import iu.edu.iustudentgovernment.authentication.getUser
import iu.edu.iustudentgovernment.database
import iu.edu.iustudentgovernment.getMap
import iu.edu.iustudentgovernment.handlebars
import iu.edu.iustudentgovernment.models.CommitteeMembership
import iu.edu.iustudentgovernment.utils.render
import spark.Spark.get

fun committees() {
    get("/committees") { request, _ ->
        val map = request.getMap("Committees")

        handlebars.render(map, "committees-home.hbs")
    }

    get("/committees/:committee") { request, response ->
        val committee = request.params(":committee")?.let { database.getCommittee(it) }
        val user = request.getUser()

        if (committee == null) response.redirect("/committees")
        else {
            val map = request.getMap("${committee.formalName} Committee Home")
            map["committee"] = committee

            if (user != null) {
                if (committee.id in user.committeeMemberships.map { it.committeeId }) map["partOfCommittee"] = true
                map["isPrivileged"] = committee.isPrivileged(user)
            }
            handlebars.render(map, "committee.hbs")
        }
    }

    get("/committees/:committee/leave") { request, response ->
        val user = request.getUser()
        val membership = user?.committeeMemberships?.find { it.committeeId == request.params(":committee") }

        if (user != null && membership != null) {
            database.deleteCommitteeMembership(membership.id)
        }

        response.redirect("/committees/${request.params(":committee")}")
    }

    get("/committees/:committee/join") { request, response ->
        val user = request.getUser()
        val membership = user?.committeeMemberships?.find { it.committeeId == request.params(":committee") }
        val committee = database.getCommittee(request.params(":committee"))

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

        response.redirect("/committees/${request.params(":committee")}")
    }
}