package de.eschoenawa.aoasample.accessory.data

import android.os.ParcelFileDescriptor
import de.eschoenawa.aoasample.common.UsbTransport
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

internal class FileDescriptorTransport(private val descriptor: ParcelFileDescriptor) : UsbTransport {

    private val input = FileInputStream(descriptor.fileDescriptor)
    private val output = FileOutputStream(descriptor.fileDescriptor)

    override fun read(buffer: ByteArray): Int = try {
        input.read(buffer)
    } catch (streamClosedByDetach: IOException) {
        -1
    }

    override fun write(bytes: ByteArray) {
        output.write(bytes)
        output.flush()
    }

    override fun close() {
        descriptor.close()
    }
}
