package ateam.demo

import akka.actor.ActorSystem
import akka.actor.Props
import ateam.demo.actor.ConnectionActor
import ateam.demo.service.DbService
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun main() {

    val mainModule = module {
        single { ConfigFactory.load() }
        single { ActorSystem.create("A_team_demo", get<Config>()) }
        single { DbService(get()) }
        single(createdAtStart = true) {
            val actorSystem = get<ActorSystem>()
            actorSystem.actorOf(Props.create(
                ConnectionActor::class.java,
                actorSystem.settings().config().getString("demo.host"),
                actorSystem.settings().config().getInt("demo.port"),
                DbService(actorSystem)))
        }
    }

    startKoin {
        modules(mainModule)
    }

}