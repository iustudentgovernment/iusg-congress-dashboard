package edu.iu.iustudentgovernment.endpoints

import edu.iu.iustudentgovernment.authentication.Member
import edu.iu.iustudentgovernment.authentication.getUser
import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.getMap
import edu.iu.iustudentgovernment.handlebars
import edu.iu.iustudentgovernment.models.Paragraph
import edu.iu.iustudentgovernment.models.Statement
import edu.iu.iustudentgovernment.utils.nullifyEmpty
import edu.iu.iustudentgovernment.utils.render
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

    get("/delete-statement/:id") { request, response ->
        val user = request.getUser()
        val statement = database.getStatement(request.params(":id"))
        if (user == null || statement == null || !canEditStatements(user)) response.redirect("/statements")
        else {
            database.deleteStatement(statement.id)

            response.redirect("/statements")
        }
    }

    get("/create-new-statement") { request, response ->
        val user = request.getUser()
        if (user == null || !canEditStatements(user)) response.redirect("/statements")
        else {
            val time = System.currentTimeMillis()
            val title = request.queryParams("title")
            val text = request.queryParams("text")
            val id = request.queryParams("statementId")?.nullifyEmpty()

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

            response.redirect("/statements/${statement.id}")
        }
    }
}