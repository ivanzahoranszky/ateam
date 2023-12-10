package iz.demo.model

abstract class Message(open val connectionId: String, open val payload: String)

data class InboundMessage(override val connectionId: String, override val payload: String): Message(connectionId, payload)

data class OutboundMessage(override val connectionId: String, override val payload: String): Message(connectionId, payload)