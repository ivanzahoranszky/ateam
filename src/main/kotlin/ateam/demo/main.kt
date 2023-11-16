package ateam.demo

import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory

fun main() {

    val config = ConfigFactory.load()

    val system = ActorSystem.create("A_team_demo", config)

    system.actorOf(Props.create(ConnectionActor::class.java,
        system.settings().config().getString("demo.host"),
        system.settings().config().getInt("demo.port"),
        DbService(system)
    ))

}