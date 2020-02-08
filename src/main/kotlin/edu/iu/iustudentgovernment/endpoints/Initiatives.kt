package edu.iu.iustudentgovernment.endpoints

import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.getMap
import edu.iu.iustudentgovernment.handlebars
import edu.iu.iustudentgovernment.models.Complaint
import edu.iu.iustudentgovernment.utils.nullifyEmpty
import edu.iu.iustudentgovernment.utils.render
import spark.Spark.get
import spark.Spark.path

fun initiatives() {
    get("/forms") { _, response -> response.redirect("/initiatives") }

    path("/initiatives") {
        get("") { request, _ ->
            val map = request.getMap("Initiatives Home")

            handlebars.render(map, "forms-home.hbs")

        }

        get("/micro-grant") { request, _ ->
            val map = request.getMap("Request a Micro-Grant")

            handlebars.render(map, "request-micro-grant.hbs")
        }

        get("/outside-legislation") { request, _ ->
            val map = request.getMap("Submit Outside Legislation")

            handlebars.render(map, "submit-outside-legislation.hbs")
        }

        get("/advertise") { request, _ ->
            val map = request.getMap("Advertise Your Group to Congress")

            handlebars.render(map, "advertise-group.hbs")
        }

        get("/complain") { request, _ ->
            val map = request.getMap("Submit Complaints or Comments about IU")

            handlebars.render(map, "submit-complaint.hbs")
        }

        get("/complaint-received") { request, _ ->
            val map = request.getMap("Message Received")

            handlebars.render(map, "message-received.hbs")
        }
    }

    get("/create-new-complaint") { request, response ->
        val name = request.queryParams("name")?.nullifyEmpty()
        val email = request.queryParams("email")?.nullifyEmpty()
        val complaintText = request.queryParams("complaint")?.nullifyEmpty()

        if (name == null || name.split(" ")
                .size < 2 || email == null || complaintText == null || complaintText.length < 50
        ) response.redirect("/initiatives/complain")
        else {
            val complaint = Complaint(
                database.getUuid(),
                name,
                email,
                complaintText,
                System.currentTimeMillis()
            )

            database.insertComplaint(complaint)

            response.redirect("/initiatives/complaint-received")
        }

    }
}