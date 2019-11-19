package org.asarkar.grpc.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.BaseUnits
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
            1, maximumPoolSize,
            60L, TimeUnit.SECONDS,
            SynchronousQueue()
        )
            .apply {
                val siteTag = listOf(Tag.of("site", site.name))
                ExecutorServiceMetrics(this, name, siteTag)
                    .bindTo(Micrometer.REGISTRY)

                val tags = Tags.concat(siteTag, "name", name)
                Gauge.builder("executor.corePool.size", this,
                    { tpe: ThreadPoolExecutor -> tpe.corePoolSize.toDouble() }
                )
                    .tags(tags)
                    .description("The core number of threads in the pool")
                    .baseUnit(BaseUnits.THREADS)
                    .register(Micrometer.REGISTRY)
                Gauge.builder("executor.maxPool.size", this,
                    { tpe: ThreadPoolExecutor -> tpe.maximumPoolSize.toDouble() }
                )
                    .tags(tags)
                    .description("The maximum number of threads in the pool")
                    .baseUnit(BaseUnits.THREADS)
                    .register(Micrometer.REGISTRY)
            }
    }

    fun destroy(executorService: ExecutorService) {
        executorService.shutdown()
        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            throw IllegalStateException("Could not terminate executor")
        }
    }
}