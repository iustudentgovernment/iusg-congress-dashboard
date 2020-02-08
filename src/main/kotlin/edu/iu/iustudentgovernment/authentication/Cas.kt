package edu.iu.iustudentgovernment.authentication

import edu.iu.iustudentgovernment.callbackUrl
import edu.iu.iustudentgovernment.casUrl
import edu.iu.iustudentgovernment.database
import org.jsoup.Jsoup
import spark.Spark
import spark.Spark.get
import spark.Spark.path

fun cas() {
    path("/cas") {
        get("/callback") { request, response ->
            val casTicket = request.queryParams("casticket")
            if (casTicket != null) {
                val text =
                    Jsoup.connect("https://cas.iu.edu/cas/validate?cassvc=IU&casticket=$casTicket&casurl=$callbackUrl")
                        .get().body().text()
                if (text == "no") response.redirect("/callback")
                else {
                    val user = database.getMember(text.split(" ")[1])
                    if (user == null || !user.active) response.redirect("/")
                    else {
                        request.session().attribute("user", user.username)
                        response.redirect((request.session().attribute("lastUrl") ?: "/") + "&login=true")
                    }
                }
            } else response.redirect(casUrl)
        }
    }

    get("/login") { _, response ->
        response.redirect(casUrl)
    }

    get("/logout") { request, response ->
        request.session().invalidate()
        response.redirect("https://cas.iu.edu/cas/logout")
    }

}