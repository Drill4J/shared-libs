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
package com.epam.drill.agent.instrument.servers

import com.alibaba.ttl.threadpool.agent.TtlAgent
import com.alibaba.ttl.threadpool.agent.internal.logging.Logger
import com.alibaba.ttl.threadpool.agent.internal.transformlet.ClassInfo
import com.alibaba.ttl.threadpool.agent.internal.transformlet.JavassistTransformlet
import com.alibaba.ttl.threadpool.agent.internal.transformlet.impl.TtlExecutorTransformlet
import com.alibaba.ttl.threadpool.agent.internal.transformlet.impl.TtlForkJoinTransformlet
import com.alibaba.ttl.threadpool.agent.internal.transformlet.impl.TtlTimerTaskTransformlet
import javassist.CtClass
import mu.KotlinLogging
import com.epam.drill.agent.instrument.AbstractTransformerObject

abstract class TTLTransformerObject : AbstractTransformerObject() {

    private val transformletList: MutableList<JavassistTransformlet> = ArrayList()

    override val logger = KotlinLogging.logger {}

    init {
        Logger.setLoggerImplType("")
        transformletList.add(TtlExecutorTransformlet(false))
        transformletList.add(TtlForkJoinTransformlet(false))
        if (TtlAgent.isEnableTimerTask()) transformletList.add(TtlTimerTaskTransformlet())
    }

    override fun permit(className: String?, superName: String?, interfaces: Array<String?>): Boolean =
        throw NotImplementedError()

    override fun transform(
        className: String,
        classFileBuffer: ByteArray,
        loader: Any?,
        protectionDomain: Any?
    ): ByteArray {
        try {
            val classInfo = ClassInfo(className.replace('/', '.'), classFileBuffer, (loader as? ClassLoader))
            for (transformlet in transformletList) {
                transformlet.doTransform(classInfo)
                if (classInfo.isModified) return classInfo.ctClass.toBytecode()
            }
        } catch (e: Exception) {
            logger.error(e) { "transform: Failed to transform class $className" }
        }
        return classFileBuffer
    }

    override fun transform(className: String, ctClass: CtClass): Unit = throw NotImplementedError()

}
