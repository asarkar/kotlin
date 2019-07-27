package org.asarkar.greeting.client

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import org.asarkar.greeting.GreetingServiceGrpc
import org.asarkar.greeting.model.GreetRequest
import org.asarkar.greeting.model.GreetResponse
import org.asarkar.greeting.model.Greeting
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GreetingClient(port: Int) {
    private val channel: ManagedChannel = ManagedChannelBuilder.forAddress("localhost", port)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext()
        .build()
    private val blockingStub = GreetingServiceGrpc.newBlockingStub(channel)
        .withDeadlineAfter(1, TimeUnit.SECONDS)

    fun shutdown() {
        channel
            .shutdown()
            .awaitTermination(5, TimeUnit.SECONDS)
        println("Client shutdown complete")
    }

    fun greet(name: String): String {
        return blockingStub
            .greet(newGreetRequest(name))
            .result
    }

    fun greetManyTimes(name: String): List<String> {
        return blockingStub.greetManyTimes(newGreetRequest(name))
            .asSequence()
            .map(GreetResponse::getResult)
            .toList()
    }

    fun longGreet(names: List<String>): String {
        val result = StringBuilder()
        val latch = CountDownLatch(1)
        // streaming client call requires async stub
        val requestObserver = GreetingServiceGrpc.newStub(channel)
            .longGreet(object : StreamObserver<GreetResponse> {
                override fun onNext(value: GreetResponse) {
                    assert(result.isEmpty()) { "Server sent more than one response" }
                    result.append(value.result)
                }

                override fun onError(t: Throwable) {
                    TODO("not implemented")
                }

                override fun onCompleted() {
                    latch.countDown()
                }
            })

        names
            .map(this::newGreetRequest)
            .forEach(requestObserver::onNext)
        requestObserver.onCompleted()

        latch.await(1, TimeUnit.SECONDS)

        return result.toString()
    }

    fun greetEveryone(names: List<String>): List<String> {
        val result = mutableListOf<String>()
        val latch = CountDownLatch(1)
        // streaming client call requires async stub
        val requestObserver = GreetingServiceGrpc.newStub(channel)
            .greetEveryone(object : StreamObserver<GreetResponse> {
                override fun onNext(value: GreetResponse) {
                    result.add(value.result)
                }

                override fun onError(t: Throwable) {
                    TODO("not implemented")
                }

                override fun onCompleted() {
                    latch.countDown()
                }
            })

        names
            .map(this::newGreetRequest)
            .forEach(requestObserver::onNext)
        requestObserver.onCompleted()

        latch.await(1, TimeUnit.SECONDS)

        return result
    }

    private fun newGreetRequest(name: String): GreetRequest {
        val greeting = Greeting.newBuilder()
            .setName(name)
            .build()
        return GreetRequest.newBuilder()
            .setGreeting(greeting)
            .build()
    }
}