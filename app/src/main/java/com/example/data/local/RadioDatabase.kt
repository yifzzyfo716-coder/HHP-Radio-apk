package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.ServerProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [ServerProfile::class], version = 1, exportSchema = false)
abstract class RadioDatabase : RoomDatabase() {

    abstract fun serverProfileDao(): ServerProfileDao

    companion object {
        @Volatile
        private var INSTANCE: RadioDatabase? = null

        fun getInstance(context: Context): RadioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RadioDatabase::class.java,
                    "hhp_radio.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate with default profiles
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).serverProfileDao().apply {
                                insert(
                                    ServerProfile(
                                        name = "สถานีวิทยุท้องถิ่น (Local Host)",
                                        host = "127.0.0.1",
                                        port = 8765,
                                        username = "สถานีหลัก (HQ)",
                                        callsign = "HHP-MAIN",
                                        isLocalStationHost = true
                                    )
                                )
                                insert(
                                    ServerProfile(
                                        name = "HHP Relay Server 1",
                                        host = "192.168.1.100",
                                        port = 8765,
                                        username = "หน่วยตรวจการณ์ 01",
                                        callsign = "HHP-01",
                                        isLocalStationHost = false
                                    )
                                )
                            }
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
