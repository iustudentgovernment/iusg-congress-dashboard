package edu.iu.iustudentgovernment.endpoints

import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.getMap
import edu.iu.iustudentgovernment.handlebars
import edu.iu.iustudentgovernment.utils.render
import spark.Spark.get

fun home() {
    get("/") { request, response ->
        val map = request.getMap("Home")

        val upcomingCongressMeeting = database.getCommittee("congress")!!.upcomingMeetings.firstOrNull()
        if (upcomingCongressMeeting == null) map["upcoming-meeting"] = "TBA"
        else {
            map["upcoming-meeting"] = "<a href='/meetings/${upcomingCongressMeeting.meetingId}'>${upcomingCongressMeeting.date} in ${upcomingCongressMeeting.location}</a>"
        }

        handlebars.render(map, "index.hbs")
    }
}