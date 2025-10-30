package com.example.ticketapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adaptador para mostrar la lista de órdenes guardadas en la pantalla de administración.
 * Cada elemento muestra la mesa o tipo (para llevar), fecha/hora y total.
 * Incluye botones para eliminar y un clic general para ver el resumen.
 */
class AdminOrderAdapter(
    private val orders: MutableList<OrderEntity>,
    private val onDelete: (Long) -> Unit,
    private val onOrderClick: (OrderEntity) -> Unit   ,
    private val onPrint: (OrderEntity) -> Unit
) : RecyclerView.Adapter<AdminOrderAdapter.AdminOrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminOrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_order, parent, false)
        return AdminOrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminOrderViewHolder, position: Int) {
        val order = orders[position]
        holder.bind(order)

        // 🔹 Botón para eliminar orden
        holder.btnDelete.setOnClickListener {
            onDelete(order.orderId)
        }

        // 🔹 Clic para mostrar el resumen del pedido
        holder.itemView.setOnClickListener {
            onOrderClick(order)
        }

        holder.btnPrint.setOnClickListener { onPrint(order) }
    }

    override fun getItemCount(): Int = orders.size

    /**
     * Reemplaza la lista de órdenes con una nueva y refresca la vista.
     */
    fun updateOrders(newOrders: List<OrderEntity>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged()
    }

    /**
     * ViewHolder para representar una orden en la lista.
     */
    inner class AdminOrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderInfo: TextView = itemView.findViewById(R.id.tvOrderInfo)
        val tvOrderTotal: TextView = itemView.findViewById(R.id.tvOrderTotal)
        val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDeleteOrder)
        val btnPrint: MaterialButton = itemView.findViewById(R.id.btnprintOrder)


        /**
         * Asigna los datos de la orden a la UI.
         */
        fun bind(order: OrderEntity) {
            val date = Date(order.createdAt)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val mesaText = order.mesa ?: "Para llevar"

            // 🔹 Muestra un indicador si la orden incluye combos
            val comboLabel = if (order.esCombo) " 🧃 (Combo)" else ""

            tvOrderInfo.text = "$mesaText - ${sdf.format(date)}$comboLabel"
            tvOrderTotal.text = "Total: $${"%.2f".format(order.grandTotal)}"
        }

    }
}
