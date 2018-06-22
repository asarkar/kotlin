package org.abhijitsarkar.kotlin.netty.gzip;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.compression.DecompressionException;

import java.nio.ByteBuffer;

/**
 * @author Abhijit Sarkar
 */
final class CompressionUtil {

    private CompressionUtil() {
    }

    static void checkChecksum(ByteBufChecksum checksum, ByteBuf uncompressed, int currentChecksum) {
        checksum.reset();
        checksum.update(uncompressed,
                uncompressed.readerIndex(), uncompressed.readableBytes());

        final int checksumResult = (int) checksum.getValue();
        if (checksumResult != currentChecksum) {
            throw new DecompressionException(String.format(
                    "stream corrupted: mismatching checksum: %d (expected: %d)",
                    checksumResult, currentChecksum));
        }
    }

    static ByteBuffer safeNioBuffer(ByteBuf buffer) {
        return buffer.nioBufferCount() == 1 ? buffer.internalNioBuffer(buffer.readerIndex(), buffer.readableBytes())
                : buffer.nioBuffer();
    }
}
