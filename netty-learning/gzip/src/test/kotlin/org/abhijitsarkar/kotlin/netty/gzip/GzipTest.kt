package org.abhijitsarkar.kotlin.netty.gzip

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.compression.ZlibWrapper
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.netty.handler.codec.compression.JdkZlibDecoder as NettyJdkZlibDecoder

class GzipTest {
    @Test
    fun `should completely read concatenated streams using JdkZlibDecoder`() {
        val testChannel = EmbeddedChannel(
//            NettyJdkZlibDecoder(ZlibWrapper.GZIP)
            JdkZlibDecoder(true)
        )

        Files.readAllBytes(Paths.get(javaClass.getResource("/multiple.gz").toURI()))
            .also { testChannel.writeInbound(Unpooled.copiedBuffer(it)) }

        val data = testChannel.inboundMessages()
            .map { it as ByteBuf }
            .map { it.toString(StandardCharsets.UTF_8) }
            .toList()

        assertEquals(2, data.size)
        assertEquals(listOf("a", "b"), data)

        assertTrue(testChannel.isOpen)

        testChannel.close()
    }

    @Test
    fun `should only read first streams using JdkZlibDecoder`() {
        val testChannel = EmbeddedChannel(
//            NettyJdkZlibDecoder(ZlibWrapper.GZIP)
            JdkZlibDecoder(ZlibWrapper.GZIP)
        )

        Files.readAllBytes(Paths.get(javaClass.getResource("/multiple.gz").toURI()))
            .also { testChannel.writeInbound(Unpooled.copiedBuffer(it)) }

        val data = testChannel.inboundMessages()
            .map { it as ByteBuf }
            .map { it.toString(StandardCharsets.UTF_8) }
            .toList()

        assertEquals(1, data.size)
        assertEquals("a", data.first())

        assertTrue(testChannel.isOpen)

        testChannel.close()
    }

    @Test
    fun `should completely read concatenated streams using GzipCompressorInputStream`() {
        GzipCompressorInputStream(javaClass.getResourceAsStream("/multiple.gz"), true).use {
            assertEquals('a', it.read().toChar())
            assertEquals('b', it.read().toChar())
            assertEquals(0, it.available())
            assertEquals(-1, it.read())
        }
    }
}