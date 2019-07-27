package org.asarkar.greeting.server

import io.grpc.Server
import io.grpc.ServerBuilder
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class GreetingServer {
    private var port: Int = -1
    private lateinit var server: Server

    fun blockingStart() {
        port = ServerSocket(0).let {
            val tmp = it.localPort
            it.close()
            tmp
        }

        server = ServerBuilder.forPort(port)
            .addService(GreetingServiceImpl())
            .build()
        server.start()
        server.awaitTermination()
    }

    fun blockUntilReady(timeout: Int): Boolean {
        val start = Instant.now()
        var ready = false
        while (!ready && abs(Duration.between(Instant.now(), start).toMillis()) < timeout) {
            Socket().use { socket ->
                try {
                    socket.connect(InetSocketAddress("localhost", port), 100)
                    println("Server listening on port: ${server.port}")
                    ready = true
                } catch (ex: Exception) {
                }
            }
        }

        return ready
    }

    fun port(): Int {
        return port
    }

    fun stop() {
        server.shutdown()
            .awaitTermination(5, TimeUnit.SECONDS)
        println("Server shutdown complete")
    }
}