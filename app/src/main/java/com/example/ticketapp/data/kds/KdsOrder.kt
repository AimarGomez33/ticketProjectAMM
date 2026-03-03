package com.example.ticketapp.data.kds

data class KdsOrder(
        val id: String = "",
        val timestamp: Long = System.currentTimeMillis(),
        val tableNumber: String = "",
        val waiterName: String = "",
        val items: List<KdsOrderItem> = emptyList(),
        val status: String = "PENDING", // Can be "PENDING", "COMPLETED"
        val isReOrder: Boolean = false // true = more items added to an existing order
)
