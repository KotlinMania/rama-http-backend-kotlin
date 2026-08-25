package io.github.kotlinmania.ramahttpbackend

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * Synchronously executes a suspending block for multiplatform tests.
 */
public fun <T> runSync(block: suspend () -> T): T {
    var capturedResult: Result<T>? = null
    block.startCoroutine(
        object : Continuation<T> {
            override val context: CoroutineContext = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                capturedResult = result
            }
        },
    )
    return capturedResult?.getOrThrow() ?: error("Coroutine suspended asynchronously in runSync")
}
