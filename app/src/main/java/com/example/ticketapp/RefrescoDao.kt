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

    @Query("SELECT * FROM refrescos WHERE id = :id") suspend fun getById(id: Long): RefrescoEntity?

    @Query("DELETE FROM refrescos") suspend fun deleteAll(): Int

    @Insert suspend fun insert(refrescos: RefrescoEntity)

    @Update suspend fun update(refrescos: RefrescoEntity)

    @Delete suspend fun delete(refrescos: RefrescoEntity)

    @Insert suspend fun insertRefresco(s: RefrescoEntity)

    @Query("UPDATE refrescos SET quantity_available = :newQuantity WHERE id = :id")
    suspend fun updateQuantity(id: Long, newQuantity: Int)

    // OPCIÓN B: Por si quieres filtrar (opcional)
    @Query("SELECT * FROM refrescos WHERE quantity_available > 0 ORDER BY flavor_name ASC")
    fun getAvailableFlavors(): Flow<List<RefrescoEntity>>

    @Query("SELECT * FROM refrescos") fun getAllRefrescos(): Flow<List<RefrescoEntity>>
}
