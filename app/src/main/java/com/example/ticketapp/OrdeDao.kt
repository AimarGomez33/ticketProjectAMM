package com.example.ticketapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para las entidades de órdenes (OrderEntity y OrderItemEntity). Define
 * todas las operaciones de base de datos para crear, leer y eliminar órdenes.
 */
@Dao
interface OrderDao {

        /**
         * Inserts an order and its items transactionally. First inserts the OrderEntity to generate
         * its ID, then assigns that ID to each OrderItemEntity and inserts them all. If anything
         * fails, the transaction rolls back.
         */
        @Transaction
        suspend fun insertOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) {
                val orderId = insertOrder(order)
                val itemsWithOrderId = items.map { it.copy(orderId = orderId) }
                insertOrderItems(itemsWithOrderId)
        }

        @androidx.room.Update suspend fun updateOrder(order: OrderEntity)

        @Query("UPDATE orders SET grand_total = :newTotal WHERE orderId = :orderId")
        suspend fun updateOrderTotal(orderId: Long, newTotal: Double)

        @Query("UPDATE orders SET is_completed = :isCompleted WHERE orderId = :orderId")
        suspend fun updateOrderStatus(orderId: Long, isCompleted: Boolean)

        /** Inserts a single order entity (used internally by the transaction). */
        @Insert suspend fun insertOrder(order: OrderEntity): Long

        /** Inserts a list of order items (used internally by the transaction). */
        @Insert suspend fun insertOrderItems(items: List<OrderItemEntity>)

        /** Returns all saved orders, ordered by creation date (descending). */
        @Query("SELECT * FROM orders ORDER BY created_at DESC")
        suspend fun getAllOrders(): List<OrderEntity>

        @Query("SELECT * FROM orders ORDER BY created_at DESC")
        fun getAllOrdersFlow(): Flow<List<OrderEntity>>

        // Ganancia por categoría en un rango [inicio, fin] (por created_at)
        @Query(
                """
SELECT COALESCE(SUM(oi.quantity * oi.unit_price), 0)
FROM order_items oi
INNER JOIN orders o ON o.orderId = oi.order_id
LEFT JOIN products p ON p.name = oi.name
WHERE (:includeCombos = 1 OR oi.esCombo = 0)
  AND o.created_at BETWEEN :inicio AND :fin
  AND (
    :category IS NULL
    OR COALESCE(
          p.category,
          CASE
            WHEN oi.name LIKE 'Hamburguesa%'                      THEN 'Hamburguesas'
            WHEN oi.name LIKE 'Orden de Papas%' OR oi.name LIKE 'Papas%' THEN 'Papas'
            WHEN oi.name LIKE 'Alitas %'                          THEN 'Alitas'
            WHEN oi.name LIKE 'Taco%'                             THEN 'Tacos'
            WHEN oi.name IN ('Refrescos','Cafe','Aguas de Sabor','Agua Natural','Agua para Te') THEN 'Bebidas'
            WHEN oi.name LIKE 'Postres %'                         THEN 'Postres'
            WHEN oi.name LIKE 'Extra %'                           THEN 'Extras'
            WHEN oi.name LIKE 'Guajoloyet%' OR oi.name LIKE 'Guajoloyets%' THEN 'Guajoloyets'
            WHEN oi.name LIKE 'Pambazo%' OR oi.name LIKE 'Pambazos%'      THEN 'Pambazos'
            WHEN oi.name LIKE 'Pozole %'                          THEN 'Pozole'
            WHEN oi.name = 'Quesadillas'                          THEN 'Quesadillas'
            WHEN oi.name = 'Chalupas'                             THEN 'Chalupas'
            ELSE 'Otros'
          END
       ) = :category
  )
"""
        )
        suspend fun getProfitByCategory(
                category: String?,
                inicio: Long,
                fin: Long,
                includeCombos: Int
        ): Double

        /** Retrieves all items associated with a specific order ID. */
        // CORREGIDO: El nombre del parámetro en la consulta (:orderId) debe coincidir con el nombre
        // del
        // parámetro de la función.
        @Query("SELECT * FROM order_items WHERE order_id = :orderId")
        suspend fun getItemsForOrder(orderId: Long): List<OrderItemEntity>

        @Query("SELECT * FROM products WHERE name = :nombre LIMIT 1")
        suspend fun getProductByName(nombre: String): Product?

        @Query("SELECT * FROM products WHERE name IN (:nombres)")
        suspend fun getProductsByNames(nombres: List<String>): List<Product>

        /**
         * Deletes all items belonging to a specific order (used before a full item replacement).
         */
        @Query("DELETE FROM order_items WHERE order_id = :orderId")
        suspend fun deleteItemsForOrder(orderId: Long)

        /** Deletes an order by its ID (its items are automatically deleted due to CASCADE). */
        @Query("DELETE FROM orders WHERE orderId = :orderId")
        suspend fun deleteOrderById(orderId: Long)

        @Query(
                """
        SELECT * FROM orders
        WHERE created_at BETWEEN :inicio AND :fin
        ORDER BY created_at ASC
    """
        )
        suspend fun getOrdersBetween(inicio: Long, fin: Long): List<OrderEntity>

        @Query(
                """
        SELECT oi.name, SUM(oi.quantity) as totalQty
        FROM order_items oi
        INNER JOIN orders o ON o.orderId = oi.order_id
        WHERE o.created_at BETWEEN :start AND :end
        GROUP BY oi.name
        ORDER BY totalQty DESC
        LIMIT 10
    """
        )
        suspend fun getTopSellingProducts(start: Long, end: Long): List<TopSellingProduct>

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

        @Query(
                value =
                        """
                SELECT DISTINCT o.* 
                FROM orders AS o
                INNER JOIN order_items AS oi ON o.orderId = oi.order_id -- Join con items del pedido
                INNER JOIN products AS p ON oi.name = p.name -- Join con productos para obtener la categoría
                WHERE 
                    p.category = :categoryName -- Filtrar por categoría
                    AND o.created_at BETWEEN :startDate AND :endDate -- Filtrar por rango de fechas
                ORDER BY o.created_at DESC
            """
        )
        suspend fun getOrdersByCategory(
                categoryName: String,
                startDate: Long,
                endDate: Long
        ): List<OrderEntity>

        @Query(
                "SELECT business_date AS businessDate, SUM(grand_total) AS totalSales, COUNT(orderId) AS ordersCount " +
                        "FROM orders GROUP BY business_date ORDER BY business_date DESC"
        )
        suspend fun getDailySales(): List<DailySummary>

        /** Returns weekly sales summary grouped by year-week (format 'YYYY-WW'). */
        @Query(
                "SELECT strftime('%Y-%W', created_at / 1000, 'unixepoch') AS week, " + // CORREGIDO:
                        // Usar
                        // created_at si
                        // business_date
                        // es para lógica
                        // de negocio
                        "SUM(grand_total) AS totalSales, COUNT(orderId) AS ordersCount " +
                        "FROM orders GROUP BY week ORDER BY week DESC"
        )
        suspend fun getWeeklySales(): List<WeeklySummary>

        /** Returns monthly sales summary grouped by year-month (format 'YYYY-MM'). */
        @Query(
                "SELECT strftime('%Y-%m', created_at / 1000, 'unixepoch') AS month, " + // CORREGIDO:
                        // Usar
                        // created_at si
                        // business_date
                        // es para
                        // lógica de
                        // negocio
                        "SUM(grand_total) AS totalSales, COUNT(orderId) AS ordersCount " +
                        "FROM orders GROUP BY month ORDER BY month DESC"
        )
        suspend fun getMonthlySales():
                List<MonthlySummary> // CORREGIDO: Se eliminó la declaración duplicada y se colocó
        // esta aquí.

        // 🔹 Calcular el total de ingresos de productos normales
        @Query("SELECT SUM(quantity * unit_price) FROM order_items WHERE esCombo = 0")
        suspend fun totalGananciasNormales(): Double?

        // 🔹 Calcular el total global (combos + normales)
        @Query("SELECT SUM(quantity * unit_price) FROM order_items")
        suspend fun totalGananciasGlobal(): Double?

        // 🔹 Obtener artículos vendidos dentro de un rango de fechas (por created_at)
        @Query(
                """
        SELECT oi.* FROM order_items oi
        INNER JOIN orders o ON o.orderId = oi.order_id
        WHERE o.created_at BETWEEN :inicio AND :fin
        ORDER BY o.created_at DESC
    """
        )
        suspend fun obtenerItemsPorRangoDeFecha(inicio: Long, fin: Long): List<OrderItemEntity>
}
