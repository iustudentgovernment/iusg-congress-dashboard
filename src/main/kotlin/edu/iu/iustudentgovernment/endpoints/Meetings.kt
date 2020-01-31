package edu.iu.iustudentgovernment.endpoints

import edu.iu.iustudentgovernment.authentication.getUser
import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.getMap
import edu.iu.iustudentgovernment.handlebars
import edu.iu.iustudentgovernment.models.Meeting
import edu.iu.iustudentgovernment.models.Note
import edu.iu.iustudentgovernment.utils.render
import spark.Spark.get
import spark.Spark.path
import java.text.SimpleDateFormat


fun meetings() {
    path("/meetings") {
        get("") { request, _ ->
            val upcomingMeetings = database.getFutureMeetings().take(10)
            val map = request.getMap("Upcoming Meetings")
            map["upcoming-meetings"] = upcomingMeetings
            map["past-meetings"] = database.getPastMeetings().take(5)

            handlebars.render(map, "meetings-all.hbs")
        }
        get("/:id") { request, response ->
            val meeting = request.params(":id")?.let { database.getMeeting(it) }

            if (meeting == null) response.redirect("/meetings")
            else {
                val map = request.getMap("Meetings")
                map["meeting"] = meeting

                handlebars.render(map, "meeting-full.hbs")
            }
        }
    }

    get("/delete-meeting") { request, response ->
        val meeting = request.queryParams("meeting")?.let { database.getMeeting(it) }
        val user = request.getUser()

        if (meeting == null || user == null || !meeting.committee!!.isPrivileged(user)) response.redirect("/meetings")
        else {
            database.deleteMeeting(meeting.meetingId)
            response.redirect("/meetings")
        }
    }

    get("/create-meeting/:committee") { request, response ->
        val user = request.getUser()
        val committee = request.params(":committee")?.let { database.getCommittee(it) }
        if (user == null || committee == null || !committee.isPrivileged(user)) response.redirect("/meetings")
        else {
            val map = request.getMap("Create Meeting")
            map["committee"] = committee

            handlebars.render(map, "new-meeting.hbs")
        }
    }

    get("/edit-meeting/:meeting") { request, response ->
        val user = request.getUser()
        val meeting = request.params(":meeting")?.let { database.getMeeting(it) }
        if (user == null || meeting == null || !meeting.committee!!.isPrivileged(user)) response.redirect("/meetings")
        else {
            val map = request.getMap("Edit Meeting")
            map["meeting"] = meeting
            handlebars.render(map, "edit-meeting.hbs")
        }
    }

    get("/edit-meeting-action") { request, response ->
        response.redirect("/edit-meeting/${request.queryParams("meetingId")}")
    }

    get("/create-new-meeting/:committee") { request, response ->
        val user = request.getUser()
        val committee = request.params(":committee")?.let { database.getCommittee(it) }
        if (user == null || committee == null || !committee.isPrivileged(user)) response.redirect("/meetings")
        else {
            val meetingId = request.queryParams("meetingId")?.let { if (it.isEmpty()) null else it }
            val date = request.queryParams("time").let { if (it.isEmpty()) null else it }?.let { time ->
                val formatter = SimpleDateFormat("yyyy-MM-dd'T'hh:mm")
                formatter.parse(time)
            }
            val title = request.queryParams("name").let { if (it.isEmpty()) null else it }
            val ledBy = request.queryParams("ledBy").split(",").map { it.trim() }
            val location = request.queryParams("location").let { if (it.isEmpty()) null else it }
            val notes = request.queryParams("notes")
                .let { if (it.isEmpty()) listOf() else it.split("\n").map { Note(user.username, it) } }
            val agendaUrl = request.queryParams("agenda").let { if (it.isEmpty()) null else it }
            val minutesUrl = request.queryParams("minutesUrl")?.let { if (it.isEmpty()) null else it }

            if (date == null || title == null || location == null) response.redirect("/committees/${committee.id}")
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
                response.redirect("/meetings/${meeting.meetingId}")
            }
        }
    }
}