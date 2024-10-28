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

import platform.posix.fprintf
import platform.posix.stderr

public object ConsoleOutputAppender : Appender {
    override val includePrefix: Boolean = true
    public override fun trace(loggerName: String, message: String): Unit = println(message)
    public override fun debug(loggerName: String, message: String): Unit = println(message)
    public override fun info(loggerName: String, message: String): Unit = println(message)
    public override fun warn(loggerName: String, message: String): Unit = println(message)

    @kotlinx.cinterop.ExperimentalForeignApi
    override fun error(loggerName: String, message: String) {
        fprintf(stderr, "$message\n")
    }
}
