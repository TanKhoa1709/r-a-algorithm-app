package app.net.discovery

import app.models.NodeConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.MulticastSocket

/**
 * Announces node presence via multicast
 */
class NodeAnnouncer(
    private val config: DiscoveryConfig,
    private val nodeConfig: NodeConfig
) {
    private var running = false
    private var socket: MulticastSocket? = null
    
    fun start() {
        running = true
        socket = MulticastSocket(config.multicastPort).apply {
            joinGroup(InetAddress.getByName(config.multicastAddress))
        }
        
        Thread {
            while (running) {
                try {
                    val announcement = Json.encodeToString(nodeConfig)
                    val data = announcement.toByteArray()
                    val packet = DatagramPacket(
                        data,
                        data.size,
                        InetAddress.getByName(config.multicastAddress),
                        config.multicastPort
                    )
                    socket?.send(packet)
                    Thread.sleep(config.discoveryInterval)
                } catch (e: Exception) {
                    if (running) {
                        e.printStackTrace()
                    }
                }
            }
        }.start()
    }
    
    fun stop() {
        running = false
        socket?.close()
        socket = null
    }
}

