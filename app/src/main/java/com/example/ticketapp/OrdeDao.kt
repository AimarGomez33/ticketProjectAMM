package com.example.ticketapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/**
 * DAO (Data Access Object) para las entidades de órdenes (OrderEntity y OrderItemEntity).
 * Define todas las operaciones de base de datos para crear, leer y eliminar órdenes.
 */
@Dao
interface OrderDao {

    /**
     * Inserta una orden y sus artículos de forma atómica (transaccional).
     * Primero inserta la OrderEntity para generar su ID, luego asigna ese ID
     * a cada OrderItemEntity y los inserta todos. Si algo falla, se deshace todo.
     */
    @Transaction
    suspend fun insertOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) {
        val orderId = insertOrder(order) // Inserta la orden y obtiene el ID generado
        val itemsWithOrderId = items.map { it.copy(orderId = orderId) } // Asigna el ID a cada artículo
        insertOrderItems(itemsWithOrderId) // Inserta todos los artículos
    }

    /** Inserta una sola entidad de orden. Usado internamente por la transacción. */
    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    /** Inserta una lista de artículos de la orden. Usado internamente por la transacción. */
    @Insert
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    /**
     * Devuelve todas las órdenes guardadas, ordenadas por fecha de creación descendente.
     * Se usa para mostrar la lista de pedidos en la pantalla de administración.
     */
    @Query("SELECT * FROM orders ORDER BY created_at DESC")
    suspend fun getAllOrders(): List<OrderEntity>

    /**
     * Recupera todos los artículos asociados a un ID de orden específico.
     * Útil para mostrar el detalle de un pedido.
     */
    @Query("SELECT * FROM order_items WHERE order_id = :orderId")
    suspend fun getItemsForOrder(orderId: Long): List<OrderItemEntity>

    /**
     * Elimina una orden por su ID. Gracias a la configuración `onDelete = ForeignKey.CASCADE`
     * en la entidad OrderItemEntity, todos sus artículos asociados se borrarán automáticamente.
     */
    @Query("DELETE FROM orders WHERE orderId = :orderId")
    suspend fun deleteOrderById(orderId: Long)

    /**
     * Devuelve las ventas totales y el número de órdenes agrupadas por día contable.
     * El resultado se mapea a la clase de datos DailySummary.
     */
    @Query(
        "SELECT business_date AS businessDate, SUM(grand_total) AS totalSales, COUNT(orderId) AS ordersCount " +
                "FROM orders GROUP BY business_date ORDER BY business_date DESC"
    )
    suspend fun getDailySales(): List<DailySummary>

    /**
     * Devuelve las ventas totales y el número de órdenes agrupadas por semana (formato 'YYYY-WW').
     * El resultado se mapea a la clase de datos WeeklySummary.
     */
    @Query(
        "SELECT strftime('%Y-%W', business_date / 1000, 'unixepoch') AS week, " +
                "SUM(grand_total) AS totalSales, COUNT(orderId) AS ordersCount " +
                "FROM orders GROUP BY week ORDER BY week DESC"
    )
    suspend fun getWeeklySales(): List<WeeklySummary>

    /**
     * Devuelve las ventas totales y el número de órdenes agrupadas por mes (formato 'YYYY-MM').
     * El resultado se mapea a la clase de datos MonthlySummary.
     */
    @Query(
        "SELECT strftime('%Y-%m', business_date / 1000, 'unixepoch') AS month, " +
                "SUM(grand_total) AS totalSales, COUNT(orderId) AS ordersCount " +
                "FROM orders GROUP BY month ORDER BY month DESC"
    )
    suspend fun getMonthlySales(): List<MonthlySummary>
}
