package org.abhijitsarkar.kotlin.netty.echo

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.abhijitsarkar.kotlin.netty.loggerFor
import java.net.InetSocketAddress

class EchoServer(private val host: String, private val port: Int) {
    @Throws(Exception::class)
    fun start() {
        val group = NioEventLoopGroup()
        try {
            ServerBootstrap()
                .group(group)
                .channel(NioServerSocketChannel::class.java)
                .localAddress(InetSocketAddress(host, port))
                .childHandler(EchoServerHandler())
                .bind().sync()
                .also {
                    LOGGER.info(
                        "{} listening for connections on: {}.",
                        EchoServer::class.java.simpleName,
                        it.channel().localAddress()
                    )
                    it.channel().closeFuture().sync()
                }
        } finally {
            group.shutdownGracefully().sync()
        }
    }

    companion object {
        val LOGGER = loggerFor<EchoServer>()

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val host = args.getOrElse(0) { "localhost" }
            val port = args.getOrElse(1) { "8080" }.toInt()

            EchoServer(host, port).start()
        }
    }
}
