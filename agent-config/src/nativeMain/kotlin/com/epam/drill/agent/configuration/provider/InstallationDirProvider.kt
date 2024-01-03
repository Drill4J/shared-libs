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
package com.epam.drill.agent.configuration.provider

import kotlinx.cinterop.toKString
import platform.posix.getenv
import com.epam.drill.agent.configuration.AgentConfigurationProvider
import com.epam.drill.agent.configuration.AgentProcessMetadata
import com.epam.drill.agent.configuration.DefaultAgentConfiguration

class InstallationDirProvider(
    private val configurationProviders: Set<AgentConfigurationProvider>,
    override val priority: Int = 300
) : AgentConfigurationProvider {

    private val pathSeparator = if (Platform.osFamily == OsFamily.WINDOWS) "\\" else "/"

    override val configuration: Map<String, String>
        get() = mapOf(Pair(DefaultAgentConfiguration.INSTALLATION_DIR.name, installationDir()))

    private fun installationDir() = fromProviders()
        ?: fromJavaToolOptions()
        ?: fromCommandLine()
        ?: "."

    private fun fromProviders() = configurationProviders
        .sortedBy(AgentConfigurationProvider::priority)
        .mapNotNull { it.configuration[DefaultAgentConfiguration.INSTALLATION_DIR.name] }
        .lastOrNull()

    private fun fromJavaToolOptions() = getenv("JAVA_TOOL_OPTIONS")?.toKString()
        ?.substringAfter("-agentpath:")
        ?.substringBefore("=")
        ?.substringBeforeLast(pathSeparator)

    private fun fromCommandLine() = runCatching(AgentProcessMetadata::commandLine::get).getOrNull()
        ?.substringAfter("-agentpath:")
        ?.substringBefore("=")
        ?.substringBeforeLast(pathSeparator)

}
