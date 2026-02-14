package com.example.ticketapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aguas_sabor")
data class AguaSaborEntity(
        @PrimaryKey
        val id: Int, // No debe ser Int? (nullable)
        @ColumnInfo(name = "flavor_name")
        val flavorName: String, // No debe ser String?
        @ColumnInfo(name = "quantity_available")
        val quantityAvailable: Int // No debe ser Int?
)