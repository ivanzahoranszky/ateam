package iz.demo

import akka.actor.ActorSystem
import akka.actor.Props
import akka.stream.alpakka.slick.javadsl.SlickSession
import iz.demo.actor.ConnectionActor
import iz.demo.service.DbService
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun main() {

    val mainModule = module {
        single { ConfigFactory.load() }
        single { SlickSession.forConfig("slick-postgres") }
        single { ActorSystem.create("A_team_demo", get<Config>()) }
        single { DbService(get(), get()) }
        single(createdAtStart = true) {
            val actorSystem = get<ActorSystem>()
            actorSystem.actorOf(
                ConnectionActor.props(
                actorSystem.settings().config().getString("demo.host"),
                actorSystem.settings().config().getInt("demo.port"),
                get()))
        }
    }

    startKoin {
        modules(mainModule)
    }

}