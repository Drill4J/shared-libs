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
package com.epam.drill.common

import kotlinx.serialization.*

@Serializable
data class AgentConfig(
    val id: String,
    val instanceId: String,
    val buildVersion: String,
    val serviceGroupId: String,
    val agentType: AgentType,
    val agentVersion: String = "",
    val needSync: Boolean = true,
    val packagesPrefixes: PackagesPrefixes = PackagesPrefixes(),
    val parameters: Map<String, AgentParameter> = emptyMap(),
    val envId: String,
)

@Serializable
data class AgentParameter(
    val type: String,
    val value: String,
    val description: String,
)

interface AgentConfigUpdater {
    fun updateParameters(config: AgentConfig)
}

@Serializable
data class PackagesPrefixes(
    val packagesPrefixes: List<String> = emptyList()
)

@Serializable
data class PluginId(val pluginId: String)

@Serializable
data class TogglePayload(val pluginId: String, val forceValue: Boolean? = null)

const val AgentConfigParam = "AgentConfig"
const val NeedSyncParam = "needSync"
