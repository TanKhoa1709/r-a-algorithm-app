package app.net.discovery

import app.models.NodeConfig
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
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
        val multicastGroup = InetAddress.getByName(config.multicastAddress)
        
        socket = if (config.bindInterface != null) {
            // Bind to specific network interface
            val bindAddr = InetAddress.getByName(config.bindInterface)
            val networkInterface = NetworkInterface.getByInetAddress(bindAddr)
            MulticastSocket(config.multicastPort).apply {
                if (networkInterface != null) {
                    setNetworkInterface(networkInterface)
                }
                joinGroup(multicastGroup)
            }
        } else {
            // Try to bind to any available interface
            MulticastSocket(config.multicastPort).apply {
                joinGroup(multicastGroup)
            }
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

