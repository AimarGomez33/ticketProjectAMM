package com.example.ticketapp

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.ViewModelProvider

class MainViewModel(
    private val repository: OrderRepository,
    val printer: PrinterManager
) : ViewModel() {

    // --- Cantidades Genéricas ---
    private val _quantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    val quantities: StateFlow<Map<String, Int>> = _quantities.asStateFlow()

    // --- Precios Base Genéricos --- 
    // Nombre del producto -> Precio
    private val genericPrices = mutableMapOf<String, Double>()

    // --- Cantidades de Hamburguesas ---
    private val _cantidadesNormales = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cantidadesNormales: StateFlow<Map<String, Int>> = _cantidadesNormales.asStateFlow()

    private val _cantidadesCombo = MutableStateFlow<Map<String, Int>>(emptyMap())
    val cantidadesCombo: StateFlow<Map<String, Int>> = _cantidadesCombo.asStateFlow()

    // --- Precios de Hamburguesas ---
    private val hamburguesasPrices = mutableMapOf<String, Double>()
    
    // Configuración de Extra Combo
    var extraComboPrice: Double = 30.0

    // --- Total ---
    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total.asStateFlow()

    fun setGenericPrice(name: String, price: Double) {
        genericPrices[name] = price
    }

    fun setHamburguesaPrice(name: String, price: Double) {
        hamburguesasPrices[name] = price
    }
    
    fun clearPrices() {
        genericPrices.clear()
        hamburguesasPrices.clear()
    }

    // --- Operaciones Genéricas ---
    fun updateQuantity(name: String, qty: Int) {
        _quantities.update { current ->
            current.toMutableMap().apply { this[name] = qty.coerceAtLeast(0) }
        }
        recalculateTotal()
    }

    fun getQuantity(name: String): Int = _quantities.value[name] ?: 0

    fun incrementQuantity(name: String, amount: Int = 1) {
        updateQuantity(name, getQuantity(name) + amount)
    }

    // --- Operaciones Hamburguesas ---
    fun updateHamburguesaNormal(name: String, qty: Int) {
        _cantidadesNormales.update { current ->
            current.toMutableMap().apply { this[name] = qty.coerceAtLeast(0) }
        }
        recalculateTotal()
    }

    fun getHamburguesaNormal(name: String): Int = _cantidadesNormales.value[name] ?: 0

    fun updateHamburguesaCombo(name: String, qty: Int) {
        _cantidadesCombo.update { current ->
            current.toMutableMap().apply { this[name] = qty.coerceAtLeast(0) }
        }
        recalculateTotal()
    }

    fun getHamburguesaCombo(name: String): Int = _cantidadesCombo.value[name] ?: 0

    // --- Limpieza Total ---
    fun clearAllQuantities() {
        _quantities.value = emptyMap()
        _cantidadesNormales.value = emptyMap()
        _cantidadesCombo.value = emptyMap()
        recalculateTotal()
    }

    // --- Lógica Central ---
    fun recalculateTotal() {
        var calculatedTotal = 0.0

        // Sumar hamburguesas (Normales y Combos)
        for ((nombre, precioBase) in hamburguesasPrices) {
            val normales = _cantidadesNormales.value[nombre] ?: 0
            val combos = _cantidadesCombo.value[nombre] ?: 0
            calculatedTotal += (precioBase * normales) + ((precioBase + extraComboPrice) * combos)
        }

        // Sumar genéricos
        for ((nombre, cantidad) in _quantities.value) {
            val precio = genericPrices[nombre] ?: 0.0
            calculatedTotal += precio * cantidad
        }

        _total.value = calculatedTotal
    }

    // --- Operaciones de Base de Datos (Delegadas al Repository) ---
    suspend fun getAllOrders() = repository.getAllOrders()
    
    fun getAllOrdersFlow() = repository.getAllOrdersFlow()

    suspend fun getItemsForOrder(orderId: Long) = repository.getItemsForOrder(orderId)

    suspend fun updateOrderStatus(orderId: Long, isCompleted: Boolean) = repository.updateOrderStatus(orderId, isCompleted)

    suspend fun deleteOrderById(orderId: Long) = repository.eliminarPedido(orderId)
    
    suspend fun guardarPedidoBD(order: OrderEntity, items: List<OrderItemEntity>) = repository.guardarPedido(order, items)

    suspend fun updateOrderTotalBD(orderId: Long, newGrandTotal: Double) = repository.updateOrderTotal(orderId, newGrandTotal)

    suspend fun deleteItemsForOrderBD(orderId: Long) = repository.deleteItemsForOrder(orderId)

    suspend fun insertOrderItemsBD(items: List<OrderItemEntity>) = repository.insertOrderItems(items)

    // --- Reportes ---
    suspend fun getDailySales() = repository.getDailySales()
    suspend fun getWeeklySales() = repository.getWeeklySales()
    suspend fun getMonthlySales() = repository.getMonthlySales()
    suspend fun getTopSellingProducts(start: Long, end: Long) = repository.getTopSellingProducts(start, end)
    suspend fun getProfitByCategory(category: String, inicio: Long, fin: Long, includeCombos: Int = 0) = repository.getProfitByCategory(category, inicio, fin, includeCombos)
    suspend fun getOrdersBetween(inicio: Long, fin: Long) = repository.getOrdersBetween(inicio, fin)

    // --- Impresión (delegada al PrinterManager) ---
    suspend fun printTicket(
        productosSeleccionados: List<Producto>,
        mesaInfo: String,
        mostrarCuenta: Boolean,
        numeroCuenta: String
    ) = printer.printTicket(productosSeleccionados, mesaInfo, mostrarCuenta, numeroCuenta)

    suspend fun generarTextoTicket(
        productosSeleccionados: List<Producto>,
        mesaInfo: String = "",
        mostrarCuenta: Boolean = false,
        numeroCuenta: String = ""
    ) = printer.generarTextoTicket(
        productosSeleccionados = productosSeleccionados,
        printerType = printer.selectedPrinterType,
        mesaInfo = mesaInfo,
        mostrarCuenta = mostrarCuenta,
        numeroCuenta = numeroCuenta
    )

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val repository: OrderRepository,
        private val printerManager: PrinterManager
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository, printerManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
