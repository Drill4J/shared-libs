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
package com.epam.drill.agent.instrument.undertow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import java.net.InetSocketAddress
import java.net.URI
import java.nio.ByteBuffer
import javax.websocket.ClientEndpoint
import javax.websocket.ClientEndpointConfig
import javax.websocket.Endpoint
import javax.websocket.EndpointConfig
import javax.websocket.MessageHandler
import javax.websocket.OnMessage
import javax.websocket.Session
import javax.websocket.WebSocketContainer
import javax.websocket.server.ServerEndpoint
import javax.websocket.server.ServerEndpointConfig
import org.xnio.OptionMap
import org.xnio.Xnio
import io.undertow.Undertow
import io.undertow.servlet.Servlets
import io.undertow.servlet.api.DeploymentManager
import io.undertow.websockets.jsr.UndertowContainerProvider
import io.undertow.websockets.jsr.WebSocketDeploymentInfo
import com.epam.drill.agent.instrument.TestPayloadProcessor
import com.epam.drill.agent.instrument.TestRequestHolder
import com.epam.drill.common.agent.request.DrillRequest

@Suppress("FunctionName")
class UndertowWsMessagesTransformerObjectTest {

    @Test
    fun `test annotated endpoint sync-remote string with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        false, "text", "basic"
    )

    @Test
    fun `test annotated endpoint sync-remote binary with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        false, "binary", "basic"
    )

    @Test
    fun `test annotated endpoint async-remote string with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        false, "text", "async"
    )

    @Test
    fun `test annotated endpoint async-remote binary with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        false, "binary", "async"
    )

    @Test
    fun `test interface endpoint sync-remote string with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        false, "text", "basic"
    )

    @Test
    fun `test interface endpoint sync-remote binary with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        false, "binary", "basic"
    )

    @Test
    fun `test interface endpoint async-remote string with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        false, "text", "async"
    )

    @Test
    fun `test interface endpoint async-remote binary with disabled per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        false, "binary", "async"
    )

    @Test
    fun `test annotated endpoint sync-remote string with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        true, "text", "basic"
    )

    @Test
    fun `test annotated endpoint sync-remote binary with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        true, "binary", "basic"
    )

    @Test
    fun `test annotated endpoint async-remote string with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        true, "text", "async"
    )

    @Test
    fun `test annotated endpoint async-remote binary with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerAnnotatedEndpoint,
        ::connectToWebsocketAnnotatedEndpoint,
        true, "binary", "async"
    )

    @Test
    fun `test interface endpoint sync-remote string with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        true, "text", "basic"
    )

    @Test
    fun `test interface endpoint sync-remote binary with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        true, "binary", "basic"
    )

    @Test
    fun `test interface endpoint async-remote string with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        true, "text", "async"
    )

    @Test
    fun `test interface endpoint async-remote binary with both-side per-message flag`() = testPerMessageRequests(
        ::withWebSocketServerInterfaceEndpoint,
        ::connectToWebsocketInterfaceEndpoint,
        true, "binary", "async"
    )

    private fun withWebSocketServerAnnotatedEndpoint(block: (String) -> Unit) = WebSocketDeploymentInfo()
        .addEndpoint(TestRequestServerAnnotatedEndpoint::class.java)
        .let { withWebSocketServer(it, block) }

    private fun withWebSocketServerInterfaceEndpoint(block: (String) -> Unit) = WebSocketDeploymentInfo()
        .addEndpoint(ServerEndpointConfig.Builder.create(TestRequestServerInterfaceEndpoint::class.java, "/").build())
        .let { withWebSocketServer(it, block) }

    private fun withWebSocketServer(info: WebSocketDeploymentInfo, block: (String) -> Unit) = Undertow.builder().run {
        val wsDeploymentManager = webSocketDeploymentManager(info)
        this.addHttpListener(0, "localhost")
        this.setHandler(wsDeploymentManager.also(DeploymentManager::deploy).let(DeploymentManager::start))
        val server = this.build()
        try {
            server.start()
            block("ws://localhost:${(server.listenerInfo[0].address as InetSocketAddress).port}")
        } finally {
            wsDeploymentManager.stop()
            wsDeploymentManager.undeploy()
            server.stop()
        }
    }

    private fun webSocketDeploymentManager(info: WebSocketDeploymentInfo) = Servlets.defaultContainer().run {
        val worker = Xnio.getInstance().createWorker(OptionMap.EMPTY)
        val deployment = Servlets.deployment()
            .setClassLoader(this::class.java.classLoader)
            .setContextPath("/")
            .setDeploymentName("test-websockets")
            .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, info.setWorker(worker))
        this.addDeployment(deployment)
    }

    private fun connectToWebsocketAnnotatedEndpoint(address: String) = TestRequestClientAnnotatedEndpoint().run {
        val session = TestUndertowContainerProvider().container.connectToServer(this, URI(address))
        this to session
    }

    private fun connectToWebsocketInterfaceEndpoint(address: String) = TestRequestClientInterfaceEndpoint().run {
        val session = TestUndertowContainerProvider().container
            .connectToServer(this, ClientEndpointConfig.Builder.create().build(), URI(address))
        this to session
    }

    private fun testPerMessageRequests(
        withServer: (block: (String) -> Unit) -> Unit,
        withConnection: (String) -> Pair<TestRequestEndpoint, Session>,
        perMessageEnabled: Boolean,
        payloadType: String,
        sendType: String
    ) = withServer { address ->
        TestRequestEndpoint.incomingMessages.clear()
        TestRequestEndpoint.incomingContexts.clear()
        TestPayloadProcessor.enabled = perMessageEnabled
        val results = callWebSocketEndpoint(withConnection, address, payloadType, sendType)
        assertEquals(10, TestRequestEndpoint.incomingMessages.size)
        assertEquals(10, TestRequestEndpoint.incomingContexts.size)
        assertEquals(10, results.first.size)
        assertEquals(10, results.second.size)
        results.first.forEachIndexed { i, message ->
            assertEquals("test-request-$i", message)
        }
        results.second.forEachIndexed { i, drillRequest ->
            assertNull(drillRequest)
        }
        TestRequestEndpoint.incomingMessages.forEachIndexed { i, message ->
            assertEquals("test-request-$i", message)
        }
        TestRequestEndpoint.incomingContexts.forEachIndexed { i, drillRequest ->
            assertNull(drillRequest)
        }
    }

    private fun callWebSocketEndpoint(
        withConnection: (String) -> Pair<TestRequestEndpoint, Session>,
        address: String,
        payloadType: String,
        sendType: String,
        body: String = "test-request-",
        count: Int = 10
    ) = withConnection(address).run {
        val session = this.second
        when (payloadType) {
            "text" -> when (sendType) {
                "basic" -> (0 until count).map(body::plus).forEach(session.basicRemote::sendText)
                "async" -> (0 until count).map(body::plus).forEach(session.asyncRemote::sendText)
            }
            "binary" -> when (sendType) {
                "basic" -> (0 until count).map(body::plus).map(String::encodeToByteArray).map(ByteBuffer::wrap)
                    .forEach(session.basicRemote::sendBinary)
                "async" -> (0 until count).map(body::plus).map(String::encodeToByteArray).map(ByteBuffer::wrap)
                    .forEach(session.asyncRemote::sendBinary)
            }
        }
        Thread.sleep(1000)
        session.close()
        this.first.incomingMessages to this.first.incomingContexts
    }

    interface TestRequestEndpoint {
        companion object {
            val incomingMessages = mutableListOf<String>()
            val incomingContexts = mutableListOf<DrillRequest?>()
        }
        val incomingMessages: MutableList<String>
        val incomingContexts: MutableList<DrillRequest?>
    }

    @Suppress("unused")
    @ServerEndpoint(value = "/")
    class TestRequestServerAnnotatedEndpoint : TestRequestEndpoint {
        override val incomingMessages = TestRequestEndpoint.incomingMessages
        override val incomingContexts = TestRequestEndpoint.incomingContexts
        @OnMessage
        fun onTextMessage(message: String, session: Session) {
            incomingMessages.add(message)
            incomingContexts.add(TestRequestHolder.retrieve())
            session.basicRemote.sendText(message)
        }
        @OnMessage
        fun onBinaryMessage(message: ByteBuffer, session: Session) {
            val text = ByteArray(message.limit()).also(message::get).decodeToString()
            incomingMessages.add(text)
            incomingContexts.add(TestRequestHolder.retrieve())
            session.basicRemote.sendBinary(ByteBuffer.wrap(text.encodeToByteArray()))
        }
    }

    class TestRequestServerInterfaceEndpoint : Endpoint(), TestRequestEndpoint {
        override val incomingMessages = TestRequestEndpoint.incomingMessages
        override val incomingContexts = TestRequestEndpoint.incomingContexts
        override fun onOpen(session: Session, config: EndpointConfig) = try {
            session.addMessageHandler(String::class.java) { message ->
                incomingMessages.add(message)
                incomingContexts.add(TestRequestHolder.retrieve())
                session.basicRemote.sendText(message)
            }
            session.addMessageHandler(ByteBuffer::class.java) { message ->
                val text = ByteArray(message.limit()).also(message::get).decodeToString()
                incomingMessages.add(text)
                incomingContexts.add(TestRequestHolder.retrieve())
                session.basicRemote.sendBinary(ByteBuffer.wrap(text.encodeToByteArray()))
            }
        } catch (e: AbstractMethodError) {
            session.addMessageHandler(ServerTextMessageHandler(session, incomingMessages, incomingContexts))
            session.addMessageHandler(ServerBinaryMessageHandler(session, incomingMessages, incomingContexts))
        }
    }

    @Suppress("unused")
    @ClientEndpoint
    class TestRequestClientAnnotatedEndpoint : TestRequestEndpoint {
        override val incomingMessages = mutableListOf<String>()
        override val incomingContexts = mutableListOf<DrillRequest?>()
        @OnMessage
        fun onTextMessage(message: String) {
            incomingMessages.add(message)
            incomingContexts.add(TestRequestHolder.retrieve())
        }
        @OnMessage
        fun onBinaryMessage(message: ByteBuffer) {
            incomingMessages.add(ByteArray(message.limit()).also(message::get).decodeToString())
            incomingContexts.add(TestRequestHolder.retrieve())
        }
    }

    class TestRequestClientInterfaceEndpoint : Endpoint(), TestRequestEndpoint {
        override val incomingMessages = mutableListOf<String>()
        override val incomingContexts = mutableListOf<DrillRequest?>()
        override fun onOpen(session: Session, config: EndpointConfig) = try {
            session.addMessageHandler(String::class.java) { message ->
                incomingMessages.add(message)
                incomingContexts.add(TestRequestHolder.retrieve())
            }
            session.addMessageHandler(ByteBuffer::class.java) { message ->
                incomingMessages.add(ByteArray(message.limit()).also(message::get).decodeToString())
                incomingContexts.add(TestRequestHolder.retrieve())
            }
        } catch (e: AbstractMethodError) {
            session.addMessageHandler(ClientTextMessageHandler(incomingMessages, incomingContexts))
            session.addMessageHandler(ClientBinaryMessageHandler(incomingMessages, incomingContexts))
        }
    }

    private class ServerTextMessageHandler(
        private val session: Session,
        private val incomingMessages: MutableList<String>,
        private val incomingContexts: MutableList<DrillRequest?>
    ) : MessageHandler.Whole<String> {
        override fun onMessage(message: String) {
            incomingMessages.add(message)
            incomingContexts.add(TestRequestHolder.retrieve())
            session.basicRemote.sendText(message)
        }
    }

    private class ServerBinaryMessageHandler(
        private val session: Session,
        private val incomingMessages: MutableList<String>,
        private val incomingContexts: MutableList<DrillRequest?>
    ) : MessageHandler.Whole<ByteBuffer> {
        override fun onMessage(message: ByteBuffer) {
            val text = ByteArray(message.limit()).also(message::get).decodeToString()
            incomingMessages.add(text)
            incomingContexts.add(TestRequestHolder.retrieve())
            session.basicRemote.sendBinary(ByteBuffer.wrap(text.encodeToByteArray()))
        }
    }

    private class ClientTextMessageHandler(
        private val incomingMessages: MutableList<String>,
        private val incomingContexts: MutableList<DrillRequest?>
    ) : MessageHandler.Whole<String> {
        override fun onMessage(message: String) {
            incomingMessages.add(message)
            incomingContexts.add(TestRequestHolder.retrieve())
        }
    }

    private class ClientBinaryMessageHandler(
        private val incomingMessages: MutableList<String>,
        private val incomingContexts: MutableList<DrillRequest?>
    ) : MessageHandler.Whole<ByteBuffer> {
        override fun onMessage(message: ByteBuffer) {
            incomingMessages.add(ByteArray(message.limit()).also(message::get).decodeToString())
            incomingContexts.add(TestRequestHolder.retrieve())
        }
    }

    private class TestUndertowContainerProvider : UndertowContainerProvider() {
        public override fun getContainer(): WebSocketContainer = super.getContainer()
    }

}
