package iu.edu.iustudentgovernment.endpoints

import iu.edu.iustudentgovernment.authentication.getUser
import iu.edu.iustudentgovernment.database
import iu.edu.iustudentgovernment.getMap
import iu.edu.iustudentgovernment.handlebars
import iu.edu.iustudentgovernment.utils.render
import spark.Spark.get
import spark.Spark.post

fun member() {
    post("/edit-profile-submit/:username") { request, response ->
        val requestUser = request.getUser()
        val representative = database.getMember(request.params(":username"))
        if ((requestUser == null || representative == null) || (!requestUser.isAdministrator() && requestUser.username != representative.username)) {
            response.redirect("/representatives")
        } else {
            val name = request.queryParams("name")
            val phoneNumber = request.queryParams("phoneNumber")
            val bio = request.queryParams("bio")

            database.updateMember(requestUser.copy(name = name, phoneNumber = phoneNumber, bio = bio))

            response.redirect("/me")
        }

    }

    get("/edit-profile/:username") { request, response ->
        val requestUser = request.getUser()
        val representative = database.getMember(request.params(":username"))
        if ((requestUser == null || representative == null) || (!requestUser.isAdministrator() && requestUser.username != representative.username)) {
            response.redirect("/representatives")
        } else {
            val map = request.getMap("Edit Member ${representative.name}")
            map["member"] = representative

            handlebars.render(map, "edit-representative-info.hbs")
        }
    }
}