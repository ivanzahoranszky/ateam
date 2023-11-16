package ateam.demo

import akka.util.ByteString
import com.google.gson.Gson
import java.time.Instant

val gson = Gson()

fun ByteString.toPayload(): ByteString {
    val payload = gson.fromJson(utf8String(), Payload::class.java)
    val message = Message(payload, Instant.now().toEpochMilli())
    val str = gson.toJson(message)
    return ByteString.fromString(str + "\n")
}

fun String.toPayload(): Payload {
    return gson.fromJson(this, Payload::class.java)
}

fun Payload.toJosn(): String = gson.toJson(this)