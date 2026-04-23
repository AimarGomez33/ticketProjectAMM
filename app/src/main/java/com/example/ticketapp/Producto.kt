package com.example.ticketapp

data class Producto(
    val nombre: String,
    val precio: Double,
    val cantidad: Int,
    val esCombo: Boolean
) {
    val total: Double
        get() = precio * cantidad
}
