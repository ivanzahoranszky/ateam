package ateam.demo

import com.google.gson.Gson

val gson = Gson()

fun Message.toJson(): String {
    return gson.toJson(this)
}
