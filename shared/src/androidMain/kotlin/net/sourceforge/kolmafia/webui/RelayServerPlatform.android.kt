package net.sourceforge.kolmafia.webui

import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

actual object RelayServerPlatform {
    private var serverSocket: ServerSocket? = null
    private val running = AtomicBoolean(false)
    private var acceptThread: Thread? = null
    private var handler: ((ByteArray) -> ByteArray)? = null

    actual fun start(
        preferredPort: Int,
        minPort: Int,
        maxPort: Int,
        allowRemote: Boolean,
        onAccept: (ByteArray) -> ByteArray,
    ): Int? {
        stop()
        handler = onAccept
        val ports = if (preferredPort != 0) {
            listOf(preferredPort)
        } else {
            (minPort..maxPort).toList()
        }
        var bound: ServerSocket? = null
        var boundPort = -1
        for (p in ports) {
            try {
                bound = if (allowRemote) {
                    ServerSocket(p, 25)
                } else {
                    ServerSocket(p, 25, InetAddress.getByName("127.0.0.1"))
                }
                boundPort = p
                break
            } catch (_: Exception) {
            }
        }
        if (bound == null || boundPort < 0) return null
        serverSocket = bound
        running.set(true)
        acceptThread = thread(name = "LocalRelayServer", isDaemon = true) {
            while (running.get()) {
                try {
                    val socket = bound.accept()
                    thread(name = "LocalRelayAgent", isDaemon = true) {
                        handleSocket(socket)
                    }
                } catch (_: Exception) {
                    if (!running.get()) break
                }
            }
        }
        return boundPort
    }

    actual fun stop() {
        running.set(false)
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
        acceptThread = null
        handler = null
    }

    private fun handleSocket(socket: Socket) {
        try {
            socket.soTimeout = 30_000
            val input = BufferedInputStream(socket.getInputStream())
            val raw = readHttpRequest(input)
            val response = handler?.invoke(raw) ?: ByteArray(0)
            val out: OutputStream = socket.getOutputStream()
            out.write(response)
            out.flush()
        } catch (_: Exception) {
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun readHttpRequest(input: BufferedInputStream): ByteArray {
        val headerBytes = java.io.ByteArrayOutputStream()
        val headerBuf = StringBuilder()
        while (true) {
            val b = input.read()
            if (b < 0) break
            headerBytes.write(b)
            headerBuf.append(b.toChar())
            if (headerBuf.endsWith("\r\n\r\n") || headerBuf.endsWith("\n\n")) break
            if (headerBytes.size() > 1_000_000) break
        }
        val headerText = headerBytes.toString("UTF-8")
        val clMatch = Regex("""Content-Length:\s*(\d+)""", RegexOption.IGNORE_CASE).find(headerText)
        val contentLength = clMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        if (contentLength <= 0) return headerBytes.toByteArray()
        val body = ByteArray(contentLength)
        var read = 0
        while (read < contentLength) {
            val n = input.read(body, read, contentLength - read)
            if (n < 0) break
            read += n
        }
        return headerBytes.toByteArray() + body.copyOf(read)
    }
}
