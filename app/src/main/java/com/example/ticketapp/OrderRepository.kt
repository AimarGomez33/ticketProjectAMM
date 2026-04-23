package com.example.ticketapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class OrderRepository(
    private val orderDao: OrderDao,
    private val productDao: ProductDao
) {
    // Aquí migraremos después los listados, guardado y consultas de la DB.
    // Por ahora para la Fase 1, sirve de cascarón preparado para que el ViewModel
    // lo utilice más adelante en lugar de inyectar DAOs directamente.

    suspend fun guardarPedido(order: OrderEntity, items: List<OrderItemEntity>) = withContext(Dispatchers.IO) {
        orderDao.insertOrderWithItems(order, items)
    }
    
    suspend fun eliminarPedido(orderId: Long) = withContext(Dispatchers.IO) {
        orderDao.deleteOrderById(orderId)
    }

    suspend fun getAllOrders(): List<OrderEntity> = withContext(Dispatchers.IO) {
        orderDao.getAllOrders()
    }

    fun getAllOrdersFlow(): Flow<List<OrderEntity>> {
        return orderDao.getAllOrdersFlow()
    }

    suspend fun getItemsForOrder(orderId: Long): List<OrderItemEntity> = withContext(Dispatchers.IO) {
        orderDao.getItemsForOrder(orderId)
    }

    suspend fun updateOrderTotal(orderId: Long, newGrandTotal: Double) = withContext(Dispatchers.IO) {
        orderDao.updateOrderTotal(orderId, newGrandTotal)
    }

    suspend fun updateOrderStatus(orderId: Long, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        orderDao.updateOrderStatus(orderId, isCompleted)
    }

    suspend fun deleteItemsForOrder(orderId: Long) = withContext(Dispatchers.IO) {
        orderDao.deleteItemsForOrder(orderId)
    }

    suspend fun insertOrderItems(items: List<OrderItemEntity>) = withContext(Dispatchers.IO) {
        orderDao.insertOrderItems(items)
    }

    // --- Reportes (Ejecución en hilo de IO aislado para optimizar interfaz) ---

    suspend fun getDailySales(): List<DailySummary> = withContext(Dispatchers.IO) {
        orderDao.getDailySales()
    }

    suspend fun getWeeklySales(): List<WeeklySummary> = withContext(Dispatchers.IO) {
        orderDao.getWeeklySales()
    }

    suspend fun getMonthlySales(): List<MonthlySummary> = withContext(Dispatchers.IO) {
        orderDao.getMonthlySales()
    }

    suspend fun getTopSellingProducts(start: Long, end: Long): List<TopSellingProduct> = withContext(Dispatchers.IO) {
        orderDao.getTopSellingProducts(start, end)
    }

    suspend fun getProfitByCategory(category: String, inicio: Long, fin: Long, includeCombos: Int = 0): Double? = withContext(Dispatchers.IO) {
        orderDao.getProfitByCategory(category, inicio, fin, includeCombos)
    }

    suspend fun getOrdersBetween(inicio: Long, fin: Long): List<OrderEntity> = withContext(Dispatchers.IO) {
        orderDao.getOrdersBetween(inicio, fin)
    }
}
