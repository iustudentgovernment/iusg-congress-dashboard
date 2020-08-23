package edu.iu.iustudentgovernment.controllers

import edu.iu.iustudentgovernment.data.getMap
import edu.iu.iustudentgovernment.http.HandlebarsContent
import edu.iu.iustudentgovernment.http.respondHbs
import io.ktor.application.call
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route

fun Route.resourcesRoutes() {
    route("/resources") {
        get("/drive") {
            // TODO
            call.respondRedirect("TODO")
        }

        get("") {
            val map = getMap(call, "Resources")

            call.respondHbs(HandlebarsContent("resources-home.hbs", map))
        }
    }
}