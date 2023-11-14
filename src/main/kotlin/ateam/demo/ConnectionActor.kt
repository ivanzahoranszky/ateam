package ateam.demo

import akka.Done
import akka.actor.AbstractActorWithStash
import akka.stream.OverflowStrategy
import akka.stream.javadsl.*
import akka.stream.javadsl.Tcp.IncomingConnection
import akka.util.ByteString
import java.util.concurrent.CompletionStage

class ConnectionActor(
    private val host: String,
    private val port: Int): AbstractActorWithStash() {

    private companion object {
        const val QUEUE_SIZE = 100
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
                log.info("Tcp connection has been established from ${connection.remoteAddress().hostName}")
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
                data.utf8String().uppercase().startsWith(ClientType.PUB.toString()) -> registerPublisher(remotePort)
                data.utf8String().uppercase().startsWith(ClientType.SUB.toString()) -> registerSubscriber(remotePort)
                else -> sendMessage(remotePort, data)
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
        portMapping[remotePort] = portMapping[remotePort]?.copy(clientType = ClientType.PUB) ?: throw RuntimeException("Connection not found")
        log.info("Publisher connected from $remotePort")
    }

    private fun registerSubscriber(remotePort: Int) {
        portMapping[remotePort] = portMapping[remotePort]?.copy(clientType = ClientType.SUB) ?: throw RuntimeException("Connection not found")
        log.info("Subscriber connected from $remotePort")
    }

    private fun sendMessage(remotePort: Int, data: ByteString) {
        val myType = portMapping[remotePort]?.clientType ?: throw RuntimeException("Connection not found")
        if (myType == ClientType.SUB) { return }

        val myPort = portMapping[remotePort]?.connection?.remoteAddress()?.port ?: throw RuntimeException("Connection not found")
        portMapping
            .filter { it.value.clientType == ClientType.SUB }
            .filter { it.value.connection.remoteAddress().port != myPort }
            .map { it.value }
            .forEach { record -> record.queue.offer(data) }
    }

    private val runningState = receiveBuilder()
        .build()

    private enum class ClientType {
        PUB,
        SUB
    }

    private data class ConnectionRec(val connection: IncomingConnection, var clientType: ClientType?, val queue: SourceQueueWithComplete<ByteString>)

}

