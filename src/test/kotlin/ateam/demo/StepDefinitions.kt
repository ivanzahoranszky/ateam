package ateam.demo

import akka.actor.ActorSystem
import io.cucumber.core.logging.LoggerFactory
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CompletableFuture

class StepDefinitions {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val actorSystem = ActorSystem.create()

    private var publisher = Socket()
    private var subscriber = Socket()

    private var flow: CompletableFuture<Unit> = CompletableFuture.completedFuture(Unit)

    @Given("^the driver has been started")
    fun `the driver has been started`() {
        actorSystem.actorOf(ConnectionActor.props("localhost", 8888))
    }

    @When("^the publisher connects$")
    fun `the publisher connects`() {
        flow = flow.thenCompose {
            connect("PUBLISHER")
        }
    }

    @And("^the subscriber connects$")
    fun `the subscriber connects`() {
        flow = flow.thenCompose {
            connect("SUBSCRIBER")
        }
    }

    @And("^the publisher sends \"(.+)\"$")
    fun `the publisher sends`(message: String) {
        flow = flow.thenApply {
            publisher.getOutputStream().write(message.toByteArray())
            publisher.getOutputStream().flush()
        }
    }

    @And("^the subscriber sends \"(.+)\"$")
    fun `the subscriber sends`(message: String) {
        flow = flow.thenApply {
            subscriber.getOutputStream().write(message.toByteArray())
            subscriber.getOutputStream().flush()
        }
    }

    @And("^the publisher receives \"(.+)\"$")
    fun `the publisher receives`(message: String) {
        flow = flow.thenCompose {
            log.info { "Waiting for $message" }
            publisherReads(message)
        }
    }

    @And("^the subscriber receives \"(.+)\"$")
    fun `the subscriber receives`(message: String) {
        flow = flow.thenCompose {
            log.info { "Waiting for $message" }
            subscriberReads(message)
        }
    }

    @Then("^the subscriber receives \"(.+)\" in JSON format$")
    fun `the subscriber receives in JSON format`(message: String) {
        flow = flow.thenCompose {
            val pattern = """^\{"payload":\{"key":"text","value":"$message"},"timeStamp":\d+\}$""".trimIndent().toRegex()
            log.info { "Waiting for $message" }
            subscriberReads(message, pattern)
        }
    }

    @And("^end$")
    fun end() {
        flow.join()
        publisher.close()
        subscriber.close()
    }

    private fun connect(clientType: String): CompletableFuture<Unit> {
        return CompletableFuture.supplyAsync {
            while (true) {
                val result = runCatching {
                    val socket = Socket()
                    socket.connect(InetSocketAddress("localhost", 8888))
                    socket
                }
                if (result.isFailure) {
                    Thread.sleep(100)
                } else {
                    if("PUBLISHER" == clientType)
                        publisher = result.getOrNull()!!
                    else {
                        subscriber = result.getOrNull()!!
                    }
                    break
                }
            }
        }
    }

    private fun subscriberReads(message: String, regex: Regex? = null): CompletableFuture<Unit> {
        return CompletableFuture.supplyAsync {
            var line: String? = null
            while (null == line) {
                line = runCatching {
                    BufferedReader(InputStreamReader(subscriber.getInputStream())).readLine()
                }.getOrNull()
                Thread.sleep(1000)
            }
            if (null == regex) {
                assertEquals(message, line)
            } else {
                assertTrue(regex.matches(line))
            }
        }
    }

    private fun publisherReads(message: String): CompletableFuture<Unit> {
        return CompletableFuture.supplyAsync {
            var line: String? = null
            while (null == line) {
                line =runCatching {
                    BufferedReader(InputStreamReader(publisher.getInputStream())).readLine()
                }.getOrNull()
            }
            assertEquals(message, line)
        }
    }

}
