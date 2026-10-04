package de.eschoenawa.aoasample.host.domain

data class UsbDeviceIdentity(val deviceName: String, val vendorId: Int, val productId: Int) {
    override fun toString() = "%04X:%04X %s".format(vendorId, productId, deviceName)
}
