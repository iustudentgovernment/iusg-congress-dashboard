package edu.iu.iustudentgovernment.models

data class MeetingMinutes(val meetingId: String, val committeeId: String, val fileId: String, val boxUrl: String)

data class AttendanceTaken(val meetingId: String, val attendenceId: String, val attendance: Map<String, AttendanceValue>)

enum class AttendanceValue {
    PRESENT, PROXY, ABSENT
}