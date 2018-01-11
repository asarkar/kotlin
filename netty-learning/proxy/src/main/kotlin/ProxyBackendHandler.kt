package org.abhijitsarkar.kotlin.netty.proxy

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpMessage
import io.netty.handler.codec.http.FullHttpResponse
import org.abhijitsarkar.kotlin.netty.Slf4jLoggingHandler
import org.abhijitsarkar.kotlin.netty.loggerFor

/**
 * @author Abhijit Sarkar
 */
class ProxyBackendHandler(private val outboundChannel: Channel) : SimpleChannelInboundHandler<FullHttpMessage>() {
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        LOGGER.error(cause.message, cause)
        closeChannels(ctx.channel(), outboundChannel)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        closeChannels(ctx.channel(), outboundChannel)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        // need to delay this until channel is active otherwise connect error
        ctx.channel().pipeline().addFirst(Slf4jLoggingHandler)
        ctx.read()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: FullHttpMessage) {
        (msg as FullHttpResponse)
            .also {
                outboundChannel.writeAndFlush(it.retain())
                    .addListener {
                        closeChannels(ctx.channel(), outboundChannel)
                    }
            }
    }

    companion object {
        val LOGGER = loggerFor<ProxyBackendHandler>()
    }
}