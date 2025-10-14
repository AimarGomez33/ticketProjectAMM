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
 * RecyclerView adapter for displaying a list of recorded orders in the
 * administration screen. Each row shows the table (or take‑out), the
 * timestamp when the order was created, the total amount and a delete button.
 *
 * @param orders    Mutable list backing the adapter; contents can be
 *                  replaced via [updateOrders].
 * @param onDelete  Callback invoked when the delete button for an order is
 *                  tapped. The long parameter is the orderId to remove.
 */
class AdminOrderAdapter(
    private val orders: MutableList<OrderEntity>,
    private val onDelete: (Long) -> Unit,
    private val onOrderClick: (OrderEntity) -> Unit
) : RecyclerView.Adapter<AdminOrderAdapter.AdminOrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminOrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_order, parent, false)
        return AdminOrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminOrderViewHolder, position: Int) {
        val order = orders[position]
        holder.bind(order)
        holder.btnDelete.setOnClickListener {
            onDelete(order.orderId)
        }
        // Llama al callback de clic cuando se pulsa en el elemento completo
        holder.itemView.setOnClickListener {
            onOrderClick(order)
        }
    }

    override fun getItemCount(): Int = orders.size

    /**
     * Replaces the contents of the adapter with the provided list and
     * notifies that the data set has changed. This method should be
     * called from the main thread.
     */
    fun updateOrders(newOrders: List<OrderEntity>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged()
    }

    inner class AdminOrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderInfo: TextView = itemView.findViewById(R.id.tvOrderInfo)
        val tvOrderTotal: TextView = itemView.findViewById(R.id.tvOrderTotal)
        val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDeleteOrder)

        /**
         * Binds the order data to the UI elements. Formats the createdAt
         * timestamp into a human readable date and time string and sets
         * the total with a currency symbol.
         */
        fun bind(order: OrderEntity) {
            // Format timestamp for display
            val date = Date(order.createdAt)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val mesaText = order.mesa ?: "Para llevar"
            val orderInfoText = "$mesaText - ${sdf.format(date)}"
            tvOrderInfo.text = orderInfoText
            tvOrderTotal.text = "Total: $${"%.2f".format(order.grandTotal)}"
        }
    }

    companion object
}