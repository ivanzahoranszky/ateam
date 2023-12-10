package iz.demo

import akka.actor.ActorSystem
import akka.actor.Props
import akka.stream.alpakka.slick.javadsl.SlickSession
import iz.demo.actor.ConnectionActor
import iz.demo.service.DbService
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import iz.demo.actor.DispatcherActor
import iz.demo.stream.Keepalive
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun main() {

    val mainModule = module {
        single { ConfigFactory.load() }
        single { SlickSession.forConfig("slick-postgres") }
        single { ActorSystem.create("A_team_demo", get<Config>()) }
        single { DbService(get(), get()) }
        single(named("ConnectionActor")) {
            get<ActorSystem>().actorOf(
                ConnectionActor.props(
                    get<ActorSystem>().settings().config().getString("demo.host"),
                    get<ActorSystem>().settings().config().getInt("demo.port")))
        }
        single(createdAtStart = true) {
            val keepalive = Keepalive(get<ActorSystem>().settings().config().getLong("demo.keepaliveIntervalInSec"), get(), get())
            keepalive.start()
            keepalive
        }
        single(createdAtStart = true) {
            get<ActorSystem>().actorOf(
                DispatcherActor.props(get(named("ConnectionActor")), get()))
        }
    }

    startKoin {
        modules(mainModule)
    }

}