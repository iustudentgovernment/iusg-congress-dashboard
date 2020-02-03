package edu.iu.iustudentgovernment.endpoints

import edu.iu.iustudentgovernment.authentication.Member
import edu.iu.iustudentgovernment.authentication.getUser
import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.getMap
import edu.iu.iustudentgovernment.handlebars
import edu.iu.iustudentgovernment.models.Whitcomb
import edu.iu.iustudentgovernment.utils.nullifyEmpty
import edu.iu.iustudentgovernment.utils.render
import spark.Spark.get
import spark.Spark.path
import java.util.*

fun administration() {
    path("/administration") {
        get("/messages") { request, response ->
            val user = request.getUser()

            if (user?.steering == true) {
                val map = request.getMap("Manage Messages")
                map["speaker-message"] = database.getSpeakerMessage()
                map["whitcomb-award-description"] = database.getWhitcombDescription()

                handlebars.render(map, "edit-messages.hbs")
            } else response.redirect("/")
        }

        get("/members") { request, response ->
            val user = request.getUser()

            if (user?.isAdministrator() == true) {
                val map = request.getMap("Manage Members")
                map["members"] = database.getMembers()
                map["chairs"] = database.getCommittees().map { it.chair to it }

                handlebars.render(map, "administrate-members.hbs")
            } else response.redirect("/")
        }

        get("/remove-member") { request, response ->
            val user = request.getUser()

            if (user?.isAdministrator() == true) {
                val username = request.queryParams("remove-member")
                database.deleteMember(username)
                response.redirect("/administration/members")
            } else response.redirect("/")
        }

        get("/set-chair/:committeeId") { request, response ->
            val user = request.getUser()
            val committee = database.getCommittee(request.params(":committeeId"))

            if (committee != null && user?.isAdministrator() == true) {
                val username = request.queryParams("set-chair")
                committee.chairUsername = username
                database.updateCommittee(committee)

                response.redirect("/administration/members")
            } else response.redirect("/")
        }

        get("/add-member") { request, response ->
            val user = request.getUser()
            val username = request.queryParams("username")?.nullifyEmpty()?.toLowerCase()
            val name = request.queryParams("name")?.nullifyEmpty()
            val phone = request.queryParams("phone")?.nullifyEmpty()
            val email = request.queryParams("email")?.nullifyEmpty()
            val constituency = request.queryParams("constituency")?.nullifyEmpty()

            if (user?.isAdministrator() == true && username != null && name != null && phone != null && email != null && constituency != null) {
                val newMember = Member(username, constituency, name, email, phone, mutableListOf())
                database.insertMember(newMember)

                response.redirect("/representatives/$username")
            } else response.redirect("/administration/members")
        }

        get("/awards-add-whitcomb") { request, response ->
            val user = request.getUser()
            val awardee = request.queryParams("username")?.nullifyEmpty()?.toLowerCase()?.let { database.getMember(it) }

            if (user?.isAdministrator() == true && awardee != null) {
                val calendar = GregorianCalendar.getInstance() as GregorianCalendar
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                val time = calendar.timeInMillis

                val award = Whitcomb(time, awardee.username)
                database.insertWhitcombAward(award)
            }
            response.redirect("/administration/members")
        }
    }

    get("/submit-edit-messages") { request, response ->
        val user = request.getUser()

        if (user?.steering == true) {
            val speakerMessage = request.queryParams("speaker_message")
            val whitcombDescription = request.queryParams("whitcomb_description")
            val committeeDescriptions = request.queryParams().filter { it.startsWith("committee_description") }
                .map { it to request.queryParams(it) }

            database.updateMessage("speaker_message", speakerMessage)
            database.updateMessage("whitcomb_description", whitcombDescription)

            committeeDescriptions.forEach { (id, value) ->
                database.updateMessage(id, value)
            }

            response.redirect("/me")
        } else response.redirect("/")

    }
}