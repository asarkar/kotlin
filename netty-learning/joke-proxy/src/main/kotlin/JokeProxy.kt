package org.abhijitsarkar.kotlin.netty.joke

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import joptsimple.OptionParser
import joptsimple.OptionSet
import joptsimple.OptionSpec
import org.abhijitsarkar.kotlin.netty.loggerFor
import java.net.InetSocketAddress
import java.net.SocketAddress

/**
 * @author Abhijit Sarkar
 */
internal fun closeChannels(vararg channels: Channel) {
    channels.forEach { it.close() }
}

internal fun Channel.hostPort() = this.remoteAddress().run {
    when (this) {
        is InetSocketAddress -> "${this.hostName}:${this.port}"
        else -> this.toString()
    }
}

object JokeProxy {
    private val LOGGER = loggerFor<JokeProxy>()

    fun start(remoteAddress: SocketAddress, localAddress: SocketAddress) {
        val parentGroup = NioEventLoopGroup(1)
        val childGroup = NioEventLoopGroup()

        try {
            ServerBootstrap()
                .group(parentGroup, childGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<Channel>() {
                    override fun initChannel(ch: Channel) {
                        with(ch.pipeline()) {
                            addLast(HttpServerCodec())
                            addLast(HttpObjectAggregator(8192, true))
                            addLast(JokeProxyFrontendHandler(OutboundChannelFactory.outboundChannel(remoteAddress)))
                        }
                    }
                })
                // read is initiated by frontend handler
                .childOption(ChannelOption.AUTO_READ, false)
                // block until ServerChannel is created
                .bind(localAddress).sync()
                .also {
                    LOGGER.info(
                        "proxying: {} to {}.",
                        it.channel().localAddress(),
                        remoteAddress
                    )
                    // block until ServerChannel is closed; this keep the server running
                    it.channel().closeFuture().sync()
                }
        } finally {
            listOf(parentGroup, childGroup).forEach { it.shutdownGracefully().syncUninterruptibly() }
        }
    }
}

fun main(args: Array<String>) {
    val cmdLineArgs = CmdLineArgs(args)
    JokeProxy.start(
        InetSocketAddress(cmdLineArgs.remoteHost, cmdLineArgs.remotePort),
        InetSocketAddress(cmdLineArgs.localPort)
    )
}

class CmdLineArgs(args: Array<String>) {
    private val opts: OptionSet
    private val localPortOpt: OptionSpec<Int>
    private val remotePortOpt: OptionSpec<Int>
    private val remoteHostOpt: OptionSpec<String>

    init {
        val parser = OptionParser()

        localPortOpt = parser
            .acceptsAll(listOf("local-port", "l"))
            .withRequiredArg().ofType(Int::class.java)
            .defaultsTo(8080)

        remotePortOpt = parser
            .acceptsAll(listOf("remote-port", "p"))
            .withRequiredArg().ofType(Int::class.java)
            .defaultsTo(80)

        remoteHostOpt = parser
            .acceptsAll(listOf("remote-host", "h"))
            .withRequiredArg().ofType(String::class.java)
            .describedAs("e.g. api.icndb.com")

        val helpOpt = parser
            .accepts("help")
            .forHelp()

        opts = parser.parse(*args)

        if (opts.has(helpOpt)
        ) {
            parser.printHelpOn(System.out)

            System.exit(0)
        }
    }

    val localPort: Int
        get() = opts.valueOf(localPortOpt)

    val remotePort: Int
        get() = opts.valueOf(remotePortOpt)

    val remoteHost: String
        get() = opts.valueOf(remoteHostOpt)
                ?: throw IllegalArgumentException("Missing required option --remote-host. Run with --help for usage.")
}

