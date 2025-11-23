package app.net.discovery

import app.models.NodeConfig
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.concurrent.ConcurrentHashMap

/**
 * Service discovery for finding other nodes in the network
 */
class ServiceDiscovery(
    private val config: DiscoveryConfig,
    private val onNodeDiscovered: (NodeConfig) -> Unit,
    private val onNodeLost: (String) -> Unit
) {
    private var running = false
    private var socket: MulticastSocket? = null
    private val discoveredNodes = ConcurrentHashMap<String, Long>()
    
    fun start() {
        running = true
        socket = MulticastSocket(config.multicastPort).apply {
            joinGroup(InetAddress.getByName(config.multicastAddress))
        }
        
        // Start receiving thread
        Thread {
            val buffer = ByteArray(1024)
            while (running) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    val nodeConfig = Json.decodeFromString<NodeConfig>(message)
                    discoveredNodes[nodeConfig.nodeId] = System.currentTimeMillis()
                    onNodeDiscovered(nodeConfig)
                } catch (e: Exception) {
                    if (running) {
                        e.printStackTrace()
                    }
                }
            }
        }.start()
        
        // Start timeout checker
        Thread {
            while (running) {
                val now = System.currentTimeMillis()
                discoveredNodes.entries.removeIf { (nodeId, lastSeen) ->
                    if (now - lastSeen > config.nodeTimeout) {
                        onNodeLost(nodeId)
                        true
                    } else {
                        false
                    }
                }
                Thread.sleep(1000)
            }
        }.start()
    }
    
    fun stop() {
        running = false
        socket?.close()
        socket = null
    }
    
    fun getDiscoveredNodes(): Set<String> = discoveredNodes.keys.toSet()
}

