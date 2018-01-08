package org.abhijitsarkar.kotlin.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.handler.logging.LoggingHandler
import java.net.SocketAddress

/**
 * @author Abhijit Sarkar
 */
object Slf4jLoggingHandler : LoggingHandler() {
    private val LOGGER = loggerFor<Slf4jLoggingHandler>()

    override fun channelRegistered(ctx: ChannelHandlerContext) {
        if (LOGGER.isInfoEnabled
        ) {
            LOGGER.info(format(ctx, "REGISTERED"))
        }

        ctx.fireChannelRegistered()
    }

    override fun channelUnregistered(ctx: ChannelHandlerContext) {
        if (LOGGER.isInfoEnabled
        ) {
            LOGGER.info(format(ctx, "UNREGISTERED"))
        }

        ctx.fireChannelUnregistered()
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        if (LOGGER.isInfoEnabled
        ) {
            LOGGER.info(format(ctx, "ACTIVE"))
        }

        ctx.fireChannelActive()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        if (LOGGER.isInfoEnabled
        ) {
            LOGGER.info(format(ctx, "INACTIVE"))
        }

        ctx.fireChannelInactive()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        LOGGER.error(format(ctx, "EXCEPTION", cause), cause)

        ctx.fireExceptionCaught(cause)
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (LOGGER.isInfoEnabled
        ) {
            LOGGER.info(format(ctx, "USER_EVENT", evt))
        }

        ctx.fireUserEventTriggered(evt)
    }

    override fun bind(ctx: ChannelHandlerContext, localAddress: SocketAddress, promise: ChannelPromise) {
        if (LOGGER.isInfoEnabled
        ) {
            LOGGER.info(format(ctx, "BIND", localAddress))
        }

        ctx.bind(localAddress, promise)
    }

    override fun connect(
        ctx: ChannelHandlerContext,
        remoteAddress: SocketAddress, localAddress: SocketAddress, promise: ChannelPromise
    ) {
        if (LOGGER.isInfoEnabled
        ) {
            LOGGER.info(format(ctx, "CONNECT", remoteAddress, localAddress))
        }

        ctx.connect(remoteAddress, localAddress, promise)
    }

    override fun disconnect(ctx: ChannelHandlerContext, promise: ChannelPromise) {
        if (LOGGER.isInfoEnabled
        ) {
            LOGGER.info(format(ctx, "DISCONNECT"))
        }

        ctx.disconnect(promise)
    }

    override fun close(ctx: ChannelHandlerContext, promise: ChannelPromise) {
        if (LOGGER.isInfoEnabled
        ) {
            LOGGER.info(format(ctx, "CLOSE"))
        }

        ctx.close(promise)
    }

    override fun deregister(ctx: ChannelHandlerContext, promise: ChannelPromise) {
        if (LOGGER.isInfoEnabled
        ) {
            LOGGER.info(format(ctx, "DEREGISTER"))
        }

        ctx.deregister(promise)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        if (LOGGER.isDebugEnabled
        ) {
            LOGGER.debug(format(ctx, "READ COMPLETE"))
        }

        ctx.fireChannelReadComplete()
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (LOGGER.isDebugEnabled
        ) {
            LOGGER.debug(format(ctx, "READ", msg))
        }

        ctx.fireChannelRead(msg)
    }

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        if (LOGGER.isDebugEnabled
        ) {
            LOGGER.debug(format(ctx, "WRITE", msg))
        }

        ctx.write(msg, promise)
    }

    override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
        if (LOGGER.isDebugEnabled
        ) {
            LOGGER.debug(format(ctx, "WRITABILITY CHANGED"))
        }

        ctx.fireChannelWritabilityChanged()
    }

    override fun flush(ctx: ChannelHandlerContext) {
        if (LOGGER.isDebugEnabled
        ) {
            LOGGER.debug(format(ctx, "FLUSH"))
        }

        ctx.flush()
    }
}