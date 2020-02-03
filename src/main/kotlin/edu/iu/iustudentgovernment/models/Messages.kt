package edu.iu.iustudentgovernment.models

data class Message(val id: String, val value: Any): Idable {
    override fun getPermanentId() = id
}