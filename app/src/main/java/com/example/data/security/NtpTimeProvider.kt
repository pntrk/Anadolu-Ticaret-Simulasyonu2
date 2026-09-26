package com.example.data.security

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * High-Reliability Multi-Source SNTP & HTTPS Time Provider.
 *
 * Queries global NTP servers (Google NTP, Cloudflare NTP, NTP Pool) over UDP port 123,
 * with graceful fallback to secure HTTPS Date headers.
 * Protects the game from clock-tampering, offline cheating, and time-travel attacks.
 */
object NtpTimeProvider {

    private const val TAG = "NtpTimeProvider"
    private const val NTP_PORT = 123
    private const val TIMEOUT_MS = 3000

    private val NTP_SERVERS = listOf(
        "time.google.com",
        "time.cloudflare.com",
        "pool.ntp.org",
        "time.android.com"
    )

    private val HTTPS_FALLBACK_URLS = listOf(
        "https://www.google.com",
        "https://www.cloudflare.com"
    )

    /**
     * Attempts to fetch high-precision network time in milliseconds.
     * Tries UDP SNTP first across multiple hosts, then falls back to HTTPS Date headers.
     */
    suspend fun fetchNetworkTimeMs(): Long? = withContext(Dispatchers.IO) {
        // 1. Try NTP Servers
        for (server in NTP_SERVERS) {
            try {
                val ntpTime = querySntp(server)
                if (ntpTime != null && ntpTime > 0L) {
                    Log.d(TAG, "Successfully synchronized with NTP server: $server ($ntpTime)")
                    return@withContext ntpTime
                }
            } catch (e: Exception) {
                Log.d(TAG, "NTP query failed for $server: ${e.message}")
            }
        }

        // 2. Fallback to HTTPS Date headers
        for (urlStr in HTTPS_FALLBACK_URLS) {
            try {
                val httpsTime = queryHttpsDate(urlStr)
                if (httpsTime != null && httpsTime > 0L) {
                    Log.d(TAG, "Successfully synchronized with HTTPS Date header: $urlStr ($httpsTime)")
                    return@withContext httpsTime
                }
            } catch (e: Exception) {
                Log.d(TAG, "HTTPS Date query failed for $urlStr: ${e.message}")
            }
        }

        null
    }

    /**
     * Queries an SNTP server (RFC 4330 standard SNTP client).
     */
    private fun querySntp(host: String): Long? {
        var socket: DatagramSocket? = null
        return try {
            val address = InetAddress.getByName(host)
            val buffer = ByteArray(48)
            buffer[0] = 0x1B // LI = 0 (no warning), VN = 3 (version 3), Mode = 3 (client)

            val requestPacket = DatagramPacket(buffer, buffer.size, address, NTP_PORT)
            socket = DatagramSocket().apply {
                soTimeout = TIMEOUT_MS
            }

            val t0 = System.currentTimeMillis()
            socket.send(requestPacket)

            val responsePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(responsePacket)
            val t3 = System.currentTimeMillis()

            // Transmit timestamp starts at byte 40 (64-bit NTP timestamp format)
            val transmitTimestamp = readNtpTimestamp(buffer, 40)
            if (transmitTimestamp <= 0L) return null

            // Estimate network latency offset
            val roundTripTime = (t3 - t0).coerceAtLeast(0L)
            transmitTimestamp + (roundTripTime / 2L)
        } catch (e: Exception) {
            null
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Converts 64-bit NTP timestamp (seconds since Jan 1, 1900) to Unix epoch milliseconds.
     */
    private fun readNtpTimestamp(buffer: ByteArray, offset: Int): Long {
        val secondsSince1900 = read32Bits(buffer, offset)
        val fraction = read32Bits(buffer, offset + 4)

        if (secondsSince1900 == 0L) return 0L

        // Seconds between 1900-01-01 and 1970-01-01 (70 years + 17 leap days)
        val offsetToEpochSeconds = 2208988800L
        val secondsSinceEpoch = secondsSince1900 - offsetToEpochSeconds
        val fractionMs = (fraction * 1000L) / 0x100000000L

        return (secondsSinceEpoch * 1000L) + fractionMs
    }

    private fun read32Bits(buffer: ByteArray, offset: Int): Long {
        val b0 = buffer[offset].toLong() and 0xFF
        val b1 = buffer[offset + 1].toLong() and 0xFF
        val b2 = buffer[offset + 2].toLong() and 0xFF
        val b3 = buffer[offset + 3].toLong() and 0xFF
        return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }

    /**
     * Fallback: Reads HTTP "Date" response header from a reliable HTTPS endpoint.
     */
    private fun queryHttpsDate(urlStr: String): Long? {
        var connection: HttpsURLConnection? = null
        return try {
            val url = URL(urlStr)
            connection = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                useCaches = false
                instanceFollowRedirects = false
            }
            connection.connect()

            val dateHeader = connection.getHeaderField("Date")
            if (!dateHeader.isNullOrBlank()) {
                val format = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.US)
                format.parse(dateHeader)?.time
            } else {
                val dateLong = connection.date
                if (dateLong > 0L) dateLong else null
            }
        } catch (e: Exception) {
            null
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }
}
