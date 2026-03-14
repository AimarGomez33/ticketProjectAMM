package com.example.ticketapp

/**
 * Interface para manejar clics en los productos de refrescos.
 */
interface OnRefrescoClickListener {
    // Clic largo para abrir vista de múltiples cantidades
    fun onRefrescoLongClick(refresco: RefrescoEntity)
}
