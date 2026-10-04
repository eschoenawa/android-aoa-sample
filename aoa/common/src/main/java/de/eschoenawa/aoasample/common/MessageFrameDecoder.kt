package de.eschoenawa.aoasample.common

import de.eschoenawa.aoasample.common.MessageFraming.LENGTH_PREFIX_BYTES
import de.eschoenawa.aoasample.common.MessageFraming.MAX_MESSAGE_BYTES
import java.nio.ByteBuffer

class MessageFrameDecoder {
    private var pending = ByteArray(0)

    fun decode(chunk: ByteArray): List<ByteArray> {
        pending += chunk
        val messages = mutableListOf<ByteArray>()
        while (pending.size >= LENGTH_PREFIX_BYTES) {
            val messageSize = ByteBuffer.wrap(pending, 0, LENGTH_PREFIX_BYTES).int
            check(messageSize in 0..MAX_MESSAGE_BYTES) { "Invalid message size $messageSize" }
            val frameSize = LENGTH_PREFIX_BYTES + messageSize
            if (pending.size < frameSize) break
            messages += pending.copyOfRange(LENGTH_PREFIX_BYTES, frameSize)
            pending = pending.copyOfRange(frameSize, pending.size)
        }
        return messages
    }
}
