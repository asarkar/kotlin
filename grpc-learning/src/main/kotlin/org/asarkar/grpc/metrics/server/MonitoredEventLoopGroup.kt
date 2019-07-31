package org.asarkar.grpc.metrics.server

import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import org.asarkar.grpc.metrics.Micrometer
import org.asarkar.grpc.metrics.MonitoredExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// Based on io.grpc.netty.Utils.DefaultEventLoopGroupResource with some modifications
object MonitoredEventLoopGroup {
    fun create(name: String): EventLoopGroup {
        val executor = Executors.newSingleThreadExecutor(
            ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat(name)
                .build()
        ).apply {
            // https://github.com/chrishantha/netty-metrics/blob/master/netty-micrometer-metrics/src/main/java/com/github/chrishantha/netty/metrics/micrometer/NettyHttpServer.java
            // https://dzone.com/articles/java-tips-creating-a-monitoring-friendly-executors
            // https://github.com/netty/netty/issues/4981
            // https://github.com/micrometer-metrics/micrometer/issues/522
            ExecutorServiceMetrics(this, name, listOf(Tag.of("site", MonitoredExecutorService.Site.SERVER.name)))
                .bindTo(Micrometer.REGISTRY)
        }
        val eventLoopGroup = NioEventLoopGroup(1, executor)
        eventLoopGroup.terminationFuture()
            .addListener {
                executor.shutdown()
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    throw IllegalStateException("Could not terminate executor")
                }
            }
        return eventLoopGroup
    }
}