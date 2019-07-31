package org.asarkar.greeting.server

import io.grpc.Server
import io.grpc.netty.NettyServerBuilder
import org.asarkar.grpc.metrics.MonitoredExecutorService
import org.asarkar.grpc.metrics.server.MonitoredEventLoopGroup
import org.asarkar.grpc.metrics.server.MonitoringServerInterceptor
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

        val bossEventLoopGroup = MonitoredEventLoopGroup.create("greeting-nio-boss-ELG")
        val workerEventLoopGroup = MonitoredEventLoopGroup.create("greeting-nio-worker-ELG")

        server = NettyServerBuilder.forPort(port)
            .addService(GreetingServiceImpl())
            .intercept(MonitoringServerInterceptor())
            .bossEventLoopGroup(bossEventLoopGroup)
            .workerEventLoopGroup(workerEventLoopGroup)
            // The Executor executes the callbacks of the rpc. This frees up the EventLoop to continue processing data
            // on the connection. When a new message arrives from the network, it is read on the event loop, and then
            // propagated up the stack to the executor. The executor takes the messages and passes them to the
            // ServerCall.Listener that processes the data.
            // By default, gRPC uses a cached thread pool. However it is strongly recommended you provide your own
            // executor. The reason is that the default thread pool behaves badly under load, creating new threads
            // when the rest are busy.
            .executor(
                MonitoredExecutorService.createBoundedThreadPool(
                    "greeting",
                    MonitoredExecutorService.Site.SERVER
                )
            )
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