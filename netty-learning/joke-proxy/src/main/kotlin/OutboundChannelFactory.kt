package org.abhijitsarkar.kotlin.netty.joke

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import java.net.SocketAddress

/**
 * @author Abhijit Sarkar
 */
object OutboundChannelFactory {
    fun outboundChannel(remoteAddress: SocketAddress): (Channel) -> Channel = { inboundChannel ->
        Bootstrap()
            .channel(inboundChannel.javaClass)
            // reuse event loop
            .group(inboundChannel.eventLoop())
            .handler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {
                    ch.pipeline().apply {
                            addLast(HttpClientCodec())
                            addLast(HttpObjectAggregator(8192, true))
                            addLast(JokeProxyBackendHandler(inboundChannel))
                        }
                }
            })
            // read is initiated by backend handler
            .option(ChannelOption.AUTO_READ, false)
            // create new channel
            .connect(remoteAddress)
            .apply {
                addListener { f ->
                    if (f.isSuccess
                    )
                        inboundChannel.read()
                    else {
                        JokeProxyFrontendHandler.LOGGER.error(f.cause().message, f.cause())
                        closeChannels(inboundChannel)
                    }
                }
            }
            .channel()
    }
}