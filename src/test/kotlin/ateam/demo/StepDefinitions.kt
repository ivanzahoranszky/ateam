package ateam.demo

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
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

    private val actorSystem = ActorSystem.create()

    private var publisher: Socket? = null
    private var subsciber: Socket? = null

    @Given("the driver has been started")
    fun `the driver has been started`() {
        actorSystem.actorOf(ConnectionActor.props("localhost", 8888))
    }

    @When("the publisher connects and gets \"(.+)\"$")
    fun `the publisher connects and gets`(result: String) {
        Thread.sleep(1000)
        publisher = Socket()
        connect(publisher!!, "pub", result)

//        connect(publisher!!, "pub", result)
    }

    @When("the subscriber connects and gets \"(.+)\"$")
    fun `the subscriber connects`(result: String) {
        subsciber = Socket()
        connect(subsciber!!, "pub", result)
    }

    @And("the publisher sends \"(.+)\"$")
    fun `the publisher sends `(message: String) {
//        publisher = Socket()
//        publisher!!.connect(InetSocketAddress("localhost", 8888))
    }

    private fun connect(socket: Socket, clientType: String, result: String) {
        CompletableFuture.supplyAsync {
            var line: String? = null
            while (null == line) {
                runCatching {
                    socket.connect(InetSocketAddress("localhost", 8888))
                    socket.getOutputStream().write("$clientType\n".toByteArray())
                    socket.getOutputStream().flush()
                    line = BufferedReader(InputStreamReader(publisher!!.getInputStream())).readLine()
                }
                Thread.sleep(100)
            }
            line
        }
            .thenAccept{
                assertEquals(result, it)
            }.join()
    }


}