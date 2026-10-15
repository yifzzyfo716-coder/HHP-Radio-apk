package com.example.network

import android.util.Log
import com.example.model.ConnectionStatus
import com.example.model.PttState
import com.example.model.RadioChannel
import com.example.model.RadioUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

class RadioClient {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var socket: Socket? = null
    private var dis: DataInputStream? = null
    private var dos: DataOutputStream? = null
    private var readJob: Job? = null
    private var pingJob: Job? = null

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _channels = MutableStateFlow<List<RadioChannel>>(emptyList())
    val channels: StateFlow<List<RadioChannel>> = _channels.asStateFlow()

    private val _users = MutableStateFlow<List<RadioUser>>(emptyList())
    val users: StateFlow<List<RadioUser>> = _users.asStateFlow()

    private val _currentChannelId = MutableStateFlow("ch_01")
    val currentChannelId: StateFlow<String> = _currentChannelId.asStateFlow()

    private val _activeSpeaker = MutableStateFlow<String?>(null)
    val activeSpeaker: StateFlow<String?> = _activeSpeaker.asStateFlow()

    private val _pingMs = MutableStateFlow(0L)
    val pingMs: StateFlow<Long> = _pingMs.asStateFlow()

    private val _serverDisplayName = MutableStateFlow("")
    val serverDisplayName: StateFlow<String> = _serverDisplayName.asStateFlow()

    var onIncomingAudio: ((ByteArray) -> Unit)? = null
    var onPttGrantCallback: ((Boolean, String?) -> Unit)? = null

    private var myUserId: String = "user_${System.currentTimeMillis()}"
    private var myCallsign: String = "HHP-01"
    private var myUsername: String = "Operator"

    fun connect(
        host: String,
        port: Int,
        username: String,
        callsign: String,
        token: String = ""
    ) {
        if (_connectionStatus.value == ConnectionStatus.CONNECTING ||
            _connectionStatus.value == ConnectionStatus.CONNECTED) {
            return
        }

        if (host.isBlank() || port <= 0 || port > 65535) {
            _lastError.value = "ที่อยู่เซิร์ฟเวอร์หรือพอร์ตไม่ถูกต้อง ($host:$port)"
            _connectionStatus.value = ConnectionStatus.ERROR
            return
        }

        myUsername = username
        myCallsign = callsign
        _connectionStatus.value = ConnectionStatus.CONNECTING
        _lastError.value = null

        scope.launch {
            try {
                val newSocket = Socket()
                newSocket.connect(InetSocketAddress(host, port), 5000)
                newSocket.tcpNoDelay = true
                socket = newSocket

                val inStream = DataInputStream(newSocket.getInputStream())
                val outStream = DataOutputStream(newSocket.getOutputStream())
                dis = inStream
                dos = outStream

                // Send Auth Request
                val authJson = JSONObject().apply {
                    put("userId", myUserId)
                    put("username", username)
                    put("callsign", callsign)
                    put("token", token)
                }
                RadioProtocol.writePacket(
                    outStream,
                    RadioProtocol.TYPE_AUTH_REQ,
                    RadioProtocol.stringToBytes(authJson.toString())
                )

                startReadingPackets(inStream)
                startPingLoop()
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed to $host:$port", e)
                _lastError.value = "ไม่สามารถเชื่อมต่อได้: ${e.localizedMessage ?: e.message}"
                _connectionStatus.value = ConnectionStatus.ERROR
                disconnect()
            }
        }
    }

    private fun startReadingPackets(inStream: DataInputStream) {
        readJob = scope.launch {
            try {
                while (isActive && socket?.isConnected == true && !socket!!.isClosed) {
                    val header = RadioProtocol.readHeader(inStream) ?: break
                    val payload = ByteArray(header.length)
                    if (header.length > 0) {
                        inStream.readFully(payload)
                    }

                    when (header.type) {
                        RadioProtocol.TYPE_AUTH_RESP -> {
                            val json = JSONObject(RadioProtocol.bytesToString(payload))
                            if (json.optString("status") == "OK") {
                                _serverDisplayName.value = json.optString("serverName", "HHP Server")
                                _connectionStatus.value = ConnectionStatus.CONNECTED
                                _lastError.value = null
                            } else {
                                _lastError.value = "การยืนยันตัวตนล้มเหลว: ${json.optString("message")}"
                                _connectionStatus.value = ConnectionStatus.ERROR
                                disconnect()
                            }
                        }

                        RadioProtocol.TYPE_CHANNEL_LIST_RESP -> {
                            val json = JSONObject(RadioProtocol.bytesToString(payload))
                            val arr = json.getJSONArray("channels")
                            val list = mutableListOf<RadioChannel>()
                            for (i in 0 until arr.length()) {
                                val item = arr.getJSONObject(i)
                                list.add(
                                    RadioChannel(
                                        id = item.getString("id"),
                                        name = item.getString("name"),
                                        frequencyMhz = item.getString("frequencyMhz"),
                                        description = item.optString("description", ""),
                                        activeUsersCount = item.optInt("userCount", 0),
                                        currentSpeakerName = item.optString("currentSpeaker", null)
                                    )
                                )
                            }
                            _channels.value = list
                        }

                        RadioProtocol.TYPE_PTT_GRANT -> {
                            onPttGrantCallback?.invoke(true, null)
                        }

                        RadioProtocol.TYPE_PTT_DENY -> {
                            val json = JSONObject(RadioProtocol.bytesToString(payload))
                            val reason = json.optString("reason", "BUSY")
                            val speaker = json.optString("speakerCallsign", "ผู้ใช้อื่น")
                            onPttGrantCallback?.invoke(false, "ช่องไม่ว่าง: กำลังใช้งานโดย $speaker")
                        }

                        RadioProtocol.TYPE_AUDIO_FRAME -> {
                            onIncomingAudio?.invoke(payload)
                        }

                        RadioProtocol.TYPE_USER_PRESENCE -> {
                            val raw = RadioProtocol.bytesToString(payload)
                            val json = JSONObject(raw)
                            if (json.optString("type") == "PRESENCE_SYNC") {
                                val arr = json.getJSONArray("users")
                                val uList = mutableListOf<RadioUser>()
                                for (i in 0 until arr.length()) {
                                    val u = arr.getJSONObject(i)
                                    val uid = u.getString("userId")
                                    uList.add(
                                        RadioUser(
                                            id = uid,
                                            callsign = u.getString("callsign"),
                                            username = u.getString("username"),
                                            channelId = u.getString("channelId"),
                                            isSelf = uid == myUserId
                                        )
                                    )
                                }
                                _users.value = uList
                            } else {
                                val chId = json.optString("channelId")
                                val isSpeaking = json.optBoolean("isSpeaking", false)
                                val callsign = json.optString("callsign")
                                if (chId == _currentChannelId.value) {
                                    _activeSpeaker.value = if (isSpeaking) callsign else null
                                }
                            }
                        }

                        RadioProtocol.TYPE_PONG -> {
                            // Handled in ping loop
                        }
                    }
                }
            } catch (e: Exception) {
                if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
                    _lastError.value = "การเชื่อมต่อขัดข้อง: ${e.localizedMessage ?: "Network disconnected"}"
                    _connectionStatus.value = ConnectionStatus.ERROR
                }
            } finally {
                disconnect()
            }
        }
    }

    private fun startPingLoop() {
        pingJob = scope.launch {
            while (isActive && _connectionStatus.value == ConnectionStatus.CONNECTED) {
                delay(3000)
                val out = dos ?: break
                val start = System.currentTimeMillis()
                try {
                    RadioProtocol.writePacket(out, RadioProtocol.TYPE_PING, ByteArray(0))
                    _pingMs.value = System.currentTimeMillis() - start
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    fun requestPtt(channelId: String) {
        val out = dos ?: return
        scope.launch {
            try {
                val json = JSONObject().apply { put("channelId", channelId) }
                RadioProtocol.writePacket(
                    out,
                    RadioProtocol.TYPE_PTT_REQUEST,
                    RadioProtocol.stringToBytes(json.toString())
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting PTT", e)
            }
        }
    }

    fun releasePtt(channelId: String) {
        val out = dos ?: return
        scope.launch {
            try {
                val json = JSONObject().apply { put("channelId", channelId) }
                RadioProtocol.writePacket(
                    out,
                    RadioProtocol.TYPE_PTT_RELEASE,
                    RadioProtocol.stringToBytes(json.toString())
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing PTT", e)
            }
        }
    }

    fun sendAudioFrame(pcmChunk: ByteArray, length: Int) {
        val out = dos ?: return
        try {
            val copy = if (length == pcmChunk.size) pcmChunk else pcmChunk.copyOf(length)
            RadioProtocol.writePacket(out, RadioProtocol.TYPE_AUDIO_FRAME, copy)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending audio frame", e)
        }
    }

    fun switchChannel(channelId: String) {
        _currentChannelId.value = channelId
        _activeSpeaker.value = null
        val out = dos ?: return
        scope.launch {
            try {
                val json = JSONObject().apply { put("channelId", channelId) }
                RadioProtocol.writePacket(
                    out,
                    RadioProtocol.TYPE_JOIN_CHANNEL,
                    RadioProtocol.stringToBytes(json.toString())
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error switching channel", e)
            }
        }
    }

    fun disconnect() {
        pingJob?.cancel()
        pingJob = null
        readJob?.cancel()
        readJob = null

        try {
            dos?.close()
            dis?.close()
            socket?.close()
        } catch (_: Exception) {}

        socket = null
        dos = null
        dis = null

        if (_connectionStatus.value != ConnectionStatus.ERROR) {
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
        }
        _activeSpeaker.value = null
    }

    companion object {
        private const val TAG = "HhpRadioClient"
    }
}
