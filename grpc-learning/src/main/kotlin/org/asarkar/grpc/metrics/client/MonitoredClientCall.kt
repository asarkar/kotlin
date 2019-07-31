package org.asarkar.grpc.metrics.client

import io.grpc.ClientCall
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.micrometer.core.instrument.Timer
import org.asarkar.grpc.metrics.GrpcMethod
import org.asarkar.grpc.metrics.Micrometer

class MonitoredClientCall<Req, Resp>(private val delegate: ClientCall<Req, Resp>, private val grpcMethod: GrpcMethod) :
    ForwardingClientCall.SimpleForwardingClientCall<Req, Resp>(delegate) {
    override fun start(responseListener: Listener<Resp>, headers: Metadata?) {
        super.start(
            MonitoringClientCallListener(responseListener, grpcMethod, Timer.start(Micrometer.REGISTRY)),
            headers
        )
    }
}