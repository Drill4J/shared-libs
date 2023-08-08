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
package com.epam.drill.agent

import kotlin.native.concurrent.SharedImmutable
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.plus
import com.epam.drill.common.agent.AgentModule

@SharedImmutable
private val _pstorage = atomic(persistentHashMapOf<String, AgentModule<*>>())

val pstorage
    get() = _pstorage.value

fun addPluginToStorage(plugin: AgentModule<*>) {
    _pstorage.update { it + (plugin.id to plugin) }
}
