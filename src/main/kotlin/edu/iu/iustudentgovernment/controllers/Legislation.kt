package edu.iu.iustudentgovernment.controllers

import edu.iu.iustudentgovernment.authentication.Member
import edu.iu.iustudentgovernment.authentication.getUser
import edu.iu.iustudentgovernment.data.getMap
import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.http.HandlebarsContent
import edu.iu.iustudentgovernment.http.respondHbs
import edu.iu.iustudentgovernment.models.Committee
import edu.iu.iustudentgovernment.models.IndividualVote
import edu.iu.iustudentgovernment.models.Legislation
import edu.iu.iustudentgovernment.models.LegislationHistory
import edu.iu.iustudentgovernment.models.LegislationStage
import edu.iu.iustudentgovernment.models.Vote
import edu.iu.iustudentgovernment.models.VoteType
import edu.iu.iustudentgovernment.utils.nullifyEmpty
import io.ktor.application.call
import io.ktor.response.respondRedirect
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import kotlin.math.roundToInt

private fun canEditLegislation(member: Member, committee: Committee) =
    member.isAdministrator() || committee.isPrivileged(member) || member.title.map { it.rank }.max()!! >= 2

fun Route.legislationRoutes() {
    route("/legislation") {
        get("/vote/{id}") {
            val user = call.getUser()
            val voteFor = call.request.queryParameters["username"] ?: user?.username

            val legislation = database.getLegislation(call.parameters["id"]!!)
            if (legislation == null || user == null || voteFor == null || voteFor !in legislation.committee.members
                    .map { it.username }
                || (voteFor != user.username && !canEditLegislation(user, legislation.committee))
            ) call.respondRedirect("/legislation/view/${legislation?.id}")
            else {
                val myVote = when (call.request.queryParameters["vote"]) {
                    "yes" -> VoteType.YES
                    "no" -> VoteType.NO
                    else -> VoteType.ABSTAIN
                }

                val vote = legislation.currentStage.vote!!
                vote.votes.add(
                    IndividualVote(
                        voteFor,
                        legislation.id,
                        myVote,
                        legislation.currentStage.legislationStage
                    )
                )

                database.updateVote(vote)

                call.respondRedirect("/legislation/view/${legislation.id}")
            }
        }

        get("/vote/end/{id}") {
            val user = call.getUser()
            val legislation = database.getLegislation(call.parameters["id"]!!)
            if (legislation == null || user == null || (!legislation.committee
                    .let { !canEditLegislation(user, it) } && !user.isAdministrator())
            ) call.respondRedirect("/legislation")
            else {
                val vote = legislation.currentStage.vote!!
                val oldStage = legislation.currentStage
                oldStage.active = false
                legislation.legislationHistory.add(oldStage)

                if (!vote.passed) {
                    legislation.active = false
                    legislation.currentStage =
                        LegislationHistory(LegislationStage.FAILED, legislation.committeeId, false, null, listOf())
                } else {
                    val nextStage = legislation.nextStage!!

                    legislation.currentStage =
                        LegislationHistory(nextStage, legislation.committeeId, true, null, listOf())

                    if (oldStage.legislationStage == LegislationStage.COMMITTEE && legislation.fundingBill) {
                        if (legislation.committeeId != "oversight" && legislation.committeeId == legislation.originalCommittee) {
                            legislation.originalCommittee = legislation.committeeId
                            legislation.committeeId = "oversight"
                            legislation.currentStage =
                                LegislationHistory(LegislationStage.COMMITTEE, "oversight", true, null, listOf())
                        } else if (legislation.committeeId == "oversight") {
                            legislation.currentStage =
                                LegislationHistory(LegislationStage.GRAMMARIAN, "congress", true, null, listOf())
                            legislation.committeeId = legislation.originalCommittee
                        }
                    }
                }

                if (legislation.currentStage
                        .legislationStage == LegislationStage.FIRST_READING || legislation.currentStage
                        .legislationStage == LegislationStage.GRAMMARIAN
                ) {
                    legislation.committeeId = "congress"
                } else if (legislation.currentStage.legislationStage == LegislationStage.ENACTED) {
                    legislation.active = false
                }

                database.updateLegislation(legislation)

                call.respondRedirect("/legislation/view/${legislation.id}")
            }
        }

        get("/advance/{id}") {
            val user = call.getUser()
            val legislation = database.getLegislation(call.parameters["id"]!!)
            if (legislation == null || user == null || (!legislation.committee
                    .let { !canEditLegislation(user, it) } && !user.isAdministrator())
            ) call.respondRedirect("/legislation")
            else {
                legislation.currentStage.voteId = null
                legislation.currentStage.advancedByUsername = user.username

                legislation.legislationHistory.add(legislation.currentStage)
                if (legislation.fundingBill && legislation.currentStage
                        .legislationStage == LegislationStage.COMMITTEE && legislation.committeeId != "oversight"
                ) {
                    if (legislation.committeeId != "oversight") {
                        legislation.committeeId = "oversight"
                        legislation.currentStage =
                            LegislationHistory(LegislationStage.COMMITTEE, "oversight", true, null, listOf())
                    }
                } else {
                    if (legislation.currentStage
                            .legislationStage == LegislationStage.COMMITTEE
                    ) legislation.committeeId = legislation.originalCommittee

                    legislation.currentStage =
                        LegislationHistory(legislation.nextStage!!, legislation.committeeId, true, null, listOf())

                    if (legislation.currentStage.legislationStage == LegislationStage.ENACTED) {
                        legislation.currentStage.active = false
                        legislation.currentStage.advancedByUsername = user.username
                        legislation.active = false
                        legislation.committeeId = legislation.originalCommittee
                        legislation.legislationHistory.add(legislation.currentStage)
                    }
                }

                if (legislation.currentStage.legislationStage == LegislationStage.FIRST_READING) {
                    legislation.committeeId = "congress"
                    legislation.currentStage.committeeId = "congress"
                }

                database.updateLegislation(legislation)

                call.respondRedirect("/legislation/view/${legislation.id}")
            }
        }

        get("/fail/{id}") {
            val user = call.getUser()
            val legislation = database.getLegislation(call.parameters["id"]!!)
            if (legislation == null || user == null || (!legislation.committee
                    .let { !canEditLegislation(user, it) } && !user.isAdministrator())
            ) call.respondRedirect("/legislation")
            else {
                legislation.currentStage.advancedByUsername = user.username

                legislation.legislationHistory.add(legislation.currentStage)
                if (legislation.currentStage.legislationStage == LegislationStage.PRESIDENT) {
                    legislation.currentStage =
                        LegislationHistory(LegislationStage.VETO, legislation.committeeId, true, null, listOf())
                } else if (legislation.currentStage.legislationStage == LegislationStage.SPEAKER) {
                    legislation.currentStage =
                        LegislationHistory(LegislationStage.SPEAKER_VETO, legislation.committeeId, true, null, listOf())
                } else if (legislation.currentStage.legislationStage == LegislationStage.GRAMMARIAN) {
                    val message = call.request.queryParameters["message"]
                    legislation.currentStage.data["message"] =
                        message ?: "No specific message was given by the Grammarian."
                    legislation.currentStage.committeeId = "steering"

                    legislation.currentStage =
                        LegislationHistory(LegislationStage.COMMITTEE, legislation.committeeId, true, null, listOf())
                } else {
                    legislation.active = false
                    legislation.currentStage =
                        LegislationHistory(LegislationStage.FAILED, legislation.committeeId, false, null, listOf())

                    legislation.committeeId = legislation.originalCommittee
                }

                database.updateLegislation(legislation)

                call.respondRedirect("/legislation/view/${legislation.id}")
            }
        }


        get("/vote/start/{id}") {
            val user = call.getUser()
            val legislation = database.getLegislation(call.parameters["id"]!!)
            if (legislation == null || user == null || (!legislation.committee
                    .let { !canEditLegislation(user, it) } && !user.isAdministrator())
            ) call.respondRedirect("/legislation")
            else {
                val vote = Vote(
                    database.getUuid(),
                    legislation.id,
                    legislation.currentStage.legislationStage,
                    System.currentTimeMillis(),
                    (legislation.committee.members.size.toDouble() * 0.67).roundToInt(),
                    .50000001,
                    mutableListOf()
                )

                database.insertVote(vote)
                legislation.currentStage.voteId = vote.voteId
                database.updateLegislation(legislation)

                call.respondRedirect("/legislation/view/${legislation.id}")
            }
        }

        get("/withdraw/{id}") {
            val user = call.getUser()
            val legislation = database.getLegislation(call.parameters["id"]!!)
            if (legislation == null || user == null || (!legislation.committee
                    .let { !canEditLegislation(user, it) } && !user.isAdministrator())
            ) call.respondRedirect("/legislation")
            else {
                database.deleteLegislation(legislation.id)

                call.respondRedirect("/committees/${legislation.committeeId}")
            }
        }

        get("/edit/{id}") {
            val user = call.getUser()
            val legislation = database.getLegislation(call.parameters["id"]!!)
            if (legislation == null || user == null || (!legislation.committee
                    .let { !canEditLegislation(user, it) } && !user.isAdministrator())
            ) call.respondRedirect("/legislation")
            else {
                val map = getMap(call, "Edit Legislation")
                map["bill"] = legislation
                map["inCommittees"] = user.committeeMemberships.map { it.committee }.filter { it.id != "congress" }

                call.respondHbs(HandlebarsContent("legislation-edit.hbs", map))
            }
        }

        get("/cosponsor/{id}") {
            val user = call.getUser()
            val legislation = database.getLegislation(call.parameters["id"]!!)
            if (legislation == null || user == null || user.username in legislation.cosponsors) call.respondRedirect("/legislation")
            else {
                legislation.cosponsors.add(user.username)
                database.updateLegislation(legislation)

                call.respondRedirect("/legislation/view/${legislation.id}")
            }
        }


        get("/failed") {
            val map = getMap(call, "Legislation")
            map["failedLegislation"] = database.getFailedLegislation()

            call.respondHbs(HandlebarsContent("legislation-failed.hbs", map))
        }

        get("") {
            val map = getMap(call, "Legislation")
            map["activeLegislation"] = database.getActiveLegislation()
            map["enactedLegislation"] = database.getEnactedLegislation()

            call.respondHbs(HandlebarsContent("legislation-homepage.hbs", map))
        }

        get("/submit") {
            val user = call.getUser()
            if (user == null) call.respondRedirect("/login")
            else {
                val map = getMap(call, "Submit Legislation")
                map["inCommittees"] = user.committeeMemberships.map { it.committee }.filter { it.id != "congress" }

                call.respondHbs(HandlebarsContent("new-legislation.hbs", map))
            }
        }

        get("/view/{id}") {
            val user = call.getUser()
            val legislation = call.parameters["id"]!!.let { database.getLegislation(it) }
            val committee = legislation?.committee

            if (committee == null) call.respondRedirect("/legislation")
            else {
                val isPrivileged =
                    user?.let { canEditLegislation(user, committee) || legislation.authorUsername == user.username }
                val map = getMap(call, "Legislation | ${legislation.name}")
                map["isPrivileged"] = isPrivileged
                map["bill"] = legislation

                map["at-grammarian"] = legislation.currentStage.legislationStage == LegislationStage.GRAMMARIAN
                map["at-speaker"] = legislation.currentStage.legislationStage == LegislationStage.SPEAKER
                map["at-president"] = legislation.currentStage.legislationStage == LegislationStage.PRESIDENT
                map["needs-advanced-to-oversight"] = legislation.fundingBill && legislation.committeeId != "oversight"

                if (legislation.currentStage.vote != null && user != null) {
                    map["hasVoted"] = legislation.currentStage.vote!!.votes.any { it.username == user.username }
                    map["myVote"] =
                        legislation.currentStage.vote!!.votes.find { it.username == user.username }?.vote?.readable
                }
                if (user != null) {
                    map["isPartOfCommittee"] =
                        user.username in committee.members.map { it.username } || user.isAdministrator()
                    map["isCosponsor"] = user.username in legislation.cosponsors
                    map["isAuthor"] = user.username == legislation.authorUsername
                }

                call.respondHbs(HandlebarsContent("legislation-single.hbs", map))
            }
        }
    }

    get("/create-new-legislation") {
        val user = call.getUser()
        if (user == null) call.respondRedirect("/login")
        else {
            val name = call.request.queryParameters["name"]?.nullifyEmpty()
            val description = call.request.queryParameters["description"]?.nullifyEmpty()
            val billUrl = call.request.queryParameters["billUrl"]?.nullifyEmpty()
            val committee = call.request.queryParameters["committee"]?.nullifyEmpty()?.let { database.getCommittee(it) }
            val legislationId = call.request.queryParameters["legislationId"]?.nullifyEmpty()
            val fundingBill = call.request.queryParameters["fundingBill"] == "yes"
            val bylawsBill = call.request.queryParameters["bylawsBill"] == "yes"

            if (description == null || name == null || billUrl == null || committee == null) call.respondRedirect("/legislation/submit")
            else {
                val legislation = Legislation(
                    name,
                    description,
                    legislationId ?: database.getUuid(),
                    user.username,
                    committee.id,
                    billUrl,
                    true,
                    fundingBill,
                    bylawsBill,
                    mutableListOf(),
                    legislationHistory = mutableListOf()
                )

                if (legislationId == null) database.insertLegislation(legislation)
                else database.updateLegislation(legislation)

                call.respondRedirect("/legislation/view/${legislation.id}")
            }
        }
    }
}