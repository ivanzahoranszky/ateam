package ateam.demo

import akka.actor.ActorSystem
import akka.stream.alpakka.slick.javadsl.*
import akka.stream.javadsl.*
import java.sql.PreparedStatement
import java.time.Instant


class DbService(private val actorSystem: ActorSystem) {

    init {
        val session = SlickSession.forConfig("slick-postgres")
        Source.single(Message(Payload("deviceType", "sensor"), Instant.now().toEpochMilli()))
            .to(Slick.sink(
                session
            ) { data, connection ->
                 val statement: PreparedStatement = connection.prepareStatement(
                     "INSERT INTO messages (payload, timestamp) VALUES (?, ?)"
                 )
                 statement.setObject(1, data.payload.toJsonString())
                 statement.setLong(2, data.timeStamp)
                 statement
            }).run(actorSystem)
    }

}