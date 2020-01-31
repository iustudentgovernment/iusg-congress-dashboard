package iu.edu.iustudentgovernment.endpoints

import iu.edu.iustudentgovernment.authentication.Member
import iu.edu.iustudentgovernment.authentication.getUser
import iu.edu.iustudentgovernment.database
import iu.edu.iustudentgovernment.getMap
import iu.edu.iustudentgovernment.handlebars
import iu.edu.iustudentgovernment.models.Paragraph
import iu.edu.iustudentgovernment.models.Statement
import iu.edu.iustudentgovernment.utils.render
import spark.Spark.get
import spark.Spark.path

private fun canEditStatements(member: Member) = member.title.map { it.rank }.max()!! >= 2

fun statements() {
    path("/statements") {
        get("") { request, _ ->
            val map = request.getMap("Statements")
            map["statements"] = database.getStatements()
            map["statementsSize"] = database.getStatements().size
            map["isPrivileged"] = request.getUser()?.let { canEditStatements(it) }

            handlebars.render(map, "statements-home.hbs")
        }

        get("/:id") { request, response ->
            val statement = database.getStatement(request.params(":id"))
            if (statement == null) response.redirect("/statements")
            else {
                val map = request.getMap("Statements | ${statement.title}")
                map["statement"] = statement

                handlebars.render(map, "statement.hbs")
            }
        }
    }

    get("/create-statement") { request, response ->
        val user = request.getUser()
        if (user == null || !canEditStatements(user)) response.redirect("/statements")
        else {
            val map = request.getMap("New Statement")

            handlebars.render(map, "new-statement.hbs")
        }
    }

    get("/edit-statement/:id") { request, response ->
        val user = request.getUser()
        val statement = database.getStatement(request.params(":id"))
        if (statement == null || user == null || !canEditStatements(user)) response.redirect("/statements")
        else {
            val map = request.getMap("Edit Statement")
            map["statement"] = statement

            handlebars.render(map, "edit-statement.hbs")
        }

    }

    get("/create-new-statement") { request, response ->
        val user = request.getUser()
        if (user == null || !canEditStatements(user)) response.redirect("/statements")
        else {
            val time = System.currentTimeMillis()
            val title = request.queryParams("title")
            val text = request.queryParams("text")

            val statement = Statement(
                database.getUuid(),
                time,
                user.username,
                null,
                null,
                title,
                text.split("\n").map { Paragraph(it) })

            database.insertStatement(statement)

            response.redirect("/statements/${statement.id}")
        }
    }
}