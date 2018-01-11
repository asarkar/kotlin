package org.abhijitsarkar.kotlin.netty.proxy

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpMessage
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import org.abhijitsarkar.kotlin.netty.loggerFor

/**
 * @author Abhijit Sarkar
 */
class ProxyFrontendHandler(
    private val outboundChannelSupplier: (Channel) -> Channel
) : SimpleChannelInboundHandler<FullHttpMessage>() {
    private lateinit var outboundChannel: Channel

    override fun channelActive(ctx: ChannelHandlerContext) {
        outboundChannel = outboundChannelSupplier(ctx.channel())
    }

    override fun channelRead0(
        ctx: ChannelHandlerContext,
        msg: FullHttpMessage
    ) {
        if (outboundChannel.isActive && msg is FullHttpRequest) {
            msg.apply {
                val hostPort = outboundChannel.hostPort()
                uri = "http://$hostPort${msg.uri()}"
                headers()
                    .set(HttpHeaderNames.HOST, "$hostPort")
            }
                .also { outboundChannel.writeAndFlush(it.retain()) }
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        closeChannels(ctx.channel(), outboundChannel)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        LOGGER.error(cause.message, cause)
        closeChannels(ctx.channel(), outboundChannel)
    }

    companion object {
        val LOGGER = loggerFor<ProxyFrontendHandler>()
    }
}