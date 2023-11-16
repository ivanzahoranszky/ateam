package ateam.demo

import akka.Done
import akka.actor.AbstractActorWithStash
import akka.actor.Props
import akka.stream.OverflowStrategy
import akka.stream.javadsl.*
import akka.stream.javadsl.Tcp.IncomingConnection
import akka.util.ByteString
import java.time.Instant
import java.util.concurrent.CompletionStage

class ConnectionActor(
    private val host: String,
    private val port: Int,
    private val dbService: DbService): AbstractActorWithStash() {

    companion object {

        private const val QUEUE_SIZE = 100
        fun props(host: String, port: Int, dbService: DbService): Props = Props.create(ConnectionActor::class.java, host, port,dbService)

    }

    private val log = context.system.log()

    private val portMapping = mutableMapOf<Int, ConnectionRec>()

    override fun createReceive(): Receive = runningState

    override fun preStart() {
        super.preStart()

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
            when {
                data.utf8String().uppercase().startsWith(ClientType.PUBLISHER.toString()) -> registerPublisher(remotePort)
                data.utf8String().uppercase().startsWith(ClientType.SUBSCRIBER.toString()) -> registerSubscriber(remotePort)
                else -> sendMessageToSubscribers(remotePort, data)
            }
        }
        return sink
    }

    private fun registerConnection(remotePort: Int, connection: IncomingConnection, queue: SourceQueueWithComplete<ByteString>) {
        if (portMapping[remotePort] == null) {
            portMapping[remotePort] = ConnectionRec(connection, null, queue)
        }
    }

    private fun registerPublisher(remotePort: Int) {
        portMapping[remotePort] = portMapping[remotePort]?.copy(clientType = ClientType.PUBLISHER) ?: throw RuntimeException("Connection not found")
        portMapping[remotePort]?.queue?.offer(ByteString.fromString("Hello PUBLISHER\n"))
        log.info("Publisher connected from $remotePort, Hello PUBLISHER sent")
    }

    private fun registerSubscriber(remotePort: Int) {
        portMapping[remotePort] = portMapping[remotePort]?.copy(clientType = ClientType.SUBSCRIBER) ?: throw RuntimeException("Connection not found")
        portMapping[remotePort]?.queue?.offer(ByteString.fromString("Hello SUBSCRIBER\n"))
        log.info("Subscriber connected from $remotePort, Hello SUBSCRIBER sent")
    }

    private fun sendMessageToSubscribers(remotePort: Int, data: ByteString) {
        val myClientType = portMapping[remotePort]?.clientType ?: throw RuntimeException("Connection not found")
        if (myClientType == ClientType.SUBSCRIBER) { return }

        portMapping
            .map { log.info("${it.value.clientType}: ${it.value.connection.remoteAddress()}"); it }
            .filter { it.value.clientType == ClientType.SUBSCRIBER }
            .map { it.value }
            .forEach { record ->
                val payload = Payload("text", data.utf8String().trim())
                val message = Message(payload, Instant.now().toEpochMilli())
                dbService.storeMessage(message)
                record.queue.offer(ByteString.fromString(message.toJsonString()))
                log.info("Message sent to ${record.connection.remoteAddress()} $message")
            }
    }

    private val runningState = receiveBuilder()
        .build()

    private enum class ClientType {
        PUBLISHER,
        SUBSCRIBER
    }

    private data class ConnectionRec(val connection: IncomingConnection, var clientType: ClientType?, val queue: SourceQueueWithComplete<ByteString>)

}

