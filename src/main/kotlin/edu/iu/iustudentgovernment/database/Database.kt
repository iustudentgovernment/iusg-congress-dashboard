package edu.iu.iustudentgovernment.database

import com.rethinkdb.RethinkDB.r
import edu.iu.iustudentgovernment.*
import edu.iu.iustudentgovernment.authentication.Member
import edu.iu.iustudentgovernment.authentication.Role
import edu.iu.iustudentgovernment.authentication.Title
import edu.iu.iustudentgovernment.models.*
import edu.iu.iustudentgovernment.utils.asPojo
import edu.iu.iustudentgovernment.utils.createEmail
import edu.iu.iustudentgovernment.utils.queryAsArrayList
import edu.iu.iustudentgovernment.utils.sendMessage
import spark.ModelAndView
import java.util.concurrent.ConcurrentHashMap

private val membersTable = "users"
private val committeesTable = "committees"
private val committeeMembershipsTable = "committee_memberships"
private val meetingsTable = "meetings"
private val meetingMinutesTable = "meeting_minutes"
private val legislationTable = "legislation"
private val attendanceTable = "attendance"
private val committeeFilesTable = "committee_files"
private val meetingFilesTable = "meeting_files"
private val votesTable = "votes"
private val statementsTable = "statements"
private val keysTable = "keys"
private val messagesTable = "messages"
private val whitcombTable = "whitcomb"

private val tables = listOf(
    membersTable to "username",
    legislationTable to "id",
    committeeMembershipsTable to "id",
    committeesTable to "id",
    meetingMinutesTable to "fileId",
    meetingsTable to "meetingId",
    attendanceTable to "attendenceId",
    meetingFilesTable to "fileId",
    committeeFilesTable to "fileId",
    votesTable to "voteId",
    statementsTable to "id",
    keysTable to "id",
    messagesTable to "id",
    whitcombTable to "week"
).toMap()

val caches = ConcurrentHashMap(
    listOf<Pair<String, MutableList<Idable>>>(
        membersTable to mutableListOf(),
        legislationTable to mutableListOf(),
        committeeMembershipsTable to mutableListOf(),
        committeesTable to mutableListOf(),
        meetingMinutesTable to mutableListOf(),
        meetingsTable to mutableListOf(),
        attendanceTable to mutableListOf(),
        meetingFilesTable to mutableListOf(),
        committeeFilesTable to mutableListOf(),
        votesTable to mutableListOf(),
        statementsTable to mutableListOf(),
        messagesTable to mutableListOf(),
        whitcombTable to mutableListOf()
    ).toMap().toMutableMap()
)

class Database(val cleanse: Boolean) {

    init {
        println("Starting db setup")

        if (cleanse) {
            if (r.dbList().run<List<String>>(connection).contains("iusg")) {
                r.dbDrop("iusg").run<Any>(connection)
            }
        }

        println("Inserted db. Inserting tables")
        if (!r.dbList().run<List<String>>(connection).contains("iusg")) {
            r.dbCreate("iusg").run<Any>(connection)
            tables.forEach { (table, key) ->
                if (!r.tableList().run<List<String>>(connection).contains(table)) {
                    if (key != "id") r.tableCreate(table).optArg("primary_key", key).run<Any>(connection)
                    else r.tableCreate(table).run<Any>(connection)
                }
            }

        }

    }

    fun insertInitial() {
        // add committees
        insertCommittee(
            Committee(
                "Steering",
                "steering",
                "ajirelan",
                1
            )
        )
        insertCommittee(
            Committee(
                "Environmental Affairs",
                "environment",
                "ajirelan",
                0
            )
        )
        insertCommittee(
            Committee(
                "Oversight",
                "oversight",
                "arouleau",
                0
            )
        )
        insertCommittee(
            Committee(
                "Student Life",
                "student",
                "ajirelan",
                0
            )
        )
        insertCommittee(
            Committee(
                "General Assembly",
                "congress",
                "ajirelan",
                0
            )
        )

        database.getCommittees().forEach { committee ->
            database.insertMessage(
                Message(
                    committee.descriptionId,
                    "Lorem ipsum dolor sit amet, nisl causae ei vim, an augue persius mel, nam dicit epicurei lucilius in. Ex per solet percipitur, soleat interpretaris ius ei."
                )
            )
        }

        // add steering members
        insertMember(
            Member(
                "ajirelan",
                "Maurer School of Law",
                "Andrew Ireland",
                "ajirelan@iu.edu",
                "812-205-1226",
                listOf(Title.SPEAKER),
                "Andrew Ireland is a third-year JD/MBA candidate at the Maurer School of Law. He recently returned from a year abroad studying for his MBA in Seoul, South Korea at Sungkyunkwan University’s Global School of Business. A former Cox Scholar, he graduated magna cum laude in 2017 from the O’Neill School of Public and Environmental Affairs and the Media School with dual degrees in Public Financial Management and Journalism. As Chairman of the IUSG Oversight and Reform Committee (“IORC”), he is especially interested in leveraging technology to increase the effectiveness and transparency of IUSG. In his free time, he enjoys walking his dog Peanut, IU Basketball and Dragon Express."
            )
        )
        insertMember(
            Member(
                "aratzman",
                "Luddy School of Informatics, Computing, and Engineering",
                "Adam Ratzman",
                "aratzman@iu.edu",
                "317-979-8260",
                listOf(Title.PARLIAMENTARIAN, Title.SITE_ADMINISTRATOR),
                "Adam Ratzman is a first-year student studying Computer Science with minors in Cognitive Science and French. Adam is an undergraduate researcher for the Cognitive Science Program and is particularly interested in building distributed systems and domain-specific programming languages. This is his first year in IUSG, where he represents the School of Informatics, Computing, and Engineering. Outside of IUSG, Adam serves as a member of the Student Organization Registration Committee and as the Vice-Chair of Finance on the Forest Honors Leadership Council. He envisions a campus with more academic resources to help students succeed and with more opportunities for personalized learning and peer teaching. During this Congressional term, Adam is focused on educational policy and student safety."
            )
        )
        insertMember(
            Member(
                "mimarush",
                "School of Education",
                "Michaela Rush",
                "mimarush@iu.edu",
                "979-595-4056",
                listOf(Title.PRESS_SECRETARY),
                "Michaela Rush is a freshman from College Station, Texas, and is currently studying Secondary Education for English/Language Arts. She is currently serving as the School of Education representative and the Congressional Press Secretary. Though she is not studying law or politics, Michaela is passionate about student government and student involvement in general, and hopes to use her position to encourage all students to be active participants in their collegiate experience. Michaela enjoys boba tea and is a piccolo player in the Marching Hundred band. Feel free to reach out to her with any questions, comments or concerns!"
            )
        )
        insertMember(
            Member(
                "dpickard",
                "University Division",
                "Daniel Pickard-Carlisle",
                "dpickard@iu.edu",
                "317-719-0433",
                listOf(Title.GRAMMARIAN),
                "Daniel Pickard-Carlisle is a freshman from Noblesville, Indiana, majoring in Spanish and History with a concentration on Latin America and colonialism. He currently represents Undergraduate Division students on the IU campus and looks forward to working for and with his constituents. Outside of Congress, Daniel is very active with La Casa and is a Hudson & Holland Scholar. He has worked as a legal intern for the Hamilton County Prosecuting Attorney's Office, and plans on attending law school after completing his undergraduate degree. He enjoys watching Game of Thrones, Euphoria, and The Office, and spends his free time doing so. Daniel is very excited to work with the Congress and pass legislation that will better the lives of students at IU."
            )
        )
        insertMember(
            Member(
                "arouleau",
                "Off-Campus",
                "Anne Rouleau",
                "arouleau@iu.edu",
                "763-3606-760",
                listOf(Title.COMMITTEE_CHAIR),
                "Anne Rouleau was elected to the IUSG Congress in the Fall of 2019 and is excited to serve the greater IU community. After two years as a Law and Public Policy major in SPEA, Anne is now studying Entrepreneurship and Corporate Innovation in the Kelley School of Business. As a member of the IU Varsity Swim Team, Anne is the team's representative to the NCAA's Student-Athlete Advisory Committee. In her spare time, Anne can be found exploring new breakfast locations in town, trying new recipes, and laughing with her friends."
            )
        )

        insertMember(
            Member(
                "thompsdp",
                "Off-Campus",
                "Dominic Thompson",
                "thompsdp@iu.edu",
                "260-212-3243",
                listOf(),
                null,
                active = false
            )
        )

        // add steering members' committee memberships
        insertCommitteeMembership(CommitteeMembership("ajirelan", getUuid(), "steering", Role.COMMITTEE_CHAIR))
        updateCommitteeMembership(getCommitteeMembershipsForMember("ajirelan").first { it.committeeId == "congress" }
            .copy(role = Role.COMMITTEE_CHAIR))
        insertCommitteeMembership(CommitteeMembership("ajirelan", getUuid(), "environment", Role.COMMITTEE_CHAIR))
        insertCommitteeMembership(CommitteeMembership("ajirelan", getUuid(), "student", Role.COMMITTEE_CHAIR))

        insertCommitteeMembership(CommitteeMembership("aratzman", getUuid(), "steering", Role.PRIVILEGED_MEMBER))
        updateCommitteeMembership(getCommitteeMembershipsForMember("aratzman").first { it.committeeId == "congress" }
            .copy(role = Role.PRIVILEGED_MEMBER))
        insertCommitteeMembership(CommitteeMembership("aratzman", getUuid(), "student", Role.MEMBER))
        insertCommitteeMembership(CommitteeMembership("aratzman", getUuid(), "oversight", Role.MEMBER))

        insertCommitteeMembership(CommitteeMembership("dpickard", getUuid(), "steering", Role.PRIVILEGED_MEMBER))
        updateCommitteeMembership(getCommitteeMembershipsForMember("dpickard").first { it.committeeId == "congress" }
            .copy(role = Role.PRIVILEGED_MEMBER))

        insertCommitteeMembership(CommitteeMembership("mimarush", getUuid(), "steering", Role.PRIVILEGED_MEMBER))
        updateCommitteeMembership(getCommitteeMembershipsForMember("mimarush").first { it.committeeId == "congress" }
            .copy(role = Role.PRIVILEGED_MEMBER))

        insertCommitteeMembership(CommitteeMembership("arouleau", getUuid(), "steering", Role.PRIVILEGED_MEMBER))
        insertCommitteeMembership(CommitteeMembership("arouleau", getUuid(), "oversight", Role.COMMITTEE_CHAIR))
        updateCommitteeMembership(getCommitteeMembershipsForMember("arouleau").first { it.committeeId == "congress" }
            .copy(role = Role.PRIVILEGED_MEMBER))

        insertMessage(
            Message(
                "speaker_message",
                "Lorem ipsum dolor sit amet, nisl causae ei vim, an augue persius mel, nam dicit epicurei lucilius in. Ex per solet percipitur, soleat interpretaris ius ei. An eum clita putant habemus, reque oportere forensibus nam ad, ea mundi consequat argumentum usu. Ut nec atqui ancillae, in sit solum labores detraxit."
            )
        )

        insertMessage(
            Message(
                "whitcomb_description",
                "Lorem ipsum dolor sit amet, nisl causae ei vim, an augue persius mel, nam dicit epicurei lucilius in. Ex per solet percipitur, soleat interpretaris ius ei. An eum clita putant habemus, reque oportere forensibus nam ad, ea mundi consequat argumentum usu. Ut nec atqui ancillae, in sit solum labores detraxit."
            )
        )

        // create meeting
        insertMeeting(
            Meeting(
                "General Body Meeting",
                getUuid(),
                1580171400000,
                "IMU State Room West",
                "congress",
                null,
                listOf("ajirelan"),
                listOf(
                    Note("aratzman", "This is the first Congressional meeting of spring semester!")
                )
            )
        )


        // insert example future meeting
        insertMeeting(
            Meeting(
                "General Body Meeting",
                getUuid(),
                1580776200000,
                "IMU State Room West",
                "congress",
                null,
                listOf("ajirelan"),
                listOf(
                    Note("aratzman", "Snacks and light refreshments will be provided")
                )
            )
        )

        // insert example statement
        insertStatement(
            Statement(
                getUuid(),
                1571025600000,
                "thompsdp",
                null,
                null,
                "Indiana University Student Government Congress Letter to President Mishkin",
                listOf(
                    Paragraph(
                        "I am writing this letter in response to your repeated attempts to delay discussing the role of the executive and legislative branches. As you ought to know, this issue has caused much contention between our two branches for several years. The executive branch has felt as if they have the power to act unilaterally on behalf of this student government. The executive branch has acted as the policy making authority and the implementing authority for as long as many of us have been in the Congress. This trend is a disturbing violation of the intent of the IUSG Constitution, which you have a duty to uphold. The mission of this letter is to inform you that the IUSG Congress will no longer allow this blatant abuse of power to continue. Throughout this letter, I will outline the role of each branch, and the expectation that Congress has going forward."
                    ),
                    Paragraph(
                        "The role of Congress is to serve as the final policy making authority for the Indiana University Student Government. This means that any policy implemented on behalf of this government must be approved by the Congress prior to implementation. The reason that this is the role of Congress is three fold. The first reason is that we represent specified constituents and have the ability, as an institution, to speak with a greater number of students on policy issues. Secondly, as an institution, we have more diversity of opinions than a small team of executive committee members. Furthermore, as proposed policies go through the legislative process, more scrutiny can be placed upon them than if it is made by a small team of individuals who make up the policy as they go. This is the reason that the IUSG Constitution leaves the power of final policy making authority within the power of the Congress, not the executive branch."
                    ),
                    Paragraph(
                        "The role of the executive branch is to be the voice of the student body and to implement the mandates of congress in any means necessary and proper. This is evident in the IUSG Constitution through a number of ways. The first way this power of implementation is evident tobe reserved for the executive branch would be when the Constitution says that the duty of the executive branch is to act upon mandates of Congress. Secondly, the executive branch has an advantage in implementing a policy that is not given to Congress, the executive branch is able to " +
                                "hold regular meetings with administrators who impact policy on campus. This allows the executive branch more power to influence campus policy that does not exist for the Congress. Finally, the executive branch is given the power to appoint directors to help implement policies adopted by IUSG. Within the Congress, committee chairs, our most equivalent role of Executive Directors,  do not have the power to act unilaterally on behalf of their committee. Committee chairs must work with their fellow congressional members to approve a path forward on policy issues. On the other head, Executive Directors only need to follow the direction of the President of IUSG."
                    ),
                    Paragraph(
                        "I would also like to shed light upon one final issue within our relationship of two branches of this government we represent. In the past, and what seems to be occurring now, the executive branch has forgotten that our Constitution does not say that any one branch shall be in charge of the other two. In fact, the word branch would imply that we are all connected to the same tree or, in this case, organization. We are supposed to operate as checks upon one another,not as the superior to each other. This has been the position of the executive branch in recent years and it must cease. This position creates toxicity between two branches and only leads to us fighting the same organization that requires both of us to exist."
                    ),
                    Paragraph(
                        "President Mishkin, I call upon you to cease your current position on acting as both a legislator, approving policy, and an executive, implementing policy. As I have demonstrated throughout this letter, continuing with your current interpretation of the constitution, whatever it may be, is antithetical to the Constitution of IUSG, which as President of this great organization, you should preserve and protect. Furthermore, I would ask you to suspend your disposition towards portraying the executive branch as a superior branch to the IUSG Congress. Ultimately,I am asking you to conform your leadership to the role laid out for your branch within the IUSG Constitution and conduct your branch in a way that respects the other branches of this great government."
                    )
                )
            )
        )

        // insert legislation
        insertLegislation(
            Legislation(
                "Late-Night Transportation",
                "To provide safe, affordable, and accessible late-night transportation options for IU students in the vicinity of the Indiana University Bloomington campus",
                database.getUuid(),
                "aratzman",
                "student",
                "https://iu.box.com/s/6t72190q8c4uqvlvwkfw6ccd6zkman5s",
                true,
                fundingBill = true,
                bylawsBill = false,
                cosponsors = mutableListOf(),
                legislationHistory = mutableListOf()
            )
        )


    }

    // members
    fun getMember(username: String): Member? = get(membersTable, username)
    fun getMembers() = getAll<Member>(membersTable).filter { it.active }.sortedBy { it.name.split(" ").last() }
    fun insertMember(member: Member) {
        insert(membersTable, member)
        if (member.active) insertCommitteeMembership(
            CommitteeMembership(
                member.username,
                getUuid(),
                "congress",
                Role.MEMBER
            )
        )
    }

    fun updateMember(member: Member) = update(membersTable, member.username, member)

    fun deleteMember(memberId: String) {
        val member = getMember(memberId)!!
        member.active = false

        updateMember(member)
    }


    // committee memberships
    fun getCommitteeMembership(id: Any): CommitteeMembership? = get(committeeMembershipsTable, id)
    fun getAllCommitteeMemberships() =
        getAll<CommitteeMembership>(committeeMembershipsTable).sortedBy { it.committee.formalName }

    fun getCommitteeMembersForCommittee(committee: String) =
        getAllCommitteeMemberships().filter { it.committeeId == committee }

    fun getCommitteeMembershipsForMember(member: String) =
        getAllCommitteeMemberships().filter { it.username == member }

    fun insertCommitteeMembership(committeeMembership: CommitteeMembership) =
        insert(committeeMembershipsTable, committeeMembership)

    fun updateCommitteeMembership(committeeMembership: CommitteeMembership) =
        update(committeeMembershipsTable, committeeMembership.id, committeeMembership)

    fun deleteCommitteeMembership(committeeMembershipId: String) =
        delete(committeeMembershipsTable, committeeMembershipId)


    // committees
    fun getCommittee(committeeId: String): Committee? = get(committeesTable, committeeId)
    fun getCommittees() = getAll<Committee>(committeesTable).sortedBy { it.formalName }

    fun insertCommittee(committee: Committee) = insert(committeesTable, committee)

    fun updateCommittee(committee: Committee) = update(committeesTable, committee.id, committee)

    fun deleteCommittee(committeeId: String) = delete(committeesTable, committeeId)

    // meetings
    fun getMeetings() = getAll<Meeting>(meetingsTable)
    fun getFutureMeetings(): List<Meeting> =
        getMeetings().filter { it.time > System.currentTimeMillis() }.sortedBy { it.time }

    fun getPastMeetings(): List<Meeting> =
        getMeetings().filter { it.time <= System.currentTimeMillis() }.sortedBy { it.time }

    fun getMeeting(id: Any): Meeting? = get(meetingsTable, id)
    fun insertMeeting(meeting: Meeting) {
        insert(meetingsTable, meeting)

        if (meeting.time >= System.currentTimeMillis()) {
            val committee = meeting.committee!!
            val email = handlebars.render(
                ModelAndView(
                    mapOf(
                        "committee" to committee,
                        "date" to meeting.date,
                        "location" to meeting.location,
                        "url" to "$urlBase/meetings/${meeting.meetingId}",
                        "urlBase" to urlBase
                    ), "emails/new-meeting.hbs"
                )
            )

            sendMessage(
                createEmail(
                    meeting.committee!!.members.map { it.email },
                    fromEmail,
                    "${committee.formalName} meeting at ${meeting.date}",
                    email
                )
            )
        }
    }

    fun updateMeeting(meeting: Meeting) = update(meetingsTable, meeting.meetingId, meeting)
    fun deleteMeeting(meetingId: String) = delete(meetingsTable, meetingId)


    // meeting minutes
    fun insertMeetingMinutes(meetingMinutes: MeetingMinutes) = insert(meetingMinutesTable, meetingMinutes)
    fun getMeetingMinutes() = getAll<MeetingMinutes>(meetingMinutesTable)
    fun getMeetingMinutes(id: Any): MeetingMinutes? = get(meetingMinutesTable, id)
    fun updateMeetingMinutes(meetingMinutes: MeetingMinutes) =
        update(meetingMinutesTable, meetingMinutes.fileId, meetingMinutes)

    fun deleteMeetingMinutes(meetingMinutesId: String) = delete(meetingMinutesTable, meetingMinutesId)


    // meeting files
    fun insertMeetingFile(meetingFile: MeetingFile) = insert(meetingFilesTable, meetingFile)
    fun getMeetingFiles() = getAll<MeetingFile>(meetingFilesTable)
    fun getMeetingFile(id: Any): MeetingFile? = get(meetingFilesTable, id)
    fun updateMeetingFile(meetingFile: MeetingFile) = update(meetingFilesTable, meetingFile.fileId, meetingFile)
    fun deleteMeetingFile(meetingFileId: String) = delete(meetingFilesTable, meetingFileId)


    // committee files
    fun insertCommitteeFile(committeeFile: CommitteeFile) = insert(committeeFilesTable, committeeFile)
    fun getCommitteeFiles() = getAll<CommitteeFile>(committeeFilesTable)
    fun getCommitteeFile(id: Any): CommitteeFile? = get(committeeFilesTable, id)
    fun updateCommitteeFile(committeeFile: CommitteeFile) =
        update(committeeFilesTable, committeeFile.fileId, committeeFile)

    fun deleteCommitteeFile(committeeFileId: String) = delete(committeeFilesTable, committeeFileId)


    // legislation
    fun insertLegislation(legislation: Legislation) {
        insert(legislationTable, legislation)

        val members = legislation.committee.members.map { it.email }

        val email = handlebars.render(
            ModelAndView(
                mapOf(
                    "legislation" to legislation,
                    "url" to "$urlBase/legislation/view/${legislation.id}",
                    "urlBase" to urlBase
                ), "emails/legislation-new.hbs"
            )
        )

        sendMessage(
            createEmail(
                members,
                fromEmail,
                "New Legislation | ${legislation.name}",
                email
            )
        )

    }

    fun getLegislation() = getAll<Legislation>(legislationTable)
    fun getLegislation(id: String): Legislation? = get(legislationTable, id)
    fun updateLegislation(legislation: Legislation) {
        update(legislationTable, legislation.id, legislation)

        if (legislation.enacted) {
            val members = legislation.committee.members.map { it.email }

            val email = handlebars.render(
                ModelAndView(
                    mapOf(
                        "legislation" to legislation,
                        "url" to "$urlBase/legislation/view/${legislation.id}",
                        "urlBase" to urlBase
                    ), "emails/legislation-enacted.hbs"
                )
            )

            sendMessage(
                createEmail(
                    members,
                    fromEmail,
                    "Legislation Has Been Enacted | ${legislation.name}",
                    email
                )
            )
        }
    }

    fun deleteLegislation(legislationId: String) = delete(legislationTable, legislationId)
    fun getEnactedLegislation() = getLegislation().filter { it.enacted }
    fun getFailedLegislation() = getLegislation().filter { it.failed }
    fun getPassedLegislation() = getLegislation().filter { it.passed }
    fun getInactiveLegislation() = getLegislation().filter { !it.active }
    fun getActiveLegislation() = getLegislation().filter { it.active }

    // attendance
    fun insertAttendance(attendanceTaken: AttendanceTaken) = insert(attendanceTable, attendanceTaken)
    fun getAttendances() = getAll<AttendanceTaken>(attendanceTable)
    fun getAttendance(id: String): AttendanceTaken? = get(attendanceTable, id)
    fun getAttendanceForMeeting(meetingId: String) = getAttendances().find { it.meetingId == meetingId }
    fun updateAttendance(attendanceTaken: AttendanceTaken) =
        update(attendanceTable, attendanceTaken.attendenceId, attendanceTaken)

    fun deleteAttendance(attendanceTakenId: String) = delete(attendanceTable, attendanceTakenId)


    // votes
    fun insertVote(vote: Vote) = insert(votesTable, vote)
    fun insertIndividualVote(individualVote: IndividualVote, vote: Vote) =
        updateVote(vote.apply { votes.add(individualVote) })

    fun getVotes() = getAll<Vote>(votesTable)
    fun getVote(id: String): Vote? = get(votesTable, id)
    fun getVotesContainingMember(member: String) =
        getVotes().filter { it.votes.any { vote -> vote.username == member } }

    fun getMemberVotes(member: String) =
        getVotesContainingMember(member).map { it.votes.first { vote -> vote.username == member } }

    fun updateVote(vote: Vote) = update(votesTable, vote.voteId, vote)
    fun deleteVote(voteId: String) = delete(votesTable, voteId)


    // statements
    fun getStatements() = getAll<Statement>(statementsTable).sortedByDescending { it.lastEditTime ?: it.createdAt }
    fun insertStatement(statement: Statement) = insert(statementsTable, statement)
    fun getStatement(id: String): Statement? = get(statementsTable, id)
    fun updateStatement(statement: Statement) = update(statementsTable, statement.id, statement)
    fun deleteStatement(statementId: String) = delete(statementsTable, statementId)

    // messages
    fun getMessage(id: String): Message? = get(messagesTable, id)
    fun updateMessage(message: Message) = update(messagesTable, message.id, message)
    fun updateMessage(id: String, value: Any) = update(messagesTable, id, Message(id, value))
    fun insertMessage(message: Message) = insert(messagesTable, message)
    fun getSpeakerMessage() = getMessage("speaker_message")!!.value
    fun getWhitcombDescription() = getMessage("whitcomb_description")!!.value.toString()

    // whitcomb award
    fun getAllWhitcombAwardees() = getAll<Whitcomb>(whitcombTable)
    fun getWhitcombAwardsForMember(username: String) = getAllWhitcombAwardees().filter { it.username == username }
    fun insertWhitcombAward(award: Whitcomb) = insert(whitcombTable, award)

    // utils

    fun getUuid() = r.uuid().run<String>(connection)!!


    fun update(table: String, id: Any, obj: Idable): Any? {
        if (table in caches.keys) {
            caches[table]!!.removeIf { it.getPermanentId() == id }
            caches[table]!!.add(obj)
        }
        return r.table(table).get(id).replace(r.json(gson.toJson(obj))).run<Any>(connection)
    }

    fun delete(table: String, id: Any): Any? {
        if (table in caches.keys) caches[table]!!.removeIf { it.getPermanentId() == id }
        return r.table(table).get(id).delete().run<Any>(connection)
    }

    fun <T : Idable> insert(table: String, obj: T): Any? {
        if (table in caches.keys) caches[table]!!.add(obj)
        return r.table(table).insert(r.json(gson.toJson(obj))).run<Any>(connection)
    }

    inline fun <reified T : Idable> getAll(table: String): List<T> {
        return if (table in caches.keys && caches[table]!!.isNotEmpty()) {
            caches[table]!!.mapNotNull { it as? T }
        } else {
            val values = r.table(table).run<Any>(connection).queryAsArrayList(gson, T::class.java).filterNotNull()
            caches[table]!!.addAll(values)

            values
        }
    }

    inline fun <reified T : Idable> get(table: String, id: Any): T? {
        return if (table in caches.keys) {
            val cache = caches[table]!!
            val found = cache.find { it.getPermanentId() == id }
            if (found == null) {
                val retrieved = asPojo(gson, r.table(table).get(id).run(connection), T::class.java)
                (retrieved as? Idable)?.let { cache.add(it) }
            }

            cache.find { it.getPermanentId() == id } as? T
        } else asPojo(gson, r.table(table).get(id).run(connection), T::class.java)
    }
}