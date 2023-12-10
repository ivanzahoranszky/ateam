package iz.demo.actor

import akka.actor.AbstractActorWithStash
import akka.actor.ActorRef
import akka.actor.Props
import iz.demo.model.InboundMessage
import iz.demo.model.OutboundMessage
import iz.demo.service.DbService

class DispatcherActor(
    private val connectionActor: ActorRef,
    private val dbService: DbService
): AbstractActorWithStash() {

    private val connectionIdToType = mutableMapOf<String, ClientType>()

    companion object {

        fun props(connectionActor: ActorRef, dbService: DbService): Props =
            Props.create(DispatcherActor::class.java, connectionActor, dbService)

    }

    override fun preStart() {
        super.preStart()

        context().system().eventStream().subscribe(self(), InboundMessage::class.java)
    }

    override fun createReceive(): Receive = runningState

    private val runningState: Receive = receiveBuilder()
        .match(InboundMessage::class.java) { message ->
            when (message.payload.trim()) {
                ClientType.PUBLISHER.toString() -> {
                    connectionIdToType[message.connectionId] = ClientType.PUBLISHER
                    connectionActor.tell(OutboundMessage(message.connectionId, "Hello PUBLISHER\n"), self)
                }
                ClientType.SUBSCRIBER.toString() -> {
                    connectionIdToType[message.connectionId] = ClientType.SUBSCRIBER
                    connectionActor.tell(OutboundMessage(message.connectionId, "Hello SUBSCRIBER\n"), self)
                }
                else -> {
                    connectionIdToType[message.connectionId]?.let { // we already know the type
                        if (it == ClientType.PUBLISHER) {
                            connectionIdToType.filter { entry ->
                                entry.value == ClientType.SUBSCRIBER
                                        && entry.key != message.connectionId // we don't send for ourselves
                            }
                            .forEach {
                                connectionActor.tell(OutboundMessage(it.key, message.payload), self)
                                dbService.storeMessage(message)
                            }
                        }
                    }
                }
            }
        }
        .build()

    private enum class ClientType {
        PUBLISHER,
        SUBSCRIBER
    }

}