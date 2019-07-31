package org.asarkar.grpc.metrics.server

import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.Status
import io.micrometer.core.instrument.Timer
import org.asarkar.grpc.metrics.GrpcMethod
import org.asarkar.grpc.metrics.Micrometer

class MonitoredServerCall<Req, Resp>(
    private val delegate: ServerCall<Req, Resp>,
    private val grpcMethod: GrpcMethod,
    private val sample: Timer.Sample
) :
    ForwardingServerCall.SimpleForwardingServerCall<Req, Resp>(delegate) {
    override fun close(status: Status, trailers: Metadata?) {
        super.close(status, trailers)
        sample.stop(
            Micrometer.REGISTRY.timer(
                "grpc.server.call",
                "serviceName", grpcMethod.serviceName,
                "methodName", grpcMethod.methodName,
                "type", grpcMethod.methodType,
                "status", status.code.name
            )
        )
    }
}