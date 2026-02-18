package com.example.ticketapp

/**
 * Interface para manejar clics en los productos de agua/refrescos.
 */
interface OnAguaClickListener {
    // Clic normal para sumar +1
    fun onAguaClick(agua: AguaSaborEntity)

    // Clic largo para abrir teclado numérico
    fun onAguaLongClick(agua: AguaSaborEntity)
}