package edu.iu.iustudentgovernment.endpoints

import edu.iu.iustudentgovernment.getMap
import edu.iu.iustudentgovernment.handlebars
import edu.iu.iustudentgovernment.utils.render
import spark.Spark.get
import spark.Spark.path

fun resources() {
    path("/resources") {
        get("/drive") { _, response -> response.redirect("https://drive.google.com/drive/u/2/folders/1e98YxBybghre4gsuUlpM8pPv22DtWvKL")}
        get("") { request, _ ->
            val map = request.getMap("Resources")

            handlebars.render(map, "resources-home.hbs")
        }
    }
}