package com.example.ticketapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
        tableName = "order_items",
        foreignKeys =
                [
                        ForeignKey(
                                entity = OrderEntity::class,
                                parentColumns = ["orderId"],
                                childColumns = ["order_id"],
                                onDelete = ForeignKey.CASCADE
                        )],
        indices = [Index("order_id")]
)
data class OrderItemEntity(
        @PrimaryKey(autoGenerate = true) val itemId: Long = 0,
        @ColumnInfo(name = "order_id") val orderId: Long,
        @ColumnInfo(name = "name") val name: String,
        @ColumnInfo(name = "unit_price") val unitPrice: Double,
        @ColumnInfo(name = "quantity") val quantity: Int,
        @ColumnInfo(name = "esCombo", defaultValue = "0") val esCombo: Boolean = false,
        @ColumnInfo(name = "comment") val comment: String? = null
)
