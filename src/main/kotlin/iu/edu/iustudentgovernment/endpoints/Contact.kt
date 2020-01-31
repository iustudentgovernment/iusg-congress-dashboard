package iu.edu.iustudentgovernment.endpoints

import iu.edu.iustudentgovernment.authentication.getUser
import iu.edu.iustudentgovernment.database
import iu.edu.iustudentgovernment.getMap
import iu.edu.iustudentgovernment.handlebars
import iu.edu.iustudentgovernment.utils.render
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
