package de.eschoenawa.aoasample.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

private const val READ_BUFFER_BYTES = 16 * 1024

class AoaChannel(private val transport: UsbTransport) : Closeable {

    private val writeLock = Mutex()
    private val closed = AtomicBoolean(false)

    // Blocking reads ignore cancellation; the reader lives outside the caller's scope and is unblocked by close().
    private val readScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun send(message: ByteArray) {
        check(!closed.get()) { "Channel closed" }
        val frame = MessageFraming.frame(message)
        writeLock.withLock { withContext(Dispatchers.IO) { transport.write(frame) } }
    }

    suspend fun runUntilDetached(
        awaitDetach: suspend () -> Unit,
        onMessage: suspend (ByteArray) -> Unit,
    ) {
        try {
            coroutineScope {
                val reader = readScope.launch { readLoop(onMessage) }
                val detach = launch { awaitDetach() }
                select {
                    reader.onJoin {}
                    detach.onJoin {}
                }
                detach.cancel()
            }
        } finally {
            close()
        }
    }

    private suspend fun readLoop(onMessage: suspend (ByteArray) -> Unit) {
        val decoder = MessageFrameDecoder()
        val buffer = ByteArray(READ_BUFFER_BYTES)
        while (!closed.get()) {
            val read = try {
                transport.read(buffer)
            } catch (streamClosed: IOException) {
                -1
            }
            if (read < 0) return
            if (read > 0) decoder.decode(buffer.copyOf(read)).forEach { onMessage(it) }
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        transport.close()
        readScope.cancel()
    }
}
