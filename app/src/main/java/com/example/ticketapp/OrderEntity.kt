package com.example.ticketapp

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad que representa una orden (una venta o ticket completo).
 * Guarda la información general de la transacción.
 */
@Entity(tableName = "orders")
data class OrderEntity(
    /** Clave primaria autogenerada para la orden. */
    @PrimaryKey(autoGenerate = true)
    val orderId: Long = 0,

    /** Identificador de la mesa o cliente. Puede ser nulo. */
    @ColumnInfo(name = "mesa")
    val mesa: String?,

    /** Fecha y hora exactas de creación de la orden (milisegundos desde la época). */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    /** Fecha contable de la orden (útil para cortes de caja). */
    @ColumnInfo(name = "business_date")
    val businessDate: Long,

    /** Total final pagado en la orden. */
    @ColumnInfo(name = "grand_total")
    val grandTotal: Double,

    val esCombo: Boolean = false,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false
)

