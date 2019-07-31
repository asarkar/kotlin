package org.asarkar.grpc.metrics

import io.grpc.MethodDescriptor

class GrpcMethod(method: MethodDescriptor<*, *>) {
    val serviceName: String? = method.serviceName
        ?.takeLastWhile { it != '.' }
    val methodName: String? = method.fullMethodName.takeLastWhile { it != '/' }
    val methodType = method.type.name
}