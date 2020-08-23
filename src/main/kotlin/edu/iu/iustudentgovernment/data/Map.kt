package edu.iu.iustudentgovernment.data

import edu.iu.iustudentgovernment.authentication.User
import edu.iu.iustudentgovernment.database
import io.ktor.application.ApplicationCall
import io.ktor.sessions.get
import io.ktor.sessions.sessions

fun createModelMap(
    title: String? = null,
    stylesheets: List<String> = listOf(),
    scripts: List<String> = listOf()
): MutableMap<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    map["stylesheets"] = stylesheets
    map["scripts"] = scripts
    map["title"] = title

    return map
}

internal fun getMap(
    call: ApplicationCall,
    pageTitle: String,
    pageId: String = pageTitle.replace(" ", "_").toLowerCase()
): MutableMap<String, Any?> {
    try {
        val map = mutableMapOf<String, Any?>()
        map["title"] = pageTitle
        map["page"] = pageId
        val user = call.sessions.get<User>()?.let { database.getMember(it.userId) }
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