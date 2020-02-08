package edu.iu.iustudentgovernment.models

data class Complaint(val id: String, val name: String, val email: String, val text: String, val time: Long): Idable {
    override fun getPermanentId()= id
}