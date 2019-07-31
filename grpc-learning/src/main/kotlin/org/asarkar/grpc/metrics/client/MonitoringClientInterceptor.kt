package org.asarkar.grpc.metrics.client

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.MethodDescriptor
import org.asarkar.grpc.metrics.GrpcMethod

class MonitoringClientInterceptor : ClientInterceptor {
    override fun <Req, Resp> interceptCall(
        method: MethodDescriptor<Req, Resp>,
        callOptions: CallOptions?,
        next: Channel
    ): ClientCall<Req, Resp> {
        val grpcMethod = GrpcMethod(method)

        return MonitoredClientCall(next.newCall(method, callOptions), grpcMethod)
    }
}