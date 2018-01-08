package org.abhijitsarkar.kotlin.netty.echo

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.CharsetUtil
import org.abhijitsarkar.kotlin.netty.loggerFor

@ChannelHandler.Sharable
class EchoServerHandler : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        (msg as ByteBuf)
            .also {
                LOGGER.info("Server received: {}.", it.toString(CharsetUtil.UTF_8))
                ctx.write(it)
            }
    }

    @Throws(Exception::class)
    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
            .addListener(ChannelFutureListener.CLOSE)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        LOGGER.error(cause.message, cause)
        ctx.close()
    }

    companion object {
        val LOGGER = loggerFor<EchoServerHandler>()
    }
}