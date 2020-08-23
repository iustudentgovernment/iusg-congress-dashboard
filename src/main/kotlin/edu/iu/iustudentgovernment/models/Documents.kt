package edu.iu.iustudentgovernment.models

data class MeetingMinutes(val meetingId: String, val committeeId: String, val fileId: String, val boxUrl: String) :
    Idable {
    override fun getPermanentId() = meetingId

}

data class AttendanceTaken(
    val meetingId: String,
    val attendenceId: String,
    val attendance: Map<String, AttendanceValue>
) : Idable {
    override fun getPermanentId() = meetingId
}

enum class AttendanceValue {
    PRESENT, PROXY, ABSENT
}