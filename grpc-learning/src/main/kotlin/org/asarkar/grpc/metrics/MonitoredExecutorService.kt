package org.asarkar.grpc.metrics

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
import java.util.concurrent.ExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


object MonitoredExecutorService {
    enum class Site {
        CLIENT, SERVER
    }

    fun createBoundedThreadPool(name: String, site: Site, maximumPoolSize: Int = 10): ExecutorService {
        return ThreadPoolExecutor(
            0, maximumPoolSize,
            60L, TimeUnit.SECONDS,
            SynchronousQueue()
        )
            .apply {
                ExecutorServiceMetrics(this, name, listOf(Tag.of("site", site.name)))
                    .bindTo(Micrometer.REGISTRY)
            }
    }

    fun destroy(executorService: ExecutorService) {
        executorService.shutdown()
        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            throw IllegalStateException("Could not terminate executor")
        }
    }
}