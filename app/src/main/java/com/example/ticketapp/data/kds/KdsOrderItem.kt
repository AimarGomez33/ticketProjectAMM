package com.example.ticketapp.data.kds

data class KdsOrderItem(
        val id: String = "",
        val productName: String = "",
        val quantity: Int = 1,
        val notes: String = "",
        val status: String = "PENDING" // Can be "PENDING", "READY"
)
