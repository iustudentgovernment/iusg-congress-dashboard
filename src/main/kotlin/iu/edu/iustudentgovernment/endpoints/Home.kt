package iu.edu.iustudentgovernment.endpoints

import iu.edu.iustudentgovernment.database
import iu.edu.iustudentgovernment.getMap
import iu.edu.iustudentgovernment.handlebars
import iu.edu.iustudentgovernment.utils.render
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