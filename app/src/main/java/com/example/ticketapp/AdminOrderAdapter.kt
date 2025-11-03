package com.example.ticketapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adaptador moderno y eficiente para mostrar la lista de órdenes usando ListAdapter y DiffUtil.
 * Gestiona automáticamente las actualizaciones de la lista con animaciones.
 */
class AdminOrderAdapter(
    private val onDelete: (Long) -> Unit,
    private val onOrderClick: (OrderEntity) -> Unit,
    private val onPrint: (OrderEntity) -> Unit
) : ListAdapter<OrderEntity, AdminOrderAdapter.AdminOrderViewHolder>(OrderDiffCallback()) {

    /**
     * Crea el ViewHolder. El listener se pasa aquí para mayor eficiencia.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminOrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_order, parent, false)
        return AdminOrderViewHolder(view, onDelete, onOrderClick, onPrint)
    }

    /**
     * Vincula los datos de una orden al ViewHolder.
     */
    override fun onBindViewHolder(holder: AdminOrderViewHolder, position: Int) {
        val order = getItem(position)
        holder.bind(order)
    }

    /**
     * Método para actualizar la lista de órdenes.
     * Internamente, utiliza submitList() para una actualización eficiente.
     * Es la forma recomendada de pasar nuevos datos al adaptador.
     * @param newOrders La nueva lista de órdenes a mostrar.
     */
    fun updateOrders(newOrders: List<OrderEntity>) {
        // Llama al método `submitList` de ListAdapter.
        submitList(newOrders)
    }

    /**
     * ViewHolder que representa una orden en la lista.
     * Ahora también se encarga de configurar los listeners una sola vez.
     */
    class AdminOrderViewHolder(
        itemView: View,
        // Pasamos las funciones lambda al ViewHolder
        private val onDelete: (Long) -> Unit,
        private val onOrderClick: (OrderEntity) -> Unit,
        private val onPrint: (OrderEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvOrderInfo: TextView = itemView.findViewById(R.id.tvOrderInfo)
        private val tvOrderTotal: TextView = itemView.findViewById(R.id.tvOrderTotal)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDeleteOrder)
        private val btnPrint: MaterialButton = itemView.findViewById(R.id.btnprintOrder)

        private var currentOrder: OrderEntity? = null

        // El bloque init se ejecuta una sola vez cuando se crea el ViewHolder.
        init {
            // 🔹 Clic para mostrar el resumen del pedido
            itemView.setOnClickListener {
                currentOrder?.let { order -> onOrderClick(order) }
            }
            // 🔹 Botón para eliminar orden
            btnDelete.setOnClickListener {
                currentOrder?.let { order -> onDelete(order.orderId) }
            }
            // 🔹 Botón para imprimir orden
            btnPrint.setOnClickListener {
                currentOrder?.let { order -> onPrint(order) }
            }
        }

        /**
         * Asigna los datos de la orden a la UI.
         */
        fun bind(order: OrderEntity) {
            currentOrder = order // Guarda la orden actual para los listeners
            val date = Date(order.createdAt)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            // Si la mesa es nula, se mostrará "N/A".
            val mesaText = order.mesa ?: "N/A"

            // Muestra un indicador si la orden incluye combos
            val comboLabel = if (order.esCombo) "  (Combo)" else ""

            tvOrderInfo.text = "$mesaText - ${sdf.format(date)}$comboLabel"
            tvOrderTotal.text = "Total: $${"%.2f".format(order.grandTotal)}"
        }
    }
}

/**
 * Clase que calcula las diferencias entre la lista vieja y la nueva para
 * realizar actualizaciones eficientes en el RecyclerView.
 */
class OrderDiffCallback : DiffUtil.ItemCallback<OrderEntity>() {
    override fun areItemsTheSame(oldItem: OrderEntity, newItem: OrderEntity): Boolean {
        // Los items son los mismos si sus IDs son iguales.
        return oldItem.orderId == newItem.orderId
    }

    override fun areContentsTheSame(oldItem: OrderEntity, newItem: OrderEntity): Boolean {
        // El contenido es el mismo si los objetos son idénticos (data class).
        return oldItem == newItem
    }
}
