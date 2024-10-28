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
package mu

import mu.internal.toStringSafe

public object DefaultMessageFormatter : Formatter {
    public override fun formatMessage(includePrefix: Boolean, level: KotlinLoggingLevel, loggerName: String, msg: () -> Any?): String =
        "${prefix(includePrefix, level, loggerName)}${msg.toStringSafe()}"

    public override fun formatMessage(includePrefix: Boolean, level: KotlinLoggingLevel, loggerName: String, t: Throwable?, msg: () -> Any?): String =
        "${prefix(includePrefix, level, loggerName)}${msg.toStringSafe()}${t.throwableToString()}"

    public override fun formatMessage(includePrefix: Boolean, level: KotlinLoggingLevel, loggerName: String, marker: Marker?, msg: () -> Any?): String =
        "${prefix(includePrefix, level, loggerName)}${marker?.getName()} ${msg.toStringSafe()}"

    public override fun formatMessage(
        includePrefix: Boolean,
        level: KotlinLoggingLevel,
        loggerName: String,
        marker: Marker?,
        t: Throwable?,
        msg: () -> Any?
    ): String =
        "${prefix(includePrefix, level, loggerName)}${marker?.getName()} ${msg.toStringSafe()}${t.throwableToString()}"

    private fun prefix(includePrefix: Boolean, level: KotlinLoggingLevel, loggerName: String): String {
        return if (includePrefix) {
            "${level.name}: [$loggerName] "
        } else {
            ""
        }
    }

    private fun Throwable?.throwableToString(): String {
        if (this == null) {
            return ""
        }
        var msg = ""
        var current = this
        while (current != null && current.cause != current) {
            msg += ", Caused by: '${current.message}'"
            current = current.cause
        }
        return msg
    }
}
