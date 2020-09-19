package com.asarkar.kotlin

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.reflect.full.memberFunctions

class TestCoroutinesExceptionHandling {
    private suspend fun f2(block: () -> Unit) {
        block()
    }

    private var defaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    @BeforeEach
    fun beforeEach() {
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
    }

    @AfterEach
    fun afterEach() {
        Thread.setDefaultUncaughtExceptionHandler(defaultUncaughtExceptionHandler)
    }

    @InternalCoroutinesApi
    @ParameterizedTest
    @MethodSource("scopeProvider")
    @DisplayName("Exception in child launch even if caught cancels the parent")
    fun test1(scope: CoroutineScope) {
        var uncaught1: Throwable? = null
        Thread.setDefaultUncaughtExceptionHandler { _, ex -> uncaught1 = ex }
        val parent = scope.launch {
            var job: Job? = null
            var uncaught2: Throwable? = null
            try {
                job = launch { f2 { throw ArithmeticException() } }
                job.join()
            } catch (ex: Exception) {
                uncaught2 = ex
            } finally {
                Assertions.assertNotNull(job)
                Assertions.assertTrue(uncaught2 is ArithmeticException)
                Assertions.assertTrue(job!!.isCancelled)
                Assertions.assertTrue(job.isCompleted)
                Assertions.assertTrue(job.getCancellationException().cause is ArithmeticException)
            }
        }
        runBlocking { parent.join() }
        Assertions.assertTrue(parent.isCancelled)
        Assertions.assertTrue(parent.isCompleted)
        Assertions.assertTrue(parent.getCancellationException().cause is ArithmeticException)
        Assertions.assertTrue(uncaught1 is ArithmeticException)
    }

    @InternalCoroutinesApi
    @ParameterizedTest
    @MethodSource("scopeProvider")
    @DisplayName("Uncaught exception in child async can be handled in the parent")
    fun test4(scope: CoroutineScope) {
        var uncaught: Throwable? = null
        val exceptionHandler = CoroutineExceptionHandler { _, ex -> uncaught = ex }
        val parent = runBlocking {
            scope.launch(exceptionHandler) {
                val deferred = async { f2 { throw ArithmeticException() } }
                Assertions.assertFalse(deferred.isCancelled)
                Assertions.assertFalse(deferred.isCompleted)
                deferred.await()
                Assertions.fail("Shouldn't be here")
            }
        }
        runBlocking { parent.join() }
        Assertions.assertTrue(parent.isCancelled)
        Assertions.assertTrue(parent.isCompleted)
        Assertions.assertTrue(parent.getCancellationException().cause is ArithmeticException)
        Assertions.assertTrue(uncaught is ArithmeticException)
    }

    // async builder always catches all exceptions and represents them in the resulting Deferred object, so its
    // CoroutineExceptionHandler has no effect
    @ParameterizedTest
    @MethodSource("scopeProvider")
    @DisplayName("Installing CoroutineExceptionHandler in async builder has no effect")
    fun test6(scope: CoroutineScope) {
        var uncaught: Throwable? = null
        val exceptionHandler = CoroutineExceptionHandler { _, ex -> uncaught = ex }
        val deferred = scope.async(exceptionHandler) { f2 { throw ArithmeticException() } }
        Assertions.assertFalse(deferred.isCancelled)
        Assertions.assertFalse(deferred.isCompleted)
        try {
            runBlocking { deferred.await() }
        } catch (ex: ArithmeticException) {
            Assertions.assertTrue(deferred.isCancelled)
            Assertions.assertTrue(deferred.isCompleted)
            Assertions.assertNull(uncaught)
        }
    }

    @ParameterizedTest
    @MethodSource("scopeProvider")
    @DisplayName("Exception is not thrown until await is called on Deferred")
    fun test7(scope: CoroutineScope) {
        val deferred = scope.async { f2 { throw ArithmeticException() } }
        Assertions.assertFalse(deferred.isCancelled)
        Assertions.assertFalse(deferred.isCompleted)
    }

    @Test
    @DisplayName("runBlocking calls await implicitly on Deferred")
    fun test8() {
        var uncaught: Throwable? = null
        val exceptionHandler = CoroutineExceptionHandler { _, ex -> uncaught = ex }
        var deferred: Deferred<*>? = null

        try {
            deferred = runBlocking { async(exceptionHandler) { throw ArithmeticException() } }
            Assertions.fail("Shouldn't be here")
        } catch (ex: Exception) {
        } finally {
            Assertions.assertNull(deferred)
            Assertions.assertNull(uncaught)
        }
    }

    // CoroutineExceptionHandler is invoked only on uncaught exceptions - exceptions that were not handled in any other
    // way.
    @InternalCoroutinesApi
    @ParameterizedTest
    @MethodSource("scopeProvider")
    @DisplayName("CoroutineExceptionHandler is invoked only on uncaught exceptions")
    fun test9(scope: CoroutineScope) {
        var uncaught: Throwable? = null
        val exceptionHandler = CoroutineExceptionHandler { _, ex -> uncaught = ex }

        val job = scope.launch(exceptionHandler) {
            try {
                f2 { throw ArithmeticException() }
                Assertions.fail("Shouldn't be here")
            } catch (ex: ArithmeticException) {
            }
        }
        runBlocking { job.join() }
        Assertions.assertFalse(job.isCancelled)
        Assertions.assertTrue(job.isCompleted)
        Assertions.assertNull(job.getCancellationException().cause)
        Assertions.assertNull(uncaught)
    }

    // It does not make sense to install an exception handler to a coroutine that is launched in the scope of the main
    // runBlocking, since the main coroutine is going to be always cancelled when its child completes with exception
    // despite the installed handler.
    @Test
    @DisplayName("CoroutineExceptionHandler installed in a child of runBlocking has no effect")
    fun test10() {
        var uncaught: Throwable? = null
        val exceptionHandler = CoroutineExceptionHandler { _, ex -> uncaught = ex }
        var job: Job? = null
        try {
            job = runBlocking {
                launch(exceptionHandler) { f2 { throw ArithmeticException() } }
            }
            Assertions.fail("Shouldn't be here")
        } catch (ex: ArithmeticException) {
        } finally {
            Assertions.assertNull(job)
        }
        Assertions.assertNull(uncaught)
    }

    @Test
    @DisplayName("CoroutineExceptionHandler installed in runBlocking has no effect")
    fun test11() {
        var uncaught: Throwable? = null
        val exceptionHandler = CoroutineExceptionHandler { _, ex -> uncaught = ex }
        Assertions.assertThrows(ArithmeticException::class.java) {
            runBlocking(exceptionHandler) { f2 { throw ArithmeticException() } }
        }
        Assertions.assertNull(uncaught)
    }

    @ParameterizedTest
    @MethodSource("scopeProvider")
    @DisplayName("CoroutineExceptionHandler installed in launch works")
    fun test12(scope: CoroutineScope) {
        var uncaught: Throwable? = null
        val exceptionHandler = CoroutineExceptionHandler { _, ex -> uncaught = ex }
        val job = scope.launch(exceptionHandler) { f2 { throw ArithmeticException() } }
        Assertions.assertFalse(job.isCancelled)
        Assertions.assertFalse(job.isCompleted)
        runBlocking { job.join() }
        Assertions.assertTrue(uncaught is ArithmeticException)
    }

    @Test
    @DisplayName("How yield works")
    fun test13() {
        val messages = listOf(
            "Parent is running",
            "Child is running",
            "Cancelling child",
            "Child is cancelled",
            "Parent is not cancelled"
        )
        val out = mutableListOf<String>()
        runBlocking {
            val parent = launch {
                val child = launch {
                    try {
                        out.add(messages[1])
                        delay(Long.MAX_VALUE)
                    } finally {
                        out.add(messages[3])
                    }
                }
                // Without the first yield, "Child is running" is never printed since the child job doesn't get a chance
                // to run. delay suspends child execution and resumes parent execution. cancel interrupts the delay and
                // moves the execution into the finally block. The join and the second yield have no real effect, but by
                // calling join on the child job, we made absolutely sure that any following code is only executed once
                // the child is completed/cancelled.
                yield()
                out.add(messages[2])
                child.cancel()
                child.join()
                yield()
                out.add(messages[4])
            }
            out.add(messages[0])
            parent.join()
        }

        Assertions.assertEquals(messages, out)
    }

    // https://medium.com/@elizarov/the-reason-to-avoid-globalscope-835337445abc
    @InternalCoroutinesApi
    @ParameterizedTest
    @MethodSource("scopeProvider")
    @DisplayName("Uncaught exception in child async cancels sibling coroutines and parent")
    fun test15(scope: CoroutineScope) {
        var uncaught: Throwable? = null
        Thread.setDefaultUncaughtExceptionHandler { _, ex -> uncaught = ex }

        val jobs = mutableListOf<Job>()
        // runBlocking waits for the completion of its children.
        runBlocking {
            val child1: Deferred<*> = scope.async(CoroutineName("child1")) { throw ArithmeticException() }
            val child2 = scope.launch(CoroutineName("child2")) { child1.await() }
            val child3 = scope.launch(CoroutineName("child3")) { println() }

            jobs.add(child1)
            jobs.add(child2)
            jobs.add(child3)
        }

        jobs.forEach { job ->
            // job may not be completed until join is called
            runBlocking { job.join() }
            // Uncaught exception in GlobalScope doesn't cancel siblings
            Assertions.assertEquals(scope !is GlobalScope || jobName(job) != "child3", job.isCancelled)
            if (job.isCancelled) {
                Assertions.assertTrue(job.getCancellationException().cause is ArithmeticException)
            }
            Assertions.assertTrue(job.isCompleted)
        }

        val parentJob = scope.coroutineContext[Job]
        Assertions.assertEquals(scope !is GlobalScope, parentJob != null)
        parentJob?.also {
            Assertions.assertTrue(it.isCancelled)
            Assertions.assertTrue(it.isCompleted)
            Assertions.assertTrue(parentJob.getCancellationException().cause is ArithmeticException)
        }

        Assertions.assertEquals(scope is GlobalScope, uncaught != null)
    }

    @InternalCoroutinesApi
    @ParameterizedTest
    @MethodSource("scopeProvider")
    @DisplayName("CoroutineExceptionHandler installed in child launch has no effect")
    fun test16(scope: CoroutineScope) {
        var uncaught1: Throwable? = null
        val exceptionHandler = CoroutineExceptionHandler { _, ex -> uncaught1 = ex }
        var uncaught2: Throwable? = null
        Thread.setDefaultUncaughtExceptionHandler { _, ex -> uncaught2 = ex }
        val parent = scope.launch {
            val job = launch(exceptionHandler) { f2 { throw ArithmeticException() } }
            job.join()
            Assertions.assertTrue(job.isCancelled)
            Assertions.assertTrue(job.isCompleted)
            Assertions.assertTrue(job.getCancellationException().cause is ArithmeticException)
        }
        runBlocking { parent.join() }
        Assertions.assertTrue(parent.isCancelled)
        Assertions.assertTrue(parent.isCompleted)
        Assertions.assertNull(uncaught1)
        Assertions.assertTrue(uncaught2 is ArithmeticException)
        Assertions.assertTrue(parent.getCancellationException().cause is ArithmeticException)
    }

    @InternalCoroutinesApi
    @ParameterizedTest
    @MethodSource("scopeProvider")
    @DisplayName("Exception in child async even if caught cancels the parent")
    fun test17(scope: CoroutineScope) {
        var uncaught: Throwable? = null
        Thread.setDefaultUncaughtExceptionHandler { _, ex -> uncaught = ex }
        val parent = scope.launch {
            val deferred = async { f2 { throw ArithmeticException() } }
            try {
                deferred.await()
            } catch (ex: ArithmeticException) {
            }
            Assertions.assertTrue(deferred.isCancelled)
            Assertions.assertTrue(deferred.isCompleted)
            Assertions.assertTrue(deferred.getCancellationException().cause is ArithmeticException)
        }
        runBlocking { parent.join() }
        Assertions.assertTrue(parent.isCancelled)
        Assertions.assertTrue(parent.isCompleted)
        Assertions.assertTrue(uncaught is ArithmeticException)
        Assertions.assertTrue(parent.getCancellationException().cause is ArithmeticException)
    }

    @InternalCoroutinesApi
    @ParameterizedTest
    @MethodSource("scopeProvider")
    @DisplayName("Exception in child launch even if handled in the parent cancels it")
    fun test18(scope: CoroutineScope) {
        var uncaught: Throwable? = null
        val exceptionHandler = CoroutineExceptionHandler { _, ex -> uncaught = ex }
        val parent = scope.launch(exceptionHandler) {
            val job = launch { f2 { throw ArithmeticException() } }
            job.join()
            Assertions.assertTrue(job.isCancelled)
            Assertions.assertTrue(job.isCompleted)
            Assertions.assertTrue(job.getCancellationException().cause is ArithmeticException)
        }
        runBlocking { parent.join() }
        Assertions.assertTrue(parent.isCancelled)
        Assertions.assertTrue(parent.isCompleted)
        Assertions.assertTrue(uncaught is ArithmeticException)
        Assertions.assertTrue(parent.getCancellationException().cause is ArithmeticException)
    }

    @InternalCoroutinesApi
    @ParameterizedTest
    @MethodSource("scopeProvider")
    @DisplayName("Uncaught CancellationException in child async doesn't cancel sibling coroutines or parent")
    fun test11(scope: CoroutineScope) {
        var uncaught: Throwable? = null
        Thread.setDefaultUncaughtExceptionHandler { _, ex -> uncaught = ex }

        val jobs = mutableListOf<Job>()
        runBlocking {
            val child1: Deferred<*> = scope.async(CoroutineName("child1")) { throw CancellationException() }
            val child2 = scope.launch(CoroutineName("child2")) { child1.await() }
            val child3 = scope.launch(CoroutineName("child3")) { println() }

            jobs.add(child1)
            jobs.add(child2)
            jobs.add(child3)
        }

        jobs.forEach { job ->
            runBlocking { job.join() }
            val name = jobName(job)
            Assertions.assertEquals(name != "child3", job.isCancelled)
            if (job.isCancelled) {
                if (name == "child1") {
                    Assertions.assertNull(job.getCancellationException().cause)
                } else if (name == "child2") {
                    Assertions.assertTrue(job.getCancellationException().cause is CancellationException)
                }
            }
            Assertions.assertTrue(job.isCompleted)
        }

        val parentJob = scope.coroutineContext[Job]
        Assertions.assertEquals(scope !is GlobalScope, parentJob != null)
        parentJob?.also {
            Assertions.assertFalse(it.isCancelled)
            Assertions.assertFalse(it.isCompleted)
        }

        Assertions.assertNull(uncaught)
    }

    @Test
    @DisplayName("Should invoke global CoroutineExceptionHandler")
    fun test14() {
        var uncaught: Throwable? = null
        Thread.setDefaultUncaughtExceptionHandler { _, ex -> uncaught = ex }

        val job = GlobalScope.launch { throw ArithmeticException() }
        runBlocking { job.join() }
        Assertions.assertTrue(uncaught is ArithmeticException)
    }

    companion object {
        @JvmStatic
        fun scopeProvider(): Stream<Arguments> {
            return Stream.of(
                CoroutineScope(Job()),
                GlobalScope
            )
                .map(Arguments::of)
        }

        @InternalCoroutinesApi
        @Suppress("UNCHECKED_CAST")
        private val nameString = AbstractCoroutine::class.memberFunctions
            .single { it.name == "nameString" } as Function1<AbstractCoroutine<*>, String>

        @InternalCoroutinesApi
        private fun jobName(job: Job): String {
            return nameString(job as AbstractCoroutine<*>)
                .replace("\"", "")
                .takeWhile { it != '#' }
        }
    }
}