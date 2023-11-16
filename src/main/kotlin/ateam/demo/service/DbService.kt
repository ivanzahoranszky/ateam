package ateam.demo.service

import akka.actor.ActorSystem
import akka.stream.alpakka.slick.javadsl.Slick
import akka.stream.alpakka.slick.javadsl.SlickSession
import akka.stream.javadsl.Source
import ateam.demo.model.Message
import ateam.demo.model.toJsonString
import java.sql.PreparedStatement
import java.sql.Types


class DbService(private val actorSystem: ActorSystem, private val session: SlickSession) {

    fun storeMessage(message: Message) {
        Source.single(message)
            .to(Slick.sink(session) { data, connection ->
                val statement: PreparedStatement = connection.prepareStatement(
                    "INSERT INTO messages (payload, timestamp) VALUES (?, ?)"
                )
                statement.setObject(1, data.payload.toJsonString(), Types.OTHER)
                statement.setLong(2, data.timeStamp)
                statement
            }).run(actorSystem)
    }

}