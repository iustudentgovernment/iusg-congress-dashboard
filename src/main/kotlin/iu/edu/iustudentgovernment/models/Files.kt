package iu.edu.iustudentgovernment.models

data class MeetingFile(val meeting: Meeting, val addedAt: Long, val authorUsername: String, val fileId: String, val boxUrl: String)

data class CommitteeFile(val committeeId: Int, val addedAt: Long, val authorUsername: String, val fileId: String, val boxUrl: String)