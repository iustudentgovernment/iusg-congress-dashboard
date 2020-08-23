package edu.iu.iustudentgovernment.controllers

import edu.iu.iustudentgovernment.authentication.Member
import edu.iu.iustudentgovernment.authentication.getUser
import edu.iu.iustudentgovernment.data.getMap
import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.http.HandlebarsContent
import edu.iu.iustudentgovernment.http.respondHbs
import edu.iu.iustudentgovernment.models.Whitcomb
import edu.iu.iustudentgovernment.utils.nullifyEmpty
import io.ktor.application.call
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.util.filter
import io.ktor.util.toMap
import java.util.Calendar
import java.util.GregorianCalendar

fun Route.administrationRoutes() {
    route("/administration") {
        get("/messages") {
            val user = call.getUser()

            if (user?.steering == true) {
                val map = getMap(call, "Manage Messages")
                map["speaker-message"] = database.getSpeakerMessage()
                map["whitcomb-award-description"] = database.getWhitcombDescription()

                call.respondHbs(HandlebarsContent("edit-messages.hbs", map))
            } else call.respondRedirect("/")
        }

        get("/members") {
            val user = call.getUser()

            if (user?.isAdministrator() == true) {
                val map = getMap(call, "Manage Members")
                map["members"] = database.getMembers()
                map["chairs"] = database.getCommittees().map { it.chair to it }

                call.respondHbs(HandlebarsContent("administrate-members.hbs", map))
            } else call.respondRedirect("/")
        }

        get("/remove-member") {
            val user = call.getUser()

            if (user?.isAdministrator() == true) {
                val username = call.request.queryParameters["remove-member"]!!
                database.deleteMember(username)
                call.respondRedirect("/administration/members")
            } else call.respondRedirect("/")
        }

        get("/set-chair/{committeeId}") {
            val user = call.getUser()
            val committee = database.getCommittee(call.parameters["committeeId"]!!)

            if (committee != null && user?.isAdministrator() == true) {
                val username = call.request.queryParameters["set-chair"]!!
                committee.chairUsername = username
                database.updateCommittee(committee)

                call.respondRedirect("/administration/members")
            } else call.respondRedirect("/")
        }

        get("/add-member") {
            val user = call.getUser()
            val username = call.request.queryParameters["username"]?.nullifyEmpty()?.toLowerCase()
            val name = call.request.queryParameters["name"]?.nullifyEmpty()
            val phone = call.request.queryParameters["phone"]?.nullifyEmpty()
            val email = call.request.queryParameters["email"]?.nullifyEmpty()
            val constituency = call.request.queryParameters["constituency"]?.nullifyEmpty()

            if (user?.isAdministrator() == true && username != null && name != null && phone != null && email != null && constituency != null) {
                val newMember = Member(username, constituency, name, email, phone, mutableListOf())
                database.insertMember(newMember)

                call.respondRedirect("/representatives/$username")
            } else call.respondRedirect("/administration/members")
        }

        get("/awards-add-whitcomb") {
            val user = call.getUser()
            val awardee = call.request.queryParameters["username"]?.nullifyEmpty()?.toLowerCase()?.let { database.getMember(it) }

            if (user?.isAdministrator() == true && awardee != null) {
                val calendar = GregorianCalendar.getInstance() as GregorianCalendar
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                val time = calendar.timeInMillis

                val award = Whitcomb(time, awardee.username)
                database.insertWhitcombAward(award)
            }

            call.respondRedirect("/administration/members")
        }
    }

    get("/submit-edit-messages") {
        val user = call.getUser()

        if (user?.steering == true) {
            val speakerMessage = call.request.queryParameters["speaker_message"]!!
            val whitcombDescription = call.request.queryParameters["whitcomb_description"]!!
            val committeeDescriptions = call.request.queryParameters
                .filter { key, _ -> key.startsWith("committee_description") }
                .toMap()
                .map { it.key to it.value.joinToString(" ") }

            database.updateMessage("speaker_message", speakerMessage)
            database.updateMessage("whitcomb_description", whitcombDescription)

            committeeDescriptions.forEach { (id, value) ->
                database.updateMessage(id, value)
            }

            call.respondRedirect("/me")
        } else call.respondRedirect("/")

    }
}