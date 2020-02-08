package edu.iu.iustudentgovernment.endpoints

import edu.iu.iustudentgovernment.authentication.getUser
import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.getMap
import edu.iu.iustudentgovernment.handlebars
import edu.iu.iustudentgovernment.utils.render
import spark.Spark.get

fun contact() {
    get("/me") { request, response ->
        val requestUser = request.getUser()
        if (requestUser == null) response.redirect("/representatives")
        else response.redirect("/representatives/${requestUser.username}")
    }

    get("/representatives/:username") { request, response ->
        val requestUser = request.getUser()

        val representative = database.getMember(request.params(":username"))
        if (representative == null) response.redirect("/representatives")
        else {
            val map = request.getMap("${representative.titles} ${representative.name}")
            map["member"] = representative
            map["isThisMember"] =
                requestUser != null && (requestUser.username == representative.username || requestUser.isAdministrator())
            map["editable"] = true

            val votes = database.getVotesContainingMember(representative.username)
                .sortedByDescending { it.start }
                .map { vote -> vote to vote.votes.first { it.username == representative.username }.vote.readable }

            map["votes"] = votes

            handlebars.render(map, "representative.hbs")
        }
    }

    get("/representatives") { request, response ->
        val map = request.getMap("All Members of Congress")
        map["members"] = database.getMembers()

        handlebars.render(map, "representatives.hbs")
    }

    val committees = database.getCommittees()
    committees.forEach { committee ->
        get("/committees/${committee.id}/members") { request, response ->
            val map = request.getMap("${committee.formalName} Committee Members")
            map["members"] = committee.members

            handlebars.render(map, "representatives.hbs")
        }
    }

    get("/contact") { request, response ->
        val map = request.getMap("Contact Representatives")

        val committee = request.queryParams("committee")?.let { database.getCommittee(it) }
        val members =
            if (committee == null) database.getMembers() else database.getCommitteeMembersForCommittee(committee.id)
                .map { it.member }

        map["members"] = members
        map["emailList"] = members.joinToString(" ") { it.email }
        map["emailListSize"] = members.size

        handlebars.render(map, "contact-representatives.hbs")
    }
}
