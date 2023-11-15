package ateam.demo

import akka.actor.ActorSystem
import io.cucumber.core.logging.LoggerFactory
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import org.junit.Assert
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CompletableFuture

class StepDefinitions {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val actorSystem = ActorSystem.create()

    private var publisher = Socket()
    private var subsciber = Socket()

    private var flow: CompletableFuture<Unit> = CompletableFuture.completedFuture(Unit)

    @Given("^the driver has been started")
    fun `the driver has been started`() {
        actorSystem.actorOf(ConnectionActor.props("localhost", 8888))
    }

    @When("^the publisher connects$")
    fun `the publisher connects`() {
//        flow = flow.thenCompose {
//            connect("PUBLISHER")
//        }

//        Thread.sleep(5000)
//        val socket = Socket()
//        socket.connect(InetSocketAddress("localhost", 8888))
//        val ist = BufferedReader(InputStreamReader(socket.getInputStream()))
//        val ost = OutputStreamWriter(socket.getOutputStream())
//        ost.write("PUBLISHER\n")
//        var line = ist.read()
//        println()
    }

    @When("^the subscriber connects$")
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
            subsciber.getOutputStream().write(message.toByteArray())
            subsciber.getOutputStream().flush()
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

    @And("^end$")
    fun end() {
        flow.join()
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
                        subsciber = result.getOrNull()!!
                    }
                    break
                }
            }
        }
    }

    private fun subscriberReads(message: String): CompletableFuture<Unit> {
        return CompletableFuture.supplyAsync {
            var line: String? = null
            while (null == line) {
                println("?= $message")
                line = runCatching {
                    BufferedReader(InputStreamReader(subsciber.getInputStream())).readLine()
                }.getOrNull()
                Thread.sleep(1000)
            }
            Assert.assertEquals(message, line)
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
            Assert.assertEquals(message, line)
        }
    }

}

fun main() {
    val actorSystem = ActorSystem.create()
    actorSystem.actorOf(ConnectionActor.props("localhost", 8888))
    Thread {
        Thread.sleep(5000)
        val publisher = Socket()
        publisher.connect(InetSocketAddress("localhost", 8888))
        val istP = BufferedReader(InputStreamReader(publisher.getInputStream()))
        val ostP = PrintWriter(publisher.getOutputStream())
        ostP.println("PUBLISHER"); ostP.flush()
        var lineP = istP.readLine()

        val subscriber = Socket()
        subscriber.connect(InetSocketAddress("localhost", 8888))
        val istS = BufferedReader(InputStreamReader(subscriber.getInputStream()))
        val ostS = PrintWriter(subscriber.getOutputStream())
        ostS.println("SUBSCRIBER"); ostS.flush()
        var lineS = istS.readLine()

        ostP.println("Hello Brother"); ostP.flush()
        lineS = istS.readLine()

        println()
    }.start()

    Thread.currentThread().join()
}