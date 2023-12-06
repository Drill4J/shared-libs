package com.epam.drill.agent.transport

import kotlin.concurrent.thread
import java.io.File
import org.apache.hc.core5.http.HttpStatus
import mu.KotlinLogging
import com.epam.drill.agent.configuration.WsConfiguration
import com.epam.drill.common.agent.transport.AgentMessage
import com.epam.drill.common.agent.transport.AgentMessageDestination
import com.epam.drill.common.agent.transport.AgentMessageSender
import com.epam.drill.common.agent.transport.ResponseStatus

object HttpAgentMessageSender: AgentMessageSender {

    private val logger = KotlinLogging.logger {}

    private var agentInstanceId = WsConfiguration.getInstanceId()
    private var attached = false

    override fun isTransportAvailable(): Boolean = attached

    override fun <T: AgentMessage> send(destination: AgentMessageDestination, message: T): ResponseStatus {
        val status = when(destination.type) {
            "POST" -> HttpClient.post("agent/$agentInstanceId/${destination.target}", message)
            "PUT" -> HttpClient.put("agent/$agentInstanceId/${destination.target}", message)
            else -> -1
        }
        return HttpResponseStatus(status)
    }

    @Suppress("UNUSED")
    fun sendAgentInstance() {
        thread {
            HttpClient.configure(
                WsConfiguration.getAdminAddress(),
                checkSslTruststorePath(WsConfiguration.getSslTruststore()),
                WsConfiguration.getSslTruststorePassword()
            )
            val agentConfigHex = WsConfiguration.getAgentConfigHexString()
            val httpCall: (String) -> Int = { HttpClient.put("agent/instance", it) }
            val logError: (Throwable) -> Unit = { logger.error(it) { "agentAttach: Attempt is failed: $it" } }
            val timeout: (Throwable) -> Unit = { Thread.sleep(5000) }
            var status = 0
            logger.debug { "agentAttach: Sending request to admin server" }
            while(status != HttpStatus.SC_OK) {
                status = agentConfigHex.runCatching(httpCall).onFailure(logError).onFailure(timeout).getOrDefault(0)
            }
            logger.debug { "agentAttach: Sending request to admin server: successful" }
            attached = true
        }
    }

    private fun checkSslTruststorePath(filePath: String) = File(filePath).run {
        val drillPath = WsConfiguration.getDrillInstallationDir()
            .removeSuffix(File.pathSeparator)
            .takeIf(String::isNotEmpty)
            ?: "."
        this.takeIf(File::exists)?.let(File::getAbsolutePath)
            ?: this.takeUnless(File::isAbsolute)?.let { File(drillPath).resolve(it).absolutePath }
            ?: filePath
    }

}
