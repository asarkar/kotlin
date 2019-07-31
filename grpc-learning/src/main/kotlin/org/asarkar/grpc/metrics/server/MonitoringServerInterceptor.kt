package org.asarkar.grpc.metrics.server

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.micrometer.core.instrument.Timer
import org.asarkar.grpc.metrics.GrpcMethod
import org.asarkar.grpc.metrics.Micrometer

class MonitoringServerInterceptor : ServerInterceptor {
    override fun <Req, Resp> interceptCall(
        call: ServerCall<Req, Resp>,
        headers: Metadata?,
        next: ServerCallHandler<Req, Resp>
    ): ServerCall.Listener<Req> {
        val grpcMethod = GrpcMethod(call.methodDescriptor)
        val monitoredServerCall = MonitoredServerCall(call, grpcMethod, Timer.start(Micrometer.REGISTRY))

        return next.startCall(monitoredServerCall, headers)
    }
}