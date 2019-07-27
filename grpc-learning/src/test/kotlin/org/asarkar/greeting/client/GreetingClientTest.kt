package org.asarkar.greeting.client

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSameSizeAs
import assertk.assertions.isEqualTo
import org.asarkar.greeting.server.GreetingServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
    }

    @AfterAll
    fun afterAll() {
        server.stop()
    }

    @BeforeEach
    fun beforeEach() {
        client = GreetingClient(server.port())
    }

    @AfterEach
    fun afterEach() {
        client.shutdown()
    }

    @Test
    fun testGreet() {
        assertThat(client.greet(NAME))
            .isEqualTo("Hello, $NAME")
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