package edu.iu.iustudentgovernment.controllers

import edu.iu.iustudentgovernment.data.getMap
import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.http.HandlebarsContent
import edu.iu.iustudentgovernment.http.respondHbs
import edu.iu.iustudentgovernment.models.Award
import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route

fun Route.awardsRoutes() {
    route("/awards") {
        get("") {
            val map = getMap(call, "Awards Home")
            map["awards"] = listOf(
                Award("Whitcomb Award", "whitcomb", database.getWhitcombDescription())
            )

            call.respondHbs(HandlebarsContent("awards-home.hbs", map))
        }

        get("/whitcomb") {
            val map = getMap(call, "Whitcomb Award")
            map["award"] = Award("Whitcomb Award", "whitcomb", database.getWhitcombDescription())
            map["winners"] = database.getAllWhitcombAwardees().sortedByDescending { it.week }

            call.respondHbs(HandlebarsContent("awards-whitcomb.hbs", map))
        }
    }
}