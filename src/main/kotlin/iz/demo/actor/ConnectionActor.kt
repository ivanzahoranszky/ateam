package iz.demo.actor

import akka.Done
import akka.actor.AbstractActorWithStash
import akka.actor.Props
import akka.stream.OverflowStrategy
import akka.stream.javadsl.*
import akka.stream.javadsl.Tcp.IncomingConnection
import akka.util.ByteString
import iz.demo.model.InboundMessage
import iz.demo.model.OutboundMessage
import iz.demo.stream.KeepaliveMessage
import java.util.concurrent.CompletionStage

class ConnectionActor(
    private val host: String,
    private val port: Int
): AbstractActorWithStash() {

    companion object {

        private const val QUEUE_SIZE = 100

        fun props(host: String, port: Int): Props = Props.create(ConnectionActor::class.java, host, port)

    }

    private val portMapping = mutableMapOf<Int, ConnectionRec>()

    override fun createReceive(): Receive = runningState

    override fun preStart() {
        super.preStart()

        context().system().eventStream().subscribe(self(), OutboundMessage::class.java)
        context().system().eventStream().subscribe(self(), KeepaliveMessage::class.java)

        Tcp.get(context.system).bind(host, port)
            .to(Sink.foreach { connection ->
                val remotePort = connection.remoteAddress().port
                val sink: Sink<ByteString, CompletionStage<Done>> = createSink(remotePort)
                val source = createSource(connection)
                val handlerFlow = Flow.fromSinkAndSource(sink, source)
                connection.handleWith(handlerFlow, context.system)
            }).run(context.system)
    }

    private fun createSource(connection: IncomingConnection): Source<ByteString, SourceQueueWithComplete<ByteString>>? {
        val remotePort = connection.remoteAddress().port
        val source = Source.queue<ByteString>(QUEUE_SIZE, OverflowStrategy.backpressure()).async()
            .mapMaterializedValue { queue ->
                registerConnection(remotePort, connection, queue)
                queue
            }
        return source
    }

    private fun createSink(remotePort: Int): Sink<ByteString, CompletionStage<Done>> {
        val sink: Sink<ByteString, CompletionStage<Done>> = Sink.foreach { data ->
            context().system().eventStream().publish(InboundMessage(remotePort.toString(), data.utf8String()))
        }
        return sink
    }

    private fun registerConnection(remotePort: Int, connection: IncomingConnection, queue: SourceQueueWithComplete<ByteString>) {
        if (portMapping[remotePort] == null) {
            portMapping[remotePort] = ConnectionRec(connection, queue)
        }
    }

    private val runningState = receiveBuilder()
        .match(OutboundMessage::class.java) { message ->
            portMapping.filter { it.key.toString() == message.connectionId }
                .map { it.value }
                .firstOrNull()
                ?.queue
                ?.offer(ByteString.fromString(message.payload))
                ?: context.system.log().error("Missing outbound tcp queue")
        }
        .match(KeepaliveMessage::class.java) { message ->
            portMapping
                .map { it.value.queue }
                .map { it.offer(ByteString.fromString(message.keepAliveMessage)) }
        }
        .build()

    private data class ConnectionRec(val connection: IncomingConnection, val queue: SourceQueueWithComplete<ByteString>)


}

