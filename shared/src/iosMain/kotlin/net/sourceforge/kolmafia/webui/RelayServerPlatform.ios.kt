package net.sourceforge.kolmafia.webui

/**
 * iOS Relay loopback — stub until Darwin/CIO server lands.
 * RelayRequest/decorate pipeline still works headlessly for tests via [RelayServer.handleBrowserRequest].
 */
actual object RelayServerPlatform {
    actual fun start(
        preferredPort: Int,
        minPort: Int,
        maxPort: Int,
        allowRemote: Boolean,
        onAccept: (ByteArray) -> ByteArray,
    ): Int? {
        // Mark as "running" on preferred/min port without binding — WebView uses synthetic handler path.
        // Full socket accept is JVM/Android for now.
        return if (preferredPort != 0) preferredPort else minPort
    }

    actual fun stop() {
        // no-op
    }
}
