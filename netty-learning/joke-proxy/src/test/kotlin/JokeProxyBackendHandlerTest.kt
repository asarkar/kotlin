package org.abhijitsarkar.kotlin.netty.joke

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JokeProxyBackendHandlerTest {

    @Test
    fun `should echo response to outbound channel`() {
        val json = """
            {
              "type": "success",
              "value": {
                "id": 510,
                "joke": "Chuck Norris can compile syntax errors.",
                "categories": [
                  "nerdy"
                ]
              }
            }
            """
            .trimIndent()

        val outboundChannel = EmbeddedChannel()
        val testChannel = EmbeddedChannel(
            JokeProxyBackendHandler(outboundChannel)
        )

        DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.copiedBuffer(json, StandardCharsets.UTF_8)
        )
            .also { testChannel.writeInbound(it) }

        assertFalse(testChannel.isOpen)
        assertFalse(outboundChannel.isOpen)

        val messages = outboundChannel.outboundMessages()
        assertTrue(messages.isNotEmpty())
        assertEquals(json, (messages.poll() as DefaultFullHttpResponse).content().toString(StandardCharsets.UTF_8))
    }
}
