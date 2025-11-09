package com.example.ticketapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para las entidades de órdenes (OrderEntity y OrderItemEntity).
 * Define todas las operaciones de base de datos para crear, leer y eliminar órdenes.
 */
@Dao
interface OrderDao {

    /**
     * Inserts an order and its items transactionally.
     * First inserts the OrderEntity to generate its ID, then assigns that ID
     * to each OrderItemEntity and inserts them all. If anything fails, the transaction rolls back.
     */
    @Transaction
    suspend fun insertOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) {
        val orderId = insertOrder(order)
        val itemsWithOrderId = items.map { it.copy(orderId = orderId) }
        insertOrderItems(itemsWithOrderId)
    }

    /** Inserts a single order entity (used internally by the transaction). */
    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    /** Inserts a list of order items (used internally by the transaction). */
    @Insert
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    /** Returns all saved orders, ordered by creation date (descending). */
    @Query("SELECT * FROM orders ORDER BY created_at DESC")
    suspend fun getAllOrders(): List<OrderEntity>

    @Query("SELECT * FROM orders ORDER BY created_at DESC")
    fun getAllOrdersFlow(): Flow<List<OrderEntity>>

    // Ganancia por categoría en un rango [inicio, fin] (por created_at)
    @Query("""
    SELECT COALESCE(SUM(oi.quantity * oi.unit_price), 0)
    FROM order_items AS oi
    INNER JOIN orders AS o    ON o.orderId = oi.order_id
    INNER JOIN products AS p  ON p.name = oi.name
    WHERE (:category IS NULL OR p.category = :category)
      AND (:includeCombos = 1 OR oi.esCombo = 0)
      AND o.created_at BETWEEN :inicio AND :fin
""")
    suspend fun getProfitByCategory(
        category: String?,     // null o nombre exacto de la categoría
        inicio: Long,
        fin: Long,
        includeCombos: Int     // 1 = incluir combos, 0 = excluir combos
    ): Double



    /** Retrieves all items associated with a specific order ID. */
    // CORREGIDO: El nombre del parámetro en la consulta (:orderId) debe coincidir con el nombre del parámetro de la función.
    @Query("SELECT * FROM order_items WHERE order_id = :orderId")
    suspend fun getItemsForOrder(orderId: Long): List<OrderItemEntity>

    @Query("SELECT * FROM products WHERE name = :nombre LIMIT 1")
    suspend fun getProductByName(nombre: String): Product?

    @Query("SELECT * FROM products WHERE name IN (:nombres)")
    suspend fun getProductsByNames(nombres: List<String>): List<Product>

    /** Deletes an order by its ID (its items are automatically deleted due to CASCADE). */
    @Query("DELETE FROM orders WHERE orderId = :orderId")
    suspend fun deleteOrderById(orderId: Long)



    // ⬅️ Usar created_at para rangos (coincide con lo que muestra la UI)
    @Query("""
        SELECT * FROM orders
        WHERE created_at BETWEEN :inicio AND :fin
        ORDER BY created_at ASC
    """)
    suspend fun getOrdersBetween(inicio: Long, fin: Long): List<OrderEntity>



    // ======== RESÚMENES / ESTADÍSTICAS =========

    // 🔹 Combo-specific filters
    /** Returns all orders marked as combos. */
    @Query("SELECT * FROM orders WHERE esCombo = 1")
    suspend fun getComboOrders(): List<OrderEntity>

    /** Returns all items that are combos (across any order). */
    // CORREGIDO: Se asumió que la columna correcta es 'esCombo', no 'combo'.
    @Query("SELECT * FROM order_items WHERE esCombo = 1")
    suspend fun getComboItems(): List<OrderItemEntity>

    /** Counts how many combo items have been sold across all orders. */
    // CORREGIDO: Se cambió 'Combo' por 'esCombo' para que coincida con otras consultas.
    @Query("SELECT SUM(quantity) FROM order_items WHERE esCombo = 1")
    suspend fun countComboItemsSold(): Int?

    /** Calculates total revenue generated from combos. */
    // CORREGIDO: Se cambió 'Combo' por 'esCombo' para que coincida con otras consultas.
    @Query("SELECT SUM(quantity * unit_price) FROM order_items WHERE esCombo = 1")
    suspend fun getTotalComboRevenue(): Double?

    /** Returns daily sales summary grouped by business_date. */
    @Query(
        "SELECT business_date AS businessDate, SUM(grand_total) AS totalSales, COUNT(orderId) AS ordersCount " +
                "FROM orders GROUP BY business_date ORDER BY business_date DESC"
    )
    suspend fun getDailySales(): List<DailySummary>

    /** Returns weekly sales summary grouped by year-week (format 'YYYY-WW'). */
    @Query(
        "SELECT strftime('%Y-%W', created_at / 1000, 'unixepoch') AS week, " + // CORREGIDO: Usar created_at si business_date es para lógica de negocio
                "SUM(grand_total) AS totalSales, COUNT(orderId) AS ordersCount " +
                "FROM orders GROUP BY week ORDER BY week DESC"
    )
    suspend fun getWeeklySales(): List<WeeklySummary>

    /** Returns monthly sales summary grouped by year-month (format 'YYYY-MM'). */
    @Query(
        "SELECT strftime('%Y-%m', created_at / 1000, 'unixepoch') AS month, " + // CORREGIDO: Usar created_at si business_date es para lógica de negocio
                "SUM(grand_total) AS totalSales, COUNT(orderId) AS ordersCount " +
                "FROM orders GROUP BY month ORDER BY month DESC"
    )
    suspend fun getMonthlySales(): List<MonthlySummary> // CORREGIDO: Se eliminó la declaración duplicada y se colocó esta aquí.

    // 🔹 Calcular el total de ingresos de productos normales
    @Query("SELECT SUM(quantity * unit_price) FROM order_items WHERE esCombo = 0")
    suspend fun totalGananciasNormales(): Double?

    // 🔹 Calcular el total global (combos + normales)
    @Query("SELECT SUM(quantity * unit_price) FROM order_items")
    suspend fun totalGananciasGlobal(): Double?

    // 🔹 Obtener artículos vendidos dentro de un rango de fechas (por created_at)
    @Query("""
        SELECT oi.* FROM order_items oi
        INNER JOIN orders o ON o.orderId = oi.order_id
        WHERE o.created_at BETWEEN :inicio AND :fin
        ORDER BY o.created_at DESC
    """)
    suspend fun obtenerItemsPorRangoDeFecha(inicio: Long, fin: Long): List<OrderItemEntity>


}
