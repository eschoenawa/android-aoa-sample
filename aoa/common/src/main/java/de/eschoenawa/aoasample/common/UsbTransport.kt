package de.eschoenawa.aoasample.common

import java.io.Closeable

interface UsbTransport : Closeable {
    fun read(buffer: ByteArray): Int

    fun write(bytes: ByteArray)
}
