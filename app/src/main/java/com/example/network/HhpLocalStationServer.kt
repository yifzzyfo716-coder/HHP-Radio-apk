package com.example.network

import android.util.Log
import com.example.model.RadioChannel
import com.example.model.RadioUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class HhpLocalStationServer(private val port: Int = 8765) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val clientHandlers = ConcurrentHashMap<String, ClientSession>()
    private val channelSpeakers = ConcurrentHashMap<String, String>() // channelId -> active speaker userId

    val defaultChannels = listOf(
        RadioChannel("ch_01", "CH-01 ช่องเรียกขานหลัก", "144.150 MHz", "ช่องสื่อสารกลางและแจ้งเหตุ"),
        RadioChannel("ch_02", "CH-02 ปฏิบัติการภาคสนาม", "144.300 MHz", "ประสานงานชุดเคลื่อนที่เร็ว"),
        RadioChannel("ch_03", "CH-03 เหตุฉุกเฉินและกู้ภัย", "145.000 MHz", "ช่องความถี่เหตุฉุกเฉิน"),
        RadioChannel("ch_04", "CH-04 ส่งกำลังบำรุง", "430.125 MHz", "ฝ่ายสนับสนุนและยานพาหนะ"),
        RadioChannel("ch_05", "CH-05 ตรวจการณ์และ รปภ.", "433.500 MHz", "สายตรวจและจุดสกัด")
    )

    var isRunning = false
        private set

    inner class ClientSession(
        val socket: Socket,
        val dis: DataInputStream,
        val dos: DataOutputStream
    ) {
        var userId: String = ""
        var callsign: String = ""
        var username: String = ""
        var currentChannelId: String = "ch_01"

        fun sendPacket(type: Byte, payload: ByteArray) {
            try {
                RadioProtocol.writePacket(dos, type, payload)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending packet to $userId", e)
            }
        }
    }

    fun start(): Boolean {
        if (isRunning) return true
        try {
            serverSocket = ServerSocket(port)
            isRunning = true
            Log.i(TAG, "HHP Local Station Server started on port $port")

            serverJob = scope.launch {
                while (isActive && isRunning) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        handleNewClient(clientSocket)
                    } catch (e: Exception) {
                        if (!isRunning) break
                        Log.e(TAG, "Server accept error", e)
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server on port $port", e)
            isRunning = false
            return false
        }
    }

    private fun handleNewClient(socket: Socket) {
        scope.launch {
            var session: ClientSession? = null
            try {
                val dis = DataInputStream(socket.getInputStream())
                val dos = DataOutputStream(socket.getOutputStream())
                session = ClientSession(socket, dis, dos)

                while (isActive && isRunning) {
                    val header = RadioProtocol.readHeader(dis) ?: break
                    val payload = ByteArray(header.length)
                    if (header.length > 0) {
                        dis.readFully(payload)
                    }

                    when (header.type) {
                        RadioProtocol.TYPE_AUTH_REQ -> {
                            val json = JSONObject(RadioProtocol.bytesToString(payload))
                            session.userId = json.optString("userId", "user_${System.currentTimeMillis()}")
                            session.callsign = json.optString("callsign", "HHP-STATION")
                            session.username = json.optString("username", "Operator")
                            clientHandlers[session.userId] = session

                            // Send AUTH_RESP
                            val resp = JSONObject().apply {
                                put("status", "OK")
                                put("assignedId", session.userId)
                                put("serverName", "HHP Local Relay Station")
                            }
                            session.sendPacket(
                                RadioProtocol.TYPE_AUTH_RESP,
                                RadioProtocol.stringToBytes(resp.toString())
                            )

                            // Send channel list
                            sendChannelList(session)
                            broadcastUserPresence()
                        }

                        RadioProtocol.TYPE_JOIN_CHANNEL -> {
                            val json = JSONObject(RadioProtocol.bytesToString(payload))
                            val newChannelId = json.getString("channelId")
                            // If user was speaking in old channel, release lock
                            if (channelSpeakers[session.currentChannelId] == session.userId) {
                                channelSpeakers.remove(session.currentChannelId)
                            }
                            session.currentChannelId = newChannelId
                            broadcastUserPresence()
                        }

                        RadioProtocol.TYPE_PTT_REQUEST -> {
                            val channelId = session.currentChannelId
                            val currentSpeaker = channelSpeakers[channelId]
                            if (currentSpeaker == null || currentSpeaker == session.userId) {
                                // Grant PTT
                                channelSpeakers[channelId] = session.userId
                                val grantJson = JSONObject().apply {
                                    put("channelId", channelId)
                                    put("granted", true)
                                }
                                session.sendPacket(
                                    RadioProtocol.TYPE_PTT_GRANT,
                                    RadioProtocol.stringToBytes(grantJson.toString())
                                )
                                broadcastSpeakerUpdate(channelId, session.userId, session.callsign, true)
                            } else {
                                // Channel is busy
                                val denyJson = JSONObject().apply {
                                    put("channelId", channelId)
                                    put("reason", "CHANNEL_BUSY")
                                    put("speakerCallsign", clientHandlers[currentSpeaker]?.callsign ?: "OPERATOR")
                                }
                                session.sendPacket(
                                    RadioProtocol.TYPE_PTT_DENY,
                                    RadioProtocol.stringToBytes(denyJson.toString())
                                )
                            }
                        }

                        RadioProtocol.TYPE_PTT_RELEASE -> {
                            val channelId = session.currentChannelId
                            if (channelSpeakers[channelId] == session.userId) {
                                channelSpeakers.remove(channelId)
                                broadcastSpeakerUpdate(channelId, session.userId, session.callsign, false)
                            }
                        }

                        RadioProtocol.TYPE_AUDIO_FRAME -> {
                            // Forward audio frame to all other users in this channel
                            val channelId = session.currentChannelId
                            if (channelSpeakers[channelId] == session.userId) {
                                for ((uid, client) in clientHandlers) {
                                    if (uid != session.userId && client.currentChannelId == channelId) {
                                        client.sendPacket(RadioProtocol.TYPE_AUDIO_FRAME, payload)
                                    }
                                }
                            }
                        }

                        RadioProtocol.TYPE_PING -> {
                            session.sendPacket(RadioProtocol.TYPE_PONG, ByteArray(0))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Client disconnected: ${session?.userId}")
            } finally {
                session?.let { s ->
                    clientHandlers.remove(s.userId)
                    if (channelSpeakers[s.currentChannelId] == s.userId) {
                        channelSpeakers.remove(s.currentChannelId)
                        broadcastSpeakerUpdate(s.currentChannelId, s.userId, s.callsign, false)
                    }
                    try {
                        s.socket.close()
                    } catch (_: Exception) {}
                    broadcastUserPresence()
                }
            }
        }
    }

    private fun sendChannelList(session: ClientSession) {
        val array = JSONArray()
        for (ch in defaultChannels) {
            val userCount = clientHandlers.values.count { it.currentChannelId == ch.id }
            val activeSpeaker = channelSpeakers[ch.id]
            val speakerCallsign = activeSpeaker?.let { clientHandlers[it]?.callsign }
            val item = JSONObject().apply {
                put("id", ch.id)
                put("name", ch.name)
                put("frequencyMhz", ch.frequencyMhz)
                put("description", ch.description)
                put("userCount", userCount)
                put("currentSpeaker", speakerCallsign)
            }
            array.put(item)
        }
        val obj = JSONObject().apply { put("channels", array) }
        session.sendPacket(
            RadioProtocol.TYPE_CHANNEL_LIST_RESP,
            RadioProtocol.stringToBytes(obj.toString())
        )
    }

    private fun broadcastSpeakerUpdate(channelId: String, userId: String, callsign: String, isSpeaking: Boolean) {
        val json = JSONObject().apply {
            put("channelId", channelId)
            put("userId", userId)
            put("callsign", callsign)
            put("isSpeaking", isSpeaking)
        }
        val bytes = RadioProtocol.stringToBytes(json.toString())
        for ((_, client) in clientHandlers) {
            if (client.currentChannelId == channelId) {
                client.sendPacket(RadioProtocol.TYPE_USER_PRESENCE, bytes)
            }
        }
    }

    private fun broadcastUserPresence() {
        val usersArray = JSONArray()
        for ((_, client) in clientHandlers) {
            val u = JSONObject().apply {
                put("userId", client.userId)
                put("callsign", client.callsign)
                put("username", client.username)
                put("channelId", client.currentChannelId)
            }
            usersArray.put(u)
        }
        val json = JSONObject().apply {
            put("type", "PRESENCE_SYNC")
            put("users", usersArray)
        }
        val bytes = RadioProtocol.stringToBytes(json.toString())
        for ((_, client) in clientHandlers) {
            client.sendPacket(RadioProtocol.TYPE_USER_PRESENCE, bytes)
        }
    }

    fun stop() {
        isRunning = false
        serverJob?.cancel()
        serverJob = null
        for ((_, client) in clientHandlers) {
            try { client.socket.close() } catch (_: Exception) {}
        }
        clientHandlers.clear()
        channelSpeakers.clear()
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket", e)
        }
        Log.i(TAG, "HHP Local Station Server stopped")
    }

    companion object {
        private const val TAG = "HhpLocalServer"

        fun getLocalIpAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val inetAddress = addresses.nextElement()
                        if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                            return inetAddress.hostAddress ?: "127.0.0.1"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting local IP", e)
            }
            return "127.0.0.1"
        }
    }
}
