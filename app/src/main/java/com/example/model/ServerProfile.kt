package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_profiles")
data class ServerProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int = 8765,
    val username: String = "Operator-1",
    val callsign: String = "HHP-01",
    val authToken: String = "",
    val autoConnect: Boolean = false,
    val isLocalStationHost: Boolean = false,
    val lastUsedTimestamp: Long = System.currentTimeMillis()
)
