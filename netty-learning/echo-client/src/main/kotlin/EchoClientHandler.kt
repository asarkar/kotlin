package org.abhijitsarkar.kotlin.netty.echo

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.util.CharsetUtil
import org.abhijitsarkar.kotlin.netty.loggerFor


/**
 * @author Abhijit Sarkar
 */
@Sharable
class EchoClientHandler : SimpleChannelInboundHandler<ByteBuf>() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.writeAndFlush(Unpooled.copiedBuffer("Netty rocks!", CharsetUtil.UTF_8))
    }

    public override fun channelRead0(ctx: ChannelHandlerContext, buf: ByteBuf) {
        LOGGER.info("Client received: {}.", buf.toString(CharsetUtil.UTF_8))
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        LOGGER.error(cause.message, cause)
        ctx.close()
    }

    companion object {
        val LOGGER = loggerFor<EchoClientHandler>()
    }
}