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

    // ------------------- DELETE THIS BLOCK AS WELL -------------------

    // -----------------------------------------------------------------


    /** Retrieves all items associated with a specific order ID. */
    @Query("SELECT * FROM order_items WHERE order_id = :orderId")
    suspend fun getItemsForOrder(orderId: Long): List<OrderItemEntity>

    @Query("SELECT * FROM products WHERE name = :nombre LIMIT 1")
    suspend fun getProductByName(nombre: String): Product?

    @Query("SELECT * FROM products WHERE name IN (:nombres)")
    suspend fun getProductsByNames(nombres: List<String>): List<Product>

    @Insert
    suspend fun insert(order: OrderEntity)

    // ✅ ADD THIS CORRECT VERSION
    /** Retrieves all orders created within a specific time range. */
    @Query("SELECT * FROM orders WHERE created_at BETWEEN :startTime AND :endTime ORDER BY created_at DESC")
    suspend fun getOrdersBetweenDates(startTime: Long, endTime: Long): List<OrderEntity>



    /** Deletes an order by its ID (its items are automatically deleted due to CASCADE). */
    @Query("DELETE FROM orders WHERE orderId = :orderId")
    suspend fun deleteOrderById(orderId: Long)


    // 🔹 Combo-specific filters
    /** Returns all orders marked as combos. */
    @Query("SELECT * FROM orders WHERE esCombo = 1")
    suspend fun getComboOrders(): List<OrderEntity>

    /** Returns all items that are combos (across any order). */
    @Query("SELECT * FROM order_items WHERE combo = 1")
    suspend fun getComboItems(): List<OrderItemEntity>

    /** Counts how many combo items have been sold across all orders. */
    @Query("SELECT SUM(quantity) FROM order_items WHERE Combo = 1")
    suspend fun countComboItemsSold(): Int?

    /** Calculates total revenue generated from combos. */
    @Query("SELECT SUM(quantity * unit_price) FROM order_items WHERE Combo = 1")
    suspend fun getTotalComboRevenue(): Double?

    /** Returns daily sales summary grouped by business_date. */
    @Query(
        "SELECT business_date AS businessDate, SUM(grand_total) AS totalSales, COUNT(orderId) AS ordersCount " +
                "FROM orders GROUP BY business_date ORDER BY business_date DESC"
    )
    suspend fun getDailySales(): List<DailySummary>

    /** Returns weekly sales summary grouped by year-week (format 'YYYY-WW'). */
    @Query(
        "SELECT strftime('%Y-%W', business_date / 1000, 'unixepoch') AS week, " +
                "SUM(grand_total) AS totalSales, COUNT(orderId) AS ordersCount " +
                "FROM orders GROUP BY week ORDER BY week DESC"
    )
    suspend fun getWeeklySales(): List<WeeklySummary>

    /** Returns monthly sales summary grouped by year-month (format 'YYYY-MM'). */
    @Query(
        "SELECT strftime('%Y-%m', business_date / 1000, 'unixepoch') AS month, " +
                "SUM(grand_total) AS totalSales, COUNT(orderId) AS ordersCount " +
                "FROM orders GROUP BY month ORDER BY month DESC"
    )
    suspend fun getMonthlySales(): List<MonthlySummary>
}
