package de.eschoenawa.aoasample.host.domain

data class HostIdentity(
    val manufacturer: String,
    val model: String,
    val description: String,
    val version: String,
    val uri: String,
    val serial: String,
) {
    fun inAoaStringIndexOrder() = listOf(manufacturer, model, description, version, uri, serial)
}
