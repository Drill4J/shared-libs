/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.logging

import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.OutputStreamAppender

actual object LoggingConfiguration {

    private var filename: String? = null

    actual fun readDefaultConfiguration() {
        val root = (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger)
        root.loggerContext.reset()
        root.level = Level.ERROR
        root.addAppender(createConsoleAppender())
    }

    actual fun setLoggingLevels(levels: List<Pair<String, String>>) {
        val levelRegex = Regex("(TRACE|DEBUG|INFO|WARN|ERROR)")
        val isCorrect: (Pair<String, String>) -> Boolean = { levelRegex.matches(it.second) }
        levels.filter(isCorrect).forEach {
            (LoggerFactory.getLogger(it.first) as Logger).level = Level.toLevel(it.second)
        }
    }

    actual fun setLoggingLevels(levels: String) {
        val levelPairRegex = Regex("([\\w.]*=)?(TRACE|DEBUG|INFO|WARN|ERROR)")
        val toLevelPair: (String) -> Pair<String, String>? = { str ->
            str.takeIf(levelPairRegex::matches)?.let { it.substringBefore("=", "ROOT") to it.substringAfter("=") }
        }
        setLoggingLevels(levels.split(";").mapNotNull(toLevelPair))
    }

    actual fun setLoggingFilename(filename: String?) {
        val appender = filename?.runCatching(::createFileAppender)?.getOrNull() ?: createConsoleAppender()
        val withAppenders: (Logger) -> Boolean = { it.iteratorForAppenders().hasNext() }
        (LoggerFactory.getILoggerFactory() as LoggerContext).loggerList.filter(withAppenders).forEach {
            it.detachAndStopAllAppenders()
            it.addAppender(appender)
        }
        this.filename = filename
    }

    actual fun getLoggingFilename(): String? = filename

    private fun createConsoleAppender() =
        configureOutputStreamAppender(ConsoleAppender()).also(ConsoleAppender<ILoggingEvent>::start)

    private fun createFileAppender(filename: String) = configureOutputStreamAppender(FileAppender()).apply {
        this.file = filename
        this.start()
    }

    private fun <T : OutputStreamAppender<ILoggingEvent>> configureOutputStreamAppender(appender: T) = appender.apply {
        val context = (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).loggerContext
        val encoder = PatternLayoutEncoder().also {
            it.pattern = "%date{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%logger] %message%n%throwable"
            it.context = context
            it.start()
        }
        this.context = context
        this.encoder = encoder
    }

}
