package org.abhijitsarkar.kotlin.reactor.echo

import io.netty.handler.codec.LineBasedFrameDecoder
import org.abhijitsarkar.kotlin.netty.loggerFor
import reactor.core.publisher.Flux
import reactor.ipc.netty.NettyPipeline
import reactor.ipc.netty.tcp.TcpClient
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CountDownLatch


class EchoClient(private val port: Int, private val latch: CountDownLatch) {
    fun start() {
        val client = TcpClient.create { opts ->
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
        val ctx = client.newHandler { inbound, outbound ->
            outbound
                    // flush immediately, not in the future
                    .options { it.flushOnEach(false) }
                    .sendString(Flux.fromArray(arrayOf("netty\n", "hi\n", "hello\n", "bye\n")))
                    .then(inbound.receive()
                            .asString(StandardCharsets.UTF_8)
                            .doOnNext {
                                LOGGER.info("Client received: {}.", it)
                                if (it == "bye") {
                                    latch.countDown()
                                }
                            }
                            .doOnError { LOGGER.error(it.message, it) }
                            .then()
                    )
        }
                .run { block(Duration.ofSeconds(30L)) }
        latch.await()

        LOGGER.info("Stopping client.")
        ctx!!.dispose()
    }

    companion object {
        val LOGGER = loggerFor<EchoClient>()
    }
}