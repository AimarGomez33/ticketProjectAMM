package com.example.ticketapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AguaSaborDao {
    @Query("SELECT * FROM aguas_sabor ORDER BY flavor_name ASC")
    fun getAllFlow(): Flow<List<AguaSaborEntity>>


    @Query("SELECT * FROM aguas_sabor ORDER BY flavor_name ASC")
    suspend fun getAll(): List<AguaSaborEntity>

    @Insert suspend fun insert(agua: AguaSaborEntity)

    @Update suspend fun update(agua: AguaSaborEntity)

    @Delete suspend fun delete(agua: AguaSaborEntity)

    @Query("UPDATE aguas_sabor SET quantity_available = :newQuantity WHERE id = :id")
    suspend fun updateQuantity(id: Long, newQuantity: Int)

    @Query("DELETE FROM aguas_sabor")
    suspend fun deleteAll()
}
