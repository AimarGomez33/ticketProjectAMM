package com.example.ticketapp

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa un producto en el sistema.
 * Puede ser un platillo normal o un combo.
 */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Nombre del producto (ej: "Hamburguesa Clásica", "Pozole Grande"). */
    val name: String,

    /** Precio unitario del producto. */
    val price: Double,

    /** Categoría o grupo al que pertenece (ej: "Hamburguesas", "Tacos", "Alitas"). */
    val category: String,

    /** Indica si este producto es un combo (true = combo, false = normal). */
    val esCombo: Boolean = false
)


val defaultProducts = listOf(
    //  TACOS
    Product(name = "Taco (c/u)", price = 25.0, category = "Tacos"),
    Product(name = "Taco con queso (c/u)", price = 30.0, category = "Tacos"),

    //  HAMBURGUESAS
    Product(name = "Hamburguesa Clásica", price = 85.0, category = "Hamburguesas"),
    Product(name = "Hamburguesa Hawaiana", price = 90.0, category = "Hamburguesas"),
    Product(name = "Hamburguesa Pollo", price = 90.0, category = "Hamburguesas"),
    Product(name = "Hamburguesa Champiñones", price = 95.0, category = "Hamburguesas"),
    Product(name = "Hamburguesa Arrachera", price = 110.0, category = "Hamburguesas"),
    Product(name = "Hamburguesa Magey", price = 100.0, category = "Hamburguesas"),
    Product(name = "Hamburguesa Doble", price = 120.0, category = "Hamburguesas"),

    //  ALITAS
    Product(name = "Alitas 6 pzas", price = 85.0, category = "Alitas"),
    Product(name = "Alitas 10 pzas", price = 120.0, category = "Alitas"),
    Product(name = "Alitas 15 pzas", price = 165.0, category = "Alitas")
)




