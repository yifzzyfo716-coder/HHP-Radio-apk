package com.example.data.repository

import com.example.data.local.ServerProfileDao
import com.example.model.ServerProfile
import kotlinx.coroutines.flow.Flow

class ServerRepository(private val dao: ServerProfileDao) {

    val allProfiles: Flow<List<ServerProfile>> = dao.getAllProfiles()

    suspend fun getProfileById(id: Long): ServerProfile? = dao.getProfileById(id)

    suspend fun saveProfile(profile: ServerProfile): Long {
        return if (profile.id == 0L) {
            dao.insert(profile)
        } else {
            dao.update(profile)
            profile.id
        }
    }

    suspend fun deleteProfile(id: Long) = dao.deleteById(id)
}
