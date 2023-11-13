package ateam.demo

import akka.Done
import akka.actor.AbstractActorWithStash
import akka.stream.OverflowStrategy
import akka.stream.javadsl.Flow
import akka.stream.javadsl.Sink
import akka.stream.javadsl.Source
import akka.stream.javadsl.SourceQueueWithComplete
import akka.stream.javadsl.Tcp
import akka.util.ByteString
import java.util.concurrent.CompletionStage

class ConnectionActor(private val host: String, private val port: Int): AbstractActorWithStash() {

    private val log = context.system.log()

    private val map = mutableMapOf<Int, ConnectionRec>()

    override fun createReceive(): Receive = runningState

    override fun preStart() {
        super.preStart()

        Tcp.get(context.system).bind(host, port)
            .to(Sink.foreach { connection ->
                val sink: Sink<ByteString, CompletionStage<Done>> = Sink.foreach {
                    if ( it.utf8String().startsWith(ClientType.pub.toString())) {
                        map[connection.remoteAddress().port] = map[connection.remoteAddress().port]!!.copy(clientType = ClientType.pub)
                        log.info("WE HAVE THE ____PUBLISHER____")
                    } else {
                        map[connection.remoteAddress().port] = map[connection.remoteAddress().port]!!.copy(clientType = ClientType.sub)
                        log.info("WE HAVE THE ____SUBSCRIBER____")
                    }
                }

                val source =  Source.queue<ByteString>(100, OverflowStrategy.backpressure()).async()
                    .mapMaterializedValue {
                        if (map[connection.remoteAddress().port] == null) {
                            map[connection.remoteAddress().port] = ConnectionRec(connection.remoteAddress().port, null, it)
                        }
                        it
                    }

                val handler = Flow.fromSinkAndSource(sink, source)
                connection.handleWith(handler, context.system)
                log.info("Tcp connection received from ${connection.remoteAddress().hostName}")
            }).run(context.system)
    }

    private val runningState = receiveBuilder()
        .build()

    enum class ClientType {
        pub,
        sub
    }

    data class ConnectionRec(val port: Int, var clientType: ClientType?, val queue: SourceQueueWithComplete<ByteString>)

}