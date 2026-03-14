package com.example.ticketapp.ui.Cards

import com.example.ticketapp.data.menu.MenuItem as DomainMenuItem
import androidx.annotation.DrawableRes


data class MenuItem(
    val id: Int,
    val name: String,
    @DrawableRes val imageRes: Int, // Referencia a R.drawable.nombre
    val price: Double

)


fun DomainMenuItem.toCardItem(): CardMenuItem = CardMenuItem(
    name     = this.name,
    price    = this.price,
    isBurger = this.isBurger
)
