package com.example.ticketapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aguas_sabor")
data class AguaSaborEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        @ColumnInfo(name = "flavor_name") val flavorName: String,
        @ColumnInfo(name = "quantity_available") val quantityAvailable: Int
)
