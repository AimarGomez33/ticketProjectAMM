package com.example.ticketapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RefrescoDao {
    @Query("SELECT * FROM refrescos ORDER BY flavor_name ASC")
    fun getAllFlow(): Flow<List<RefrescoEntity>>

    @Query("SELECT * FROM refrescos ORDER BY flavor_name ASC")
    suspend fun getAll(): List<RefrescoEntity>

    @Insert suspend fun insert(refresco: RefrescoEntity)

    @Update suspend fun update(refresco: RefrescoEntity)

    @Delete suspend fun delete(refresco: RefrescoEntity)

    @Query("UPDATE refrescos SET quantity_available = :newQuantity WHERE id = :id")
    suspend fun updateQuantity(id: Long, newQuantity: Int)

    @Query("DELETE FROM refrescos") suspend fun deleteAll()
}
