package org.abhijitsarkar.kotlin.netty

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Abhijit Sarkar
 */
inline fun <reified T : Any> loggerFor(): Logger = LoggerFactory.getLogger(T::class.java)