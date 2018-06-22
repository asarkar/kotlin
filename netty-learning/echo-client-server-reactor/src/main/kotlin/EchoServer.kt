package org.abhijitsarkar.kotlin.reactor.echo

import io.netty.handler.codec.LineBasedFrameDecoder
import org.abhijitsarkar.kotlin.netty.loggerFor
import reactor.ipc.netty.NettyPipeline
import reactor.ipc.netty.tcp.TcpServer
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CountDownLatch

class EchoServer(private val port: Int, private val latch: CountDownLatch) {
    fun start() {
        val server = TcpServer.create { opts ->
            opts.port(port)
                    .afterChannelInit {
                        it.pipeline()
                                .addBefore(
                                        NettyPipeline.ReactiveBridge,
                                        "codec",
                                        LineBasedFrameDecoder(1024)
                                )
                    }
        }
        val ctx = server.newHandler { inbound, outbound ->
            outbound
                    // flush immediately, not in the future
                    .options { it.flushOnEach(false) }
                    .sendString(inbound.receive()
                            .asString(StandardCharsets.UTF_8)
                            .doOnNext {
                                LOGGER.info("Server received: {}.", it)
                                if (it == "bye") {
                                    latch.countDown()
                                }
                            }
                            .doOnError { LOGGER.error(it.message, it) }
                            // LineBasedFrameDecoder removes newline, so add it again for client side to receive
                            // individual strings
                            .map { "$it\n" }
                    )
        }
                .also { LOGGER.info("Listening on port: {}.", port) }
                .run { block(Duration.ofSeconds(30L)) }
        latch.await()

        LOGGER.info("Stopping server.")
        ctx!!.dispose()
    }

    companion object {
        val LOGGER = loggerFor<EchoServer>()
    }
}