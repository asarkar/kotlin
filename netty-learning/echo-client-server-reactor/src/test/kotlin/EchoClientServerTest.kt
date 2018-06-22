package org.abhijitsarkar.kotlin.reactor.echo

import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test

class EchoClientServerTest {
    @Test
    fun testClientServer() {
        val port = findAvailableTcpPort()
        val latch = CountDownLatch(2)

        thread() { EchoServer(port, latch).start() }
        thread() { EchoClient(port, latch).start() }
        latch.await(2, TimeUnit.SECONDS)
    }

    private fun findAvailableTcpPort(): Int {
        ServerSocket(0).use {
            val port = it.localPort
            it.reuseAddress = true
            return port
        }
    }
}