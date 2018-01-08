package org.abhijitsarkar.kotlin.netty.echo

import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import java.net.InetSocketAddress


/**
 * @author Abhijit Sarkar
 */
class EchoClient(private val host: String, private val port: Int) {

    @Throws(Exception::class)
    fun start() {
        val group = NioEventLoopGroup()
        try {
            Bootstrap()
                .group(group)
                .channel(NioSocketChannel::class.java)
                .remoteAddress(InetSocketAddress(host, port))
                .handler(EchoClientHandler())
                .connect().sync()
                .channel().closeFuture().sync()
        } finally {
            group.shutdownGracefully().sync()
        }
    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val host = args.getOrElse(0) { "localhost" }
            val port = args.getOrElse(1) { "8080" }.toInt()
            EchoClient(host, port).start()
        }
    }
}