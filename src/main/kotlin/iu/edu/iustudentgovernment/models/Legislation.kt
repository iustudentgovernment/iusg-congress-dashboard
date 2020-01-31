package iu.edu.iustudentgovernment.models

import iu.edu.iustudentgovernment.database

data class Vote(
    val voteId: String,
    val legislationId: String,
    val legislationStage: LegislationStage,
    val start: Long,
    val quorum: Int,
    val requiredPercentToPass: Double,
    val votes: MutableList<IndividualVote>
) {
    val valid get() = votes.size >= quorum && !votePercent.isNaN()
    val isValid get() = votes.size >= quorum && !votePercent.isNaN()
    val votePercent get() = if (votes.isEmpty()) Double.NaN else (votes.sumBy { it.vote.value }.toDouble() / votes.size)
    val passed get() = valid && votePercent >= requiredPercentToPass
    val numVotes get() = votes.size
}

data class IndividualVote(
    val username: String,
    val legislationId: String,
    val vote: VoteType,
    val legislationStage: LegislationStage
)

enum class LegislationStage(val readable: String, val voteable: Boolean) {
    COMMITTEE("Committee", true), FIRST_READING("First Reading", false),
    SECOND_READING("Second Reading", false), FLOOR("Floor Vote", true),
    SPEAKER("Speaker Signature", false), SPEAKER_VETO("Speaker Veto Override", true),
    PRESIDENT("President Signature", false), VETO("Veto Override Vote", true),
    ENACTED("Enacted Legislation", false), FAILED("Failed Legislation", false)
}

enum class VoteType(val readable: String, val value: Int) {
    YES("Yes", 1), NO("No", 0), ABSTAIN("Abstain", 0)
}


data class Legislation(
    val name: String,
    val id: String,
    val authorUsername: String,
    var committeeId: String,
    val legislationBoxUrl: String,
    var active: Boolean,
    val cosponsors: MutableList<String>,
    var currentStage: LegislationHistory = LegislationHistory(
        LegislationStage.COMMITTEE,
        committeeId,
        true,
        null,
        listOf()
    ),
    val legislationHistory: MutableList<LegislationHistory>,
    val originalCommittee: String = committeeId
) {
    val failed get() = currentStage.legislationStage == LegislationStage.FAILED
    val passed
        get() = legislationHistory.find { it.legislationStage == LegislationStage.FLOOR }
            ?.let { it.vote != null && it.vote!!.passed } == true
    val enacted get() = legislationHistory.find { it.legislationStage == LegislationStage.ENACTED } != null
    val committee get() = database.getCommittee(committeeId)!!
    val author get() = database.getMember(authorUsername)!!
    val cosponsorsString get() = cosponsors.joinToString(", ") { database.getMember(it)!!.asLink }

    val nextStage
        get() = when (currentStage.legislationStage) {
            LegislationStage.COMMITTEE -> LegislationStage.FIRST_READING
            LegislationStage.FIRST_READING -> LegislationStage.SECOND_READING
            LegislationStage.SECOND_READING -> LegislationStage.FLOOR
            LegislationStage.FLOOR -> LegislationStage.SPEAKER
            LegislationStage.SPEAKER -> LegislationStage.PRESIDENT
            LegislationStage.SPEAKER_VETO -> LegislationStage.PRESIDENT
            LegislationStage.PRESIDENT -> LegislationStage.ENACTED
            LegislationStage.VETO -> LegislationStage.ENACTED
            else -> null
        }
}

fun List<Legislation>.filterCommittee(vararg committees: Committee) = filter { legislation ->
    legislation.legislationHistory.any { it.committeeId in committees.map { committees -> committees.id } }
}

data class LegislationHistory(
    val legislationStage: LegislationStage,
    val committeeId: String?,
    var active: Boolean,
    var voteId: String?,
    val notes: List<Note>
) {
    val vote get() = voteId?.let { database.getVote(it) }
}

data class Note(val authorUsername: String, val text: String)