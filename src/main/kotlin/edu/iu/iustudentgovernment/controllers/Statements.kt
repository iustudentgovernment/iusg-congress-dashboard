package edu.iu.iustudentgovernment.controllers

import edu.iu.iustudentgovernment.authentication.Member
import edu.iu.iustudentgovernment.authentication.getUser
import edu.iu.iustudentgovernment.data.getMap
import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.http.HandlebarsContent
import edu.iu.iustudentgovernment.http.respondHbs
import edu.iu.iustudentgovernment.models.Paragraph
import edu.iu.iustudentgovernment.models.Statement
import edu.iu.iustudentgovernment.utils.nullifyEmpty
import io.ktor.application.call
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route

private fun canEditStatements(member: Member) = member.title.map { it.rank }.max()!! >= 2

fun Route.statementsRoutes() {
    route("/statements") {
        get("") {
            val map = getMap(call, "Statements")
            map["statements"] = database.getStatements()
            map["statementsSize"] = database.getStatements().size
            map["isPrivileged"] = call.getUser()?.let { canEditStatements(it) }

            call.respondHbs(HandlebarsContent("statements-home.hbs", map))
        }

        get("/{id}") {
            val statement = database.getStatement(call.parameters["id"]!!)
            if (statement == null) call.respondRedirect("/statements")
            else {
                val map = getMap(call, "Statements | ${statement.title}")
                map["statement"] = statement

                call.respondHbs(HandlebarsContent("statement.hbs", map))
            }
        }
    }

    get("/create-statement") {
        val user = call.getUser()
        if (user == null || !canEditStatements(user)) call.respondRedirect("/statements")
        else {
            val map = getMap(call, "New Statement")

            call.respondHbs(HandlebarsContent("new-statement.hbs", map))
        }
    }

    get("/edit-statement/{id}") {
        val user = call.getUser()
        val statement = database.getStatement(call.parameters["id"]!!)
        if (statement == null || user == null || !canEditStatements(user)) call.respondRedirect("/statements")
        else {
            val map = getMap(call, "Edit Statement")
            map["statement"] = statement

            call.respondHbs(HandlebarsContent("edit-statement.hbs", map))
        }

    }

    get("/delete-statement/{id}") {
        val user = call.getUser()
        val statement = database.getStatement(call.parameters["id"]!!)
        if (user == null || statement == null || !canEditStatements(user)) call.respondRedirect("/statements")
        else {
            database.deleteStatement(statement.id)

            call.respondRedirect("/statements")
        }
    }

    get("/create-new-statement") {
        val user = call.getUser()
        if (user == null || !canEditStatements(user)) call.respondRedirect("/statements")
        else {
            val time = System.currentTimeMillis()
            val title = call.request.queryParameters["title"]!!
            val text = call.request.queryParameters["text"]!!
            val id = call.request.queryParameters["statementId"]?.nullifyEmpty()

            val statement = if (id == null) {
                val tmp = Statement(
                    id ?: database.getUuid(),
                    time,
                    user.username,
                    null,
                    null,
                    title,
                    text.split("\n").map { Paragraph(it) })

                database.insertStatement(tmp)
                tmp
            } else {
                val tmp = database.getStatement(id)!!.copy(
                    lastEditTime = System.currentTimeMillis(),
                    lastEditedByUsername = user.username,
                    paragraphs = text.split("\n").map { Paragraph(it) },
                    title = title
                )
                database.updateStatement(tmp)
                tmp
            }

            call.respondRedirect("/statements/${statement.id}")
        }
    }
}