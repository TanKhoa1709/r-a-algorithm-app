package app.core

import app.proto.MsgType
import app.proto.RAMessage
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RicartAgrawalaTest {
    @Test
    fun testRequestCriticalSection() {
        var requestSent = false
        val ra = RicartAgrawala(
            nodeId = "node1",
            onSendRequest = { requestSent = true },
            onSendReply = {},
            onSendRelease = {},
            onEnterCS = {},
            onExitCS = {}
        )
        
        ra.registerNode("node1")
        ra.registerNode("node2")
        
        val requestId = ra.requestCriticalSection()
        assertNotNull(requestId)
        assertTrue(requestSent)
    }
    
    @Test
    fun testHandleRequest() {
        var replySent = false
        val ra = RicartAgrawala(
            nodeId = "node1",
            onSendRequest = {},
            onSendReply = { replySent = true },
            onSendRelease = {},
            onEnterCS = {},
            onExitCS = {}
        )
        
        ra.registerNode("node1")
        ra.registerNode("node2")
        
        val request = RAMessage(
            type = MsgType.REQUEST,
            timestamp = 1,
            nodeId = "node2",
            requestId = "req1"
        )
        
        ra.handleRequest(request)
        assertTrue(replySent)
    }
}

