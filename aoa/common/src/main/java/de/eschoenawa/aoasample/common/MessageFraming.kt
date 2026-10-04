package de.eschoenawa.aoasample.common

import java.nio.ByteBuffer

object MessageFraming {
    const val LENGTH_PREFIX_BYTES = Int.SIZE_BYTES
    const val MAX_MESSAGE_BYTES = 16 * 1024 - LENGTH_PREFIX_BYTES

    fun frame(message: ByteArray): ByteArray {
        require(message.size <= MAX_MESSAGE_BYTES) { "Message exceeds $MAX_MESSAGE_BYTES bytes" }
        return ByteBuffer.allocate(LENGTH_PREFIX_BYTES + message.size)
            .putInt(message.size)
            .put(message)
            .array()
    }
}
