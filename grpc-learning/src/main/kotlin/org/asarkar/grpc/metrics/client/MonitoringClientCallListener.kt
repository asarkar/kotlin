package org.asarkar.grpc.metrics.client

import io.grpc.ClientCall
import io.grpc.ForwardingClientCallListener
import io.grpc.Metadata
import io.grpc.Status
import io.micrometer.core.instrument.Timer
import org.asarkar.grpc.metrics.GrpcMethod
import org.asarkar.grpc.metrics.Micrometer

class MonitoringClientCallListener<Resp>(
    delegate: ClientCall.Listener<Resp>,
    private val grpcMethod: GrpcMethod,
    private val sample: Timer.Sample
) :
    ForwardingClientCallListener.SimpleForwardingClientCallListener<Resp>(delegate) {
    override fun onClose(status: Status, trailers: Metadata?) {
        super.onClose(status, trailers)
        sample.stop(
            Micrometer.REGISTRY.timer(
                "grpc.client.call",
                "serviceName", grpcMethod.serviceName,
                "methodName", grpcMethod.methodName,
                "type", grpcMethod.methodType,
                "status", status.code.name
            )
        )
    }
}