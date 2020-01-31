package edu.iu.iustudentgovernment.utils

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.Base64.encodeBase64URLSafeString
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Message
import edu.iu.iustudentgovernment.CongressInternalSite
import java.io.*
import java.util.*
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart


private const val APPLICATION_NAME = "Gmail API Java Quickstart"
private val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance()
private const val TOKENS_DIRECTORY_PATH = "tokens"

private val SCOPES: List<String> = GmailScopes.all().toList()
private const val CREDENTIALS_FILE_PATH = "/credentials.json"

val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()!!
var service = Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
    .setApplicationName(APPLICATION_NAME)
    .build()!!
const val userId = "me"

private fun getCredentials(HTTP_TRANSPORT: NetHttpTransport): Credential? {
    val inputStream = CongressInternalSite::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH)
        ?: throw FileNotFoundException("Resource not found: $CREDENTIALS_FILE_PATH")
    val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(inputStream))

    // Build flow and trigger user authorization request.
    val flow = GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
        .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
        .setAccessType("offline")
        .build()

    val receiver = LocalServerReceiver.Builder().setPort(8888).build()
    return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
}

fun createEmail(
    to: String?,
    from: String?,
    subject: String?,
    bodyText: String?
): MimeMessage? {
    val props = Properties()
    val session: Session = Session.getDefaultInstance(props, null)
    val email = MimeMessage(session)
    email.setFrom(InternetAddress(from))
    email.addRecipient(
        javax.mail.Message.RecipientType.TO,
        InternetAddress(to)
    )
    email.subject = subject
    email.setText(bodyText)
    return email
}

fun createMessageWithEmail(emailContent: MimeMessage): Message? {
    val buffer = ByteArrayOutputStream()
    emailContent.writeTo(buffer)
    val bytes = buffer.toByteArray()
    val encodedEmail: String = encodeBase64URLSafeString(bytes)
    val message = Message()
    message.raw = encodedEmail
    return message
}

fun createEmailWithAttachment(
    to: String?,
    from: String?,
    subject: String?,
    bodyText: String?,
    file: File
): MimeMessage? {
    val props = Properties()
    val session = Session.getDefaultInstance(props, null)
    val email = MimeMessage(session)
    email.setFrom(InternetAddress(from))
    email.addRecipient(
        javax.mail.Message.RecipientType.TO,
        InternetAddress(to)
    )
    email.subject = subject
    var mimeBodyPart = MimeBodyPart()
    mimeBodyPart.setContent(bodyText, "text/plain")
    val multipart: Multipart = MimeMultipart()
    multipart.addBodyPart(mimeBodyPart)
    mimeBodyPart = MimeBodyPart()
    val source = FileDataSource(file)
    mimeBodyPart.dataHandler = DataHandler(source)
    mimeBodyPart.fileName = file.name
    multipart.addBodyPart(mimeBodyPart)
    email.setContent(multipart)
    return email
}

fun sendMessage(
    emailContent: MimeMessage?
): Message? {
    var message = createMessageWithEmail(emailContent!!)
    message = service.users().messages().send(userId, message).execute()
    return message
}
