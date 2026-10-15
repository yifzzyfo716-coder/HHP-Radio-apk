package com.example.network

import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

object RadioProtocol {
    const val MAGIC: Short = 0x4850 // 'HP' in ASCII (HHP Protocol)
    const val VERSION: Byte = 1

    // Packet Types
    const val TYPE_AUTH_REQ: Byte = 0x01
    const val TYPE_AUTH_RESP: Byte = 0x02
    const val TYPE_CHANNEL_LIST_REQ: Byte = 0x03
    const val TYPE_CHANNEL_LIST_RESP: Byte = 0x04
    const val TYPE_JOIN_CHANNEL: Byte = 0x05
    const val TYPE_LEAVE_CHANNEL: Byte = 0x06
    const val TYPE_PTT_REQUEST: Byte = 0x07
    const val TYPE_PTT_GRANT: Byte = 0x08
    const val TYPE_PTT_DENY: Byte = 0x09
    const val TYPE_PTT_RELEASE: Byte = 0x0A
    const val TYPE_AUDIO_FRAME: Byte = 0x0B
    const val TYPE_USER_PRESENCE: Byte = 0x0C
    const val TYPE_PING: Byte = 0x0D
    const val TYPE_PONG: Byte = 0x0E

    fun writePacket(dos: DataOutputStream, type: Byte, payload: ByteArray) {
        synchronized(dos) {
            dos.writeShort(MAGIC.toInt())
            dos.writeByte(VERSION.toInt())
            dos.writeByte(type.toInt())
            dos.writeInt(payload.size)
            if (payload.isNotEmpty()) {
                dos.write(payload)
            }
            dos.flush()
        }
    }

    data class Header(val magic: Short, val version: Byte, val type: Byte, val length: Int)

    fun readHeader(dis: DataInputStream): Header? {
        val magic = dis.readShort()
        if (magic != MAGIC) return null
        val version = dis.readByte()
        val type = dis.readByte()
        val length = dis.readInt()
        return Header(magic, version, type, length)
    }

    fun stringToBytes(str: String): ByteArray = str.toByteArray(StandardCharsets.UTF_8)
    fun bytesToString(bytes: ByteArray): String = String(bytes, StandardCharsets.UTF_8)
}
