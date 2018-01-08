package netty

import io.netty.bootstrap.Bootstrap


/**
 * @author Abhijit Sarkar
 */
object UserAgent {
    private const val UNKNOWN = "unknown"

    private val javaClientVersion = UserAgent::class.java.`package`.implementationVersion ?: UNKNOWN
    private val javaVendor = System.getProperty("java.vendor") ?: UNKNOWN
    private val javaVersion = System.getProperty("java.version") ?: UNKNOWN
    private val nettyVersion = Bootstrap::class.java.`package`.implementationVersion ?: UNKNOWN

    val userAgent: String get() = "NettyJavaClient/$javaClientVersion (Java; $javaVendor/$javaVersion) (Netty/$nettyVersion)"
}