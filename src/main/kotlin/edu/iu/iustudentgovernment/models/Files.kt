package edu.iu.iustudentgovernment.models

data class MeetingFile(val meeting: Meeting, val addedAt: Long, val authorUsername: String, val fileId: String, val boxUrl: String): Idable {
    override fun getPermanentId() = fileId
}

data class CommitteeFile(val committeeId: Int, val addedAt: Long, val authorUsername: String, val fileId: String, val boxUrl: String): Idable {
    override fun getPermanentId() = fileId
}