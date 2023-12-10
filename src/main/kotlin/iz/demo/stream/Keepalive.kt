package iz.demo.stream

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Cancellable
import akka.stream.javadsl.Keep
import akka.stream.javadsl.Sink
import akka.stream.javadsl.Source
import java.time.Duration

class Keepalive(private val intervalInSec: Long, private val actorSystem: ActorSystem, private val connectionActor: ActorRef) {

    private var keepAlive: Cancellable? = null

    fun start() {
        keepAlive = Source.tick(Duration.ZERO, Duration.ofSeconds(intervalInSec), Unit)
            .wireTap {  }
            .toMat(Sink.foreach {
                actorSystem.eventStream().publish(KeepaliveMessage("__ping__\n")) }, Keep.left())
            .run(actorSystem)
    }

    fun stop() {
        keepAlive?.cancel() ?: actorSystem.log().error("Timer has not started yet")
    }

}

data class KeepaliveMessage(val keepAliveMessage: String)