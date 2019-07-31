package org.asarkar.grpc.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry

object Micrometer {
    val REGISTRY = SimpleMeterRegistry()
}