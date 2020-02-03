package edu.iu.iustudentgovernment.endpoints

import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.getMap
import edu.iu.iustudentgovernment.handlebars
import edu.iu.iustudentgovernment.models.Award
import edu.iu.iustudentgovernment.utils.render
import spark.Spark.get
import spark.Spark.path

fun awards() {
    path("/awards") {
        get("") { request, _ ->
            val map = request.getMap("Awards Home")
            map["awards"] = listOf(
                Award("Whitcomb Award", "whitcomb", database.getWhitcombDescription())
            )

            handlebars.render(map, "awards-home.hbs")
        }

        get("/whitcomb") { request, _ ->
            val map = request.getMap("Whitcomb Award")
            map["award"] = Award("Whitcomb Award", "whitcomb", database.getWhitcombDescription())
            map["winners"] = database.getAllWhitcombAwardees().sortedByDescending { it.week }

            handlebars.render(map, "awards-whitcomb.hbs")
        }
    }
}