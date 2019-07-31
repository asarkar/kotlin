package org.asarkar.greeting.client

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSameSizeAs
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotNull
import io.grpc.MethodDescriptor.MethodType.UNARY
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micrometer.core.instrument.Timer
import org.asarkar.greeting.server.GreetingServer
import org.asarkar.grpc.metrics.Micrometer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import kotlin.concurrent.thread


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GreetingClientTest {
    private lateinit var client: GreetingClient
    private lateinit var server: GreetingServer

    @BeforeAll
    fun beforeAll() {
        server = GreetingServer()
        thread(block = { server.blockingStart() })

        assert(server.blockUntilReady(2000)) { "Server is not ready" }
        client = GreetingClient(server.port())
    }

    @AfterAll
    fun afterAll() {
        client.shutdown()
        server.stop()
    }

    @Test
    fun testGreet() {
        assertThat(client.greet(NAME))
            .isEqualTo("Hello, $NAME")
    }

    @Test
    fun testGreetError() {
        assertThrows<StatusRuntimeException> { client.greet("") }

    }

    @Test
    fun testGreetMetrics() {
        client.greet(NAME)
        client.greet(NAME)

        val clientTimer = getTimer("grpc.client.call")
        assertThat(clientTimer).isNotNull()
        assertThat(clientTimer!!.count()).isGreaterThanOrEqualTo(2L)
        val serverTimer = getTimer("grpc.server.call")
        assertThat(serverTimer).isNotNull()
        assertThat(serverTimer!!.count()).isGreaterThanOrEqualTo(2L)

        val gauges = Micrometer.REGISTRY
            .get("executor.pool.size")
            .gauges()
            .groupBy { it.id.getTag("name") }
            .mapValues { it.value.firstOrNull() }
        val clientExecutorGauge = gauges["greeting-client"]
        assertThat(clientExecutorGauge).isNotNull()
        val serverExecutorGauge = gauges["greeting"]
        assertThat(serverExecutorGauge).isNotNull()
        assertThat(serverExecutorGauge!!.value().toInt()).isEqualTo(1)
        val bossElgGauge = gauges["greeting-nio-boss-ELG"]
        assertThat(bossElgGauge).isNotNull()
        assertThat(bossElgGauge!!.value().toInt()).isEqualTo(1)
        val workerElgGauge = gauges["greeting-nio-worker-ELG"]
        assertThat(workerElgGauge).isNotNull()
        assertThat(workerElgGauge!!.value().toInt()).isEqualTo(1)
    }

    private fun getTimer(name: String): Timer? {
        return Micrometer.REGISTRY
            .get(name)
            .timers().firstOrNull {
                it.id.getTag("serviceName").equals(
                    "GreetingService",
                    true
                ) &&
                        it.id.getTag("methodName").equals(
                            "greet",
                            true
                        ) &&
                        it.id.getTag("status") == Status.OK.code.name &&
                        it.id.getTag("type") == UNARY.name
            }
    }

    @Test
    fun testGreetManyTimes() {
        client.greetManyTimes(NAME)
            .forEach { it.matches("Hello, $NAME[\\d]".toRegex()) }
    }

    @Test
    fun testLongGreet() {
        val names = (1..3).map { "$NAME[$it]" }
        val greetings = client.longGreet(names)
            .split("!")
            .map { it.trim() }
        assertThat(greetings).hasSameSizeAs(names)
        names
            .map { "Hello, $it" }
            .forEach { assertThat(greetings).contains(it) }
    }

    @Test
    fun testGreetEveryone() {
        val names = (1..3).map { "$NAME[$it]" }
        val greetings = client.greetEveryone(names)
        assertThat(greetings).hasSameSizeAs(names)
        names
            .map { "Hello, $it" }
            .forEach { assertThat(greetings).contains(it) }
    }

    companion object {
        const val NAME = "John"
    }
}