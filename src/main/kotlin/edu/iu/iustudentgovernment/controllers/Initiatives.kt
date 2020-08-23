package edu.iu.iustudentgovernment.controllers

import edu.iu.iustudentgovernment.data.getMap
import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.http.HandlebarsContent
import edu.iu.iustudentgovernment.http.respondHbs
import edu.iu.iustudentgovernment.models.Complaint
import edu.iu.iustudentgovernment.utils.nullifyEmpty
import io.ktor.application.call
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route

fun Route.initiativesRoutes() {
    get("/forms") { call.respondRedirect("/initiatives") }

    route("/initiatives") {
        get("") {
            val map = getMap(call, "Initiatives Home")

            call.respondHbs(HandlebarsContent("forms-home.hbs", map))
        }

        get("/micro-grant") {
            val map = getMap(call, "a Micro-Grant")

            call.respondHbs(HandlebarsContent("micro-grant.hbs", map))
        }

        get("/outside-legislation") {
            val map = getMap(call, "Submit Outside Legislation")

            call.respondHbs(HandlebarsContent("submit-outside-legislation.hbs", map))
        }

        get("/advertise") {
            val map = getMap(call, "Advertise Your Group to Congress")

            call.respondHbs(HandlebarsContent("advertise-group.hbs", map))
        }

        get("/complain") {
            val map = getMap(call, "Submit Complaints or Comments about IU")

            call.respondHbs(HandlebarsContent("submit-complaint.hbs", map))
        }

        get("/complaint-received") {
            val map = getMap(call, "Message Received")

            call.respondHbs(HandlebarsContent("message-received.hbs", map))
        }
    }

    get("/create-new-complaint") {
        val name = call.request.queryParameters["name"]?.nullifyEmpty()
        val email = call.request.queryParameters["email"]?.nullifyEmpty()
        val complaintText = call.request.queryParameters["complaint"]?.nullifyEmpty()

        if (name == null || name.split(" ")
                .size < 2 || email == null || complaintText == null || complaintText.length < 50
        ) call.respondRedirect("/initiatives/complain")
        else {
            val complaint = Complaint(
                database.getUuid(),
                name,
                email,
                complaintText,
                System.currentTimeMillis()
            )

            database.insertComplaint(complaint)

            call.respondRedirect("/initiatives/complaint-received")
        }

    }
}