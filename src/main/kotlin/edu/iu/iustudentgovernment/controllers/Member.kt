package edu.iu.iustudentgovernment.controllers

import edu.iu.iustudentgovernment.authentication.getUser
import edu.iu.iustudentgovernment.data.getMap
import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.http.HandlebarsContent
import edu.iu.iustudentgovernment.http.respondHbs
import io.ktor.application.call
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post

fun Route.memberRoutes() {
    post("/edit-profile-submit/{username}") {
        val requestUser = call.getUser()
        val representative = database.getMember(call.parameters["username"]!!)
        if ((requestUser == null || representative == null) || (!requestUser.isAdministrator() && requestUser.username != representative.username)) {
            call.respondRedirect("/representatives")
        } else {
            val name = call.request.queryParameters["name"]!!
            val phoneNumber = call.request.queryParameters["phoneNumber"]
            val bio = call.request.queryParameters["bio"]

            database.updateMember(requestUser.copy(name = name, phoneNumber = phoneNumber, bio = bio))

            call.respondRedirect("/me")
        }

    }

    get("/edit-profile/{username}") {
        val requestUser = call.getUser()
        val representative = database.getMember(call.parameters["username"]!!)
        if ((requestUser == null || representative == null) || (!requestUser.isAdministrator() && requestUser.username != representative.username)) {
            call.respondRedirect("/representatives")
        } else {
            val map = getMap(call, "Edit Member ${representative.name}")
            map["member"] = representative

            call.respondHbs(HandlebarsContent("edit-representative-info.hbs", map))
        }
    }
}