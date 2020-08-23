package edu.iu.iustudentgovernment.controllers

import edu.iu.iustudentgovernment.authentication.getUser
import edu.iu.iustudentgovernment.data.getMap
import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.http.HandlebarsContent
import edu.iu.iustudentgovernment.http.respondHbs
import edu.iu.iustudentgovernment.models.Meeting
import edu.iu.iustudentgovernment.models.Note
import io.ktor.application.call
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import java.text.SimpleDateFormat

fun Route.meetingsRoutes() {
    route("/meetings") {
        get("") {
            val upcomingMeetings = database.getFutureMeetings().take(10)
            val map = getMap(call, "Upcoming Meetings")
            map["upcoming-meetings"] = upcomingMeetings
            map["past-meetings"] = database.getPastMeetings().take(5)

            call.respondHbs(HandlebarsContent("meetings-all.hbs", map))
        }

        get("/{id}") {
            val meeting = call.parameters["id"]?.let { database.getMeeting(it) }

            if (meeting == null) call.respondRedirect("/meetings")
            else {
                val map = getMap(call, "Meetings")
                map["meeting"] = meeting

                call.respondHbs(HandlebarsContent("meeting-full.hbs", map))
            }
        }
    }

    get("/delete-meeting") {
        val meeting = call.request.queryParameters["meeting"]?.let { database.getMeeting(it) }
        val user = call.getUser()

        if (meeting == null || user == null || !meeting.committee!!.isPrivileged(user)) call.respondRedirect("/meetings")
        else {
            database.deleteMeeting(meeting.meetingId)
            call.respondRedirect("/meetings")
        }
    }

    get("/create-meeting/{committee}") {
        val user = call.getUser()
        val committee = call.parameters["committee"]?.let { database.getCommittee(it) }
        if (user == null || committee == null || !committee.isPrivileged(user)) call.respondRedirect("/meetings")
        else {
            val map = getMap(call, "Create Meeting")
            map["committee"] = committee

            call.respondHbs(HandlebarsContent("new-meeting.hbs", map))
        }
    }

    get("/edit-meeting/{meeting}") {
        val user = call.getUser()
        val meeting = call.parameters["meeting"]?.let { database.getMeeting(it) }
        if (user == null || meeting == null || !meeting.committee!!.isPrivileged(user)) call.respondRedirect("/meetings")
        else {
            val map = getMap(call, "Edit Meeting")
            map["meeting"] = meeting

            call.respondHbs(HandlebarsContent("edit-meeting.hbs", map))
        }
    }

    get("/edit-meeting-action") {
        call.respondRedirect("/edit-meeting/${call.request.queryParameters["meetingId"]}")
    }

    get("/create-new-meeting/{committee}") {
        val user = call.getUser()
        val committee = call.parameters["committee"]?.let { database.getCommittee(it) }
        if (user == null || committee == null || !committee.isPrivileged(user)) call.respondRedirect("/meetings")
        else {
            val meetingId = call.request.queryParameters["meetingId"]?.let { if (it.isEmpty()) null else it }
            val date = call.request.queryParameters["time"]?.let { if (it.isEmpty()) null else it }?.let { time ->
                val formatter = SimpleDateFormat("yyyy-MM-dd'T'hh:mm")
                formatter.parse(time)
            }
            val title = call.request.queryParameters["name"]?.let { if (it.isEmpty()) null else it }
            val ledBy =
                call.request.queryParameters["ledBy"]?.let {
                    if (it.isEmpty()) listOf() else it.split(",").map { it.trim() }
                } ?: listOf()
            val location = call.request.queryParameters["location"]?.let { if (it.isEmpty()) null else it }
            val notes = call.request.queryParameters["notes"]
                ?.let { if (it.isEmpty()) listOf() else it.split("\n").map { Note(user.username, it) } } ?: listOf()
            val agendaUrl = call.request.queryParameters["agenda"]?.let { if (it.isEmpty()) null else it }
            val minutesUrl = call.request.queryParameters["minutesUrl"]?.let { if (it.isEmpty()) null else it }

            if (date == null || title == null || location == null) call.respondRedirect("/committees/${committee.id}")
            else {
                val meeting = Meeting(
                    title,
                    meetingId ?: database.getUuid(),
                    date.toInstant().toEpochMilli(),
                    location,
                    committee.id,
                    agendaUrl,
                    ledBy,
                    notes,
                    minutesUrl
                )
                if (meetingId == null) database.insertMeeting(meeting)
                else database.updateMeeting(meeting)

                call.respondRedirect("/meetings/${meeting.meetingId}")
            }
        }
    }
}