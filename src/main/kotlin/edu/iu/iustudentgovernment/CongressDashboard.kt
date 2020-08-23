package edu.iu.iustudentgovernment

import edu.iu.iustudentgovernment.authentication.User
import edu.iu.iustudentgovernment.authentication.casRoutes
import edu.iu.iustudentgovernment.controllers.administrationRoutes
import edu.iu.iustudentgovernment.controllers.awardsRoutes
import edu.iu.iustudentgovernment.controllers.committeesRoutes
import edu.iu.iustudentgovernment.controllers.contactRoutes
import edu.iu.iustudentgovernment.controllers.homeRoutes
import edu.iu.iustudentgovernment.controllers.initiativesRoutes
import edu.iu.iustudentgovernment.controllers.legislationRoutes
import edu.iu.iustudentgovernment.controllers.meetingsRoutes
import edu.iu.iustudentgovernment.controllers.memberRoutes
import edu.iu.iustudentgovernment.controllers.resourcesRoutes
import edu.iu.iustudentgovernment.controllers.staticContentRoutes
import edu.iu.iustudentgovernment.http.statusConfiguration
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.routing.Routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.SessionStorageMemory
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie

fun main(args: Array<String>) {
    println("Starting up")

    embeddedServer(
        Netty,
        System.getenv("PORT")?.toInt() ?: 80,
        watchPaths =
        if (System.getenv("PORT")?.toInt() == null) listOf("iusg-congress-internal")
        else listOf(),
        module = Application::module
    ).start()

}

fun Application.module() {
    install(DefaultHeaders)
    install(CallLogging)
    install(StatusPages) {
        statusConfiguration()
    }
    install(Sessions) {
        cookie<User>("user", storage = SessionStorageMemory())
    }

    install(Routing) {
        println("doing routing")
        staticContentRoutes()

        homeRoutes()
        contactRoutes()
        resourcesRoutes()
        administrationRoutes()
        awardsRoutes()
        committeesRoutes()
        contactRoutes()
        initiativesRoutes()
        memberRoutes()
        legislationRoutes()
        meetingsRoutes()
        resourcesRoutes()
        casRoutes()
    }
}