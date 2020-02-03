package edu.iu.iustudentgovernment.endpoints

import edu.iu.iustudentgovernment.authentication.Member
import edu.iu.iustudentgovernment.authentication.getUser
import edu.iu.iustudentgovernment.database
import edu.iu.iustudentgovernment.getMap
import edu.iu.iustudentgovernment.handlebars
import edu.iu.iustudentgovernment.models.*
import edu.iu.iustudentgovernment.utils.nullifyEmpty
import edu.iu.iustudentgovernment.utils.render
import spark.Spark.get
import spark.Spark.path
import kotlin.math.roundToInt

private fun canEditLegislation(member: Member, committee: Committee) =
    member.isAdministrator() || committee.isPrivileged(member) || member.title.map { it.rank }.max()!! >= 2

fun legislation() {
    path("/legislation") {
        get("/vote/:id") { request, response ->
            val user = request.getUser()
            val legislation = database.getLegislation(request.params(":id"))
            if (legislation == null || user == null || user.username !in legislation.committee.members
                    .map { it.username }
            ) response.redirect("/legislation")
            else {
                val myVote = when (request.queryParams("vote")) {
                    "yes" -> VoteType.YES
                    "no" -> VoteType.NO
                    else -> VoteType.ABSTAIN
                }

                val vote = legislation.currentStage.vote!!
                vote.votes.add(
                    IndividualVote(
                        user.username,
                        legislation.id,
                        myVote,
                        legislation.currentStage.legislationStage
                    )
                )

                database.updateVote(vote)

                response.redirect("/legislation/view/${legislation.id}")
            }
        }

        get("/vote/end/:id") { request, response ->
            val user = request.getUser()
            val legislation = database.getLegislation(request.params(":id"))
            if (legislation == null || user == null || (!legislation.committee
                    .let { !canEditLegislation(user, it) } && !user.isAdministrator())
            ) response.redirect("/legislation")
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

                if (legislation.currentStage.legislationStage == LegislationStage.FIRST_READING || legislation.currentStage.legislationStage == LegislationStage.GRAMMARIAN) {
                    legislation.committeeId = "congress"
                } else if (legislation.currentStage.legislationStage == LegislationStage.ENACTED) {
                    legislation.legislationHistory.add(legislation.currentStage)
                    legislation.active = false
                }

                database.updateLegislation(legislation)

                response.redirect("/legislation/view/${legislation.id}")
            }
        }

        get("/advance/:id") { request, response ->
            val user = request.getUser()
            val legislation = database.getLegislation(request.params(":id"))
            if (legislation == null || user == null || (!legislation.committee
                    .let { !canEditLegislation(user, it) } && !user.isAdministrator())
            ) response.redirect("/legislation")
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
                   if (legislation.currentStage.legislationStage == LegislationStage.COMMITTEE) legislation.committeeId = legislation.originalCommittee

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

                response.redirect("/legislation/view/${legislation.id}")
            }
        }

        get("/fail/:id") { request, response ->
            val user = request.getUser()
            val legislation = database.getLegislation(request.params(":id"))
            if (legislation == null || user == null || (!legislation.committee
                    .let { !canEditLegislation(user, it) } && !user.isAdministrator())
            ) response.redirect("/legislation")
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
                    legislation.currentStage =
                        LegislationHistory(LegislationStage.COMMITTEE, legislation.committeeId, true, null, listOf())
                } else {
                    legislation.active = false
                    legislation.currentStage =
                        LegislationHistory(LegislationStage.FAILED, legislation.committeeId, false, null, listOf())

                    legislation.committeeId = legislation.originalCommittee
                }

                database.updateLegislation(legislation)

                response.redirect("/legislation/view/${legislation.id}")
            }
        }


        get("/vote/start/:id") { request, response ->
            val user = request.getUser()
            val legislation = database.getLegislation(request.params(":id"))
            if (legislation == null || user == null || (!legislation.committee
                    .let { !canEditLegislation(user, it) } && !user.isAdministrator())
            ) response.redirect("/legislation")
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

                response.redirect("/legislation/view/${legislation.id}")
            }
        }

        get("/withdraw/:id") { request, response ->
            val user = request.getUser()
            val legislation = database.getLegislation(request.params(":id"))
            if (legislation == null || user == null || (!legislation.committee
                    .let { !canEditLegislation(user, it) } && !user.isAdministrator())
            ) response.redirect("/legislation")
            else {
                database.deleteLegislation(legislation.id)

                response.redirect("/committees/${legislation.committeeId}")
            }
        }

        get("/edit/:id") { request, response ->
            val user = request.getUser()
            val legislation = database.getLegislation(request.params(":id"))
            if (legislation == null || user == null || (!legislation.committee
                    .let { !canEditLegislation(user, it) } && !user.isAdministrator())
            ) response.redirect("/legislation")
            else {
                val map = request.getMap("Edit Legislation")
                map["bill"] = legislation
                map["inCommittees"] = user.committeeMemberships.map { it.committee }.filter { it.id != "congress" }

                handlebars.render(map, "legislation-edit.hbs")
            }
        }

        get("/cosponsor/:id") { request, response ->
            val user = request.getUser()
            val legislation = database.getLegislation(request.params(":id"))
            if (legislation == null || user == null || user.username in legislation.cosponsors) response.redirect("/legislation")
            else {
                legislation.cosponsors.add(user.username)
                database.updateLegislation(legislation)

                response.redirect("/legislation/view/${legislation.id}")
            }
        }


        get("/failed") { request, _ ->
            val map = request.getMap("Legislation")
            map["failedLegislation"] = database.getFailedLegislation()

            handlebars.render(map, "legislation-failed.hbs")
        }

        get("") { request, _ ->
            val map = request.getMap("Legislation")
            map["activeLegislation"] = database.getActiveLegislation()
            map["enactedLegislation"] = database.getEnactedLegislation()

            handlebars.render(map, "legislation-homepage.hbs")
        }

        get("/submit") { request, response ->
            val user = request.getUser()
            if (user == null) response.redirect("/login")
            else {
                val map = request.getMap("Submit Legislation")
                map["inCommittees"] = user.committeeMemberships.map { it.committee }.filter { it.id != "congress" }

                handlebars.render(map, "new-legislation.hbs")
            }
        }

        get("/view/:id") { request, response ->
            val user = request.getUser()
            val legislation = request.params(":id").let { database.getLegislation(it) }
            val committee = legislation?.committee

            if (committee == null) response.redirect("/legislation")
            else {
                val isPrivileged =
                    user?.let { canEditLegislation(user, committee) || legislation.authorUsername == user.username }
                val map = request.getMap("Bill \"${legislation.name}\"")
                map["isPrivileged"] = isPrivileged
                map["bill"] = legislation
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
                handlebars.render(map, "legislation-single.hbs")
            }
        }
    }

    get("/create-new-legislation") { request, response ->
        val user = request.getUser()
        if (user == null) response.redirect("/login")
        else {
            val name = request.queryParams("name")?.nullifyEmpty()
            val description = request.queryParams("description")?.nullifyEmpty()
            val billUrl = request.queryParams("billUrl")?.nullifyEmpty()
            val committee = request.queryParams("committee")?.nullifyEmpty()?.let { database.getCommittee(it) }
            val legislationId = request.queryParams("legislationId")?.nullifyEmpty()
            val fundingBill = request.queryParams("fundingBill") == "yes"

            if (description == null || name == null || billUrl == null || committee == null) response.redirect("/legislation/submit")
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
                    mutableListOf(),
                    legislationHistory = mutableListOf()
                )

                if (legislationId == null) database.insertLegislation(legislation)
                else database.updateLegislation(legislation)

                response.redirect("/legislation/view/${legislation.id}")
            }
        }
    }
}