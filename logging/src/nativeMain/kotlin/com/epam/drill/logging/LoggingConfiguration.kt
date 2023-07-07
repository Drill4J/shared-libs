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

import mu.KotlinLoggingConfiguration
import mu.KotlinLoggingLevel

actual object LoggingConfiguration {

    actual fun readDefaultConfiguration() {
        KotlinLoggingConfiguration.formatter = SimpleMessageFormatter
        KotlinLoggingConfiguration.logLevel = KotlinLoggingLevel.INFO
    }

    actual fun setLoggingLevels(levels: List<Pair<String, String>>) {
        val levelRegex = Regex("(TRACE|DEBUG|INFO|WARN|ERROR)")
        val defaultLoggers = sequenceOf("", "com", "com.epam", "com.epam.drill")
        val isCorrect: (Pair<String, String>) -> Boolean = { levelRegex.matches(it.second) }
        val isDefaultLogger: (Pair<String, String>) -> Boolean = { defaultLoggers.contains(it.first) }
        levels.filter(isCorrect).sortedBy(Pair<String, String>::first).lastOrNull(isDefaultLogger)?.let {
            KotlinLoggingConfiguration.logLevel = KotlinLoggingLevel.valueOf(it.second)
        }
    }

    actual fun setLoggingLevels(levels: String) {
        val levelPairRegex = Regex("([\\w.]*=)?(TRACE|DEBUG|INFO|WARN|ERROR)")
        val toLevelPair: (String) -> Pair<String, String>? = { str ->
            str.takeIf(levelPairRegex::matches)?.let { it.substringBefore("=", "") to it.substringAfter("=") }
        }
        setLoggingLevels(levels.split(";").mapNotNull(toLevelPair))
    }

}
