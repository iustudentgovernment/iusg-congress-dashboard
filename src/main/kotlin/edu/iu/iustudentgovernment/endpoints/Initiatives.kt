package edu.iu.iustudentgovernment.endpoints

import edu.iu.iustudentgovernment.getMap
import edu.iu.iustudentgovernment.handlebars
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
    }
}