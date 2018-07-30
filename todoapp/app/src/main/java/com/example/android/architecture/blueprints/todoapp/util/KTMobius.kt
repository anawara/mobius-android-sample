package com.example.android.architecture.blueprints.todoapp.util

import com.spotify.mobius.Connection
import com.spotify.mobius.Next
import com.spotify.mobius.Update
import com.spotify.mobius.rx2.RxMobius
import io.reactivex.ObservableTransformer
import io.reactivex.Scheduler
import io.reactivex.functions.Consumer

private fun <M, E, F> updateWrapper(u: (M, E) -> Next<M, F>): Update<M, E, F> = Update { m: M, e: E -> u(m, e) }

fun <M, E, F> loopFactory(u: (M, E) -> Next<M, F>, fh: ObservableTransformer<F, E>) = RxMobius.loop(updateWrapper(u), fh)

class PartialConnection<T>(val onModelChange: (T) -> Unit) {
    fun onDispose(dispose: () -> Unit): Connection<T> = object : Connection<T> {
        override fun accept(value: T) {
            onModelChange(value)
        }

        override fun dispose() {
            dispose()
        }
    }
}

fun <T> onAccept(onModelChange: (T) -> Unit): PartialConnection<T> = PartialConnection(onModelChange)

class SubtypeEffectHandlerBuilder<F, E> {
    val builder: RxMobius.SubtypeEffectHandlerBuilder<F, E> = RxMobius.subtypeEffectHandler()

    inline fun <reified G : F> addTransformer(
            transformer: ObservableTransformer<G, E>) =
            apply { builder.addTransformer(G::class.java, transformer) }


    inline fun <reified G : F> addAction(
            crossinline block: () -> Unit,
            scheduler: Scheduler? = null) =
            apply {
                scheduler?.run { builder.addAction(G::class.java, { block() }, this) }
                        ?: builder.addAction(G::class.java) { block() }
            }

    inline fun <reified G : F> addConsumer(
            crossinline block: (G) -> Unit,
            scheduler: Scheduler? = null) =
            apply {
                scheduler?.run { builder.addConsumer(G::class.java, { block(it) }, this) }
                        ?: builder.addConsumer(G::class.java) { block(it) }
            }

    inline fun <reified G : F> addFunction(
            crossinline block: (G) -> E,
            scheduler: Scheduler? = null) =
            apply {
                scheduler?.run { builder.addFunction(G::class.java, { block(it) }, this) }
                        ?: builder.addFunction(G::class.java) { block(it) }
            }

    fun withFatalErrorHandler(block: (ObservableTransformer<out F, E>) -> Consumer<Throwable>) =
            apply { builder.withFatalErrorHandler { block(it) } }

    fun build(): ObservableTransformer<F, E> = builder.build()
}