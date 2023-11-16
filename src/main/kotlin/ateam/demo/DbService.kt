package ateam.demo

import akka.actor.ActorSystem
import akka.stream.alpakka.slick.javadsl.Slick
import akka.stream.alpakka.slick.javadsl.SlickSession
import akka.stream.javadsl.Keep
import akka.stream.javadsl.Source
import java.sql.PreparedStatement
import java.sql.Types


class DbService(private val actorSystem: ActorSystem) {

    fun storeMessage(message: Message) {
        val session = SlickSession.forConfig("slick-postgres")
        Source.single(message)
            .to(Slick.sink(session) { data, connection ->
                val statement: PreparedStatement = connection.prepareStatement(
                    "INSERT INTO messages (payload, timestamp) VALUES (?, ?)"
                )
                try {
                    statement.setObject(1, data.payload.toJsonString(), Types.OTHER)
                    statement.setLong(2, data.timeStamp)
                } catch (e: Exception) {
                    println(e.message)
                }
                statement
            }).run(actorSystem)
    }

}