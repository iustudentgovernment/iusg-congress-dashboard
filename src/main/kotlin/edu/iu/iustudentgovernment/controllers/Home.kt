package edu.iu.iustudentgovernment.controllers

import edu.iu.iustudentgovernment.data.getMap
import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.http.HandlebarsContent
import edu.iu.iustudentgovernment.http.respondHbs
import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.homeRoutes() {
    get("/") {
        val map = getMap(call, "Home")

        val upcomingCongressMeeting = database.getCommittee("congress")!!.upcomingMeetings.firstOrNull()
        if (upcomingCongressMeeting == null) map["upcoming-meeting"] = "TBA"
        else {
            map["upcoming-meeting"] =
                "<a href='/meetings/${upcomingCongressMeeting.meetingId}'>${upcomingCongressMeeting.date} in ${upcomingCongressMeeting.location}</a>"
        }

        map["speaker-message"] = database.getSpeakerMessage()

        call.respondHbs(HandlebarsContent("index.hbs", map))
    }
}