package org.abhijitsarkar.kotlin.netty.joke

import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JokeProxyFrontendHandlerTest {

    @Test
    fun `should proxy request to outbound channel`() {
        val outboundChannel = EmbeddedChannel()
        val testChannel = EmbeddedChannel(
            JokeProxyFrontendHandler({ outboundChannel })
        )

        DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.GET,
            "/jokes/random"
        )
            .also { testChannel.writeInbound(it) }

        assertTrue(testChannel.isOpen)
        assertTrue(outboundChannel.isOpen)

        val messages = outboundChannel.outboundMessages()
        assertTrue(messages.isNotEmpty())
        val req = messages.poll() as DefaultFullHttpRequest
        assertEquals("http://${outboundChannel.hostPort()}/jokes/random", req.uri())
        assertEquals(outboundChannel.hostPort(), req.headers()[HttpHeaderNames.HOST])
    }
}
