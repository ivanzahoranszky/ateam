package ateam.demo

import akka.actor.ActorSystem
import akka.stream.alpakka.slick.javadsl.*
import akka.stream.javadsl.*

class DbService(private val actorSystem: ActorSystem) {

    init {
        val session = SlickSession.forConfig("slick-postgres");
        println()
        Slick.source(session, "SELECT * FROM messages") {
            it.nextObject()
            Message(it.nextString(), it.nextLong())
        }.to(Sink.foreach {
            println(it)
        }).run(actorSystem)

    }

}