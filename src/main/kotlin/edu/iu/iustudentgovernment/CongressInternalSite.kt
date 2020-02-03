package edu.iu.iustudentgovernment

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Options
import com.google.gson.Gson
import com.rethinkdb.RethinkDB.r
import edu.iu.iustudentgovernment.authentication.cas
import edu.iu.iustudentgovernment.database.Database
import edu.iu.iustudentgovernment.endpoints.*
import edu.iu.iustudentgovernment.utils.render
import org.jsoup.Jsoup
import spark.Request
import spark.Response
import spark.Spark.*
import spark.template.handlebars.HandlebarsTemplateEngine

val urlBase = "https://iusg.herokuapp.com"
val fromEmail = "aratzman@iu.edu"
val emailTest = true

val connection = r.connection().db("iusg").user("admin", "iusg").hostname("dockhero-adjacent-48582.dockhero.io").connect()
val handlebars = HandlebarsTemplateEngine()
val gson = Gson()
val callbackUrl = "$urlBase/cas/callback"
val casUrl = "https://cas.iu.edu/cas/login?cassvc=IU&casurl=$callbackUrl"

val database = Database()

val savedPages = mutableMapOf<String, String>()

fun main() {
        println("Starting up")

        port(getHerokuAssignedPort())
        staticFileLocation("/static")

        registerHelpers()

        notFound { request, _ ->
            val map = request.getMap("404 - Not Found", "404")
            handlebars.render(map, "404.hbs")
        }

        exception(Exception::class.java) { exception, _, _ ->
            exception.printStackTrace()
        }

        println("Web server started")

        home()
        contact()
        member()
        committees()
        cas()
        statements()
        meetings()
        legislation()
        administration()
        awards()

        println("Endpoints registered")
}

fun createModelMap(
    request: Request,
    response: Response,
    title: String? = null,
    stylesheets: List<String> = listOf(),
    scripts: List<String> = listOf()
): MutableMap<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    map["request"] = request
    map["response"] = response
    map["stylesheets"] = stylesheets
    map["scripts"] = scripts;
    map.putAll(request.getMap(title ?: "Home", title ?: "home"))

    return map
}

private fun registerHelpers() {
    val field = handlebars::class.java.getDeclaredField("handlebars")
    field.isAccessible = true
    val handle = field.get(handlebars) as Handlebars

    handle.registerHelper("include-external") { first: String, options: Options ->
        if (!savedPages.containsKey(first)) savedPages[first] = Jsoup.connect(first).get().html()
        val html =  savedPages.getValue(first)
        Handlebars.SafeString(html)
    }
}

internal fun Request.getMap(
    pageTitle: String,
    pageId: String = pageTitle.replace(" ", "_").toLowerCase()
): MutableMap<String, Any?> {
    try {
        val map = mutableMapOf<String, Any?>()
        map["title"] = pageTitle
        map["page"] = pageId
        val user = session().attribute<String?>("user")?.let { database.getMember(it) }
        map["user"] = user
        map["loggedIn"] = map["user"] != null
        map["committees"] = database.getCommittees()
        map["steering"] = user?.steering

        // meta
        map["description"] = "IUSG Congressional Site"

        return map
    } catch (e: Exception) {
        e.printStackTrace()
        throw e
    }
}

class Congress


fun getHerokuAssignedPort(): Int {
    val processBuilder = ProcessBuilder()
    return if (processBuilder.environment()["PORT"] != null) {
        Integer.parseInt(processBuilder.environment()["PORT"])
    } else 80 // return default port if heroku-port isn't set (i.e. on localhost)
}