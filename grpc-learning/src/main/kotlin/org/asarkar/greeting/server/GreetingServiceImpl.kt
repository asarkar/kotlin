package org.asarkar.greeting.server

import io.grpc.stub.StreamObserver
import org.asarkar.greeting.GreetingServiceGrpc
import org.asarkar.greeting.model.GreetRequest
import org.asarkar.greeting.model.GreetResponse

class GreetingServiceImpl : GreetingServiceGrpc.GreetingServiceImplBase() {
    override fun greet(request: GreetRequest, responseObserver: StreamObserver<GreetResponse>) {
        val response = GreetResponse.newBuilder()
            .setResult("Hello, ${request.greeting.name}")
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun greetManyTimes(request: GreetRequest, responseObserver: StreamObserver<GreetResponse>) {
        (1..3)
            .forEach { i ->
                val response = GreetResponse.newBuilder()
                    .setResult("Hello, ${request.greeting.name}[$i]")
                    .build()
                responseObserver.onNext(response)
            }
        responseObserver.onCompleted()
    }

    override fun longGreet(responseObserver: StreamObserver<GreetResponse>): StreamObserver<GreetRequest> {
        return object : StreamObserver<GreetRequest> {
            val result = mutableListOf<String>()
            override fun onError(t: Throwable) {
                TODO("not implemented")
            }

            override fun onCompleted() {
                responseObserver
                    .onNext(
                        GreetResponse.newBuilder()
                            .setResult(result.joinToString(separator = "! "))
                            .build()
                    )
                responseObserver.onCompleted()
            }

            override fun onNext(request: GreetRequest) {
                result.add("Hello, ${request.greeting.name}")
            }
        }
    }

    override fun greetEveryone(responseObserver: StreamObserver<GreetResponse>): StreamObserver<GreetRequest> {
        return object : StreamObserver<GreetRequest> {
            override fun onError(t: Throwable) {
                TODO("not implemented")
            }

            override fun onCompleted() {
                responseObserver.onCompleted()
            }

            override fun onNext(request: GreetRequest) {
                responseObserver
                    .onNext(
                        GreetResponse.newBuilder()
                            .setResult("Hello, ${request.greeting.name}")
                            .build()
                    )
            }
        }
    }
}