package ateam.demo

import akka.util.ByteString
import com.google.gson.Gson
import java.time.Instant

val gson = Gson()
fun ByteString.toJson() =
    ByteString.fromString(Message(gson.toJson(utf8String()), Instant.now()).toString())