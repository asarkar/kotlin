package com.asarkar.kotlin

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class GlobalCoroutineExceptionHandler : CoroutineExceptionHandler,
    AbstractCoroutineContextElement(CoroutineExceptionHandler) {
    init {
        println("Global CoroutineExceptionHandler instantiated")
    }
    override fun handleException(context: CoroutineContext, exception: Throwable) {
        println("Global CoroutineExceptionHandler called from thread: ${Thread.currentThread().name}")
    }
}