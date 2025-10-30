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
    val items: List<OrderItemEntity>, // This is the list you need
    val total: Double,
    val timestamp: String,
    val tableNumber: String,
    val id: Int = 0,
    val customerName: String,
    // ... other fields
    val purchasedItems: List<String>
)

/**
 * Entidad que representa un artículo individual dentro de una orden.
 * Está vinculada a una OrderEntity a través de una llave foránea.
 */
@Entity(
    tableName = "order_items",
    // Define la relación "muchos a uno" con OrderEntity
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["orderId"],  // Columna en la tabla padre (orders)
            childColumns = ["order_id"],   // Columna en esta tabla (order_items)
            onDelete = ForeignKey.CASCADE // Si se borra una orden, se borran sus artículos
        )
    ],
    // Crea un índice en la columna order_id para acelerar las consultas
    indices = [Index("order_id")]
)
data class OrderItemEntity(
    /** Clave primaria autogenerada para el artículo de la orden. */
    @PrimaryKey(autoGenerate = true)
    val itemId: Long = 0,

    /** Referencia a la orden a la que pertenece este artículo. */
    @ColumnInfo(name = "order_id")
    val orderId: Long,

    /** Nombre del producto vendido. */
    @ColumnInfo(name = "name")
    val name: String,

    /** Precio unitario del producto al momento de la venta. */
    @ColumnInfo(name = "unit_price")
    val unitPrice: Double,

    /** Cantidad vendida de este producto. */
    @ColumnInfo(name = "quantity")
    val quantity: Int,

    @ColumnInfo(name = "combo")
    val esCombo: Boolean = false
)