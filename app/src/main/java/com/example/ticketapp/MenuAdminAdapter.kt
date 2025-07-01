package com.example.ticketapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class MenuAdminAdapter(
    private val listener: AdminProductClickListener
) : ListAdapter<Product, MenuAdminAdapter.ProductViewHolder>(DiffCallback()) {

    // Interfaz para comunicar los clics a la actividad
    interface AdminProductClickListener {
        fun onEditClicked(product: Product)
        fun onDeleteClicked(product: Product)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, listener)
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.adminProductName)
        private val priceTextView: TextView = itemView.findViewById(R.id.adminProductPrice)
        private val editButton: Button = itemView.findViewById(R.id.editButton)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        fun bind(product: Product, listener: AdminProductClickListener) {
            nameTextView.text = product.name
            priceTextView.text = "$${"%.2f".format(product.price)}"

            editButton.setOnClickListener { listener.onEditClicked(product) }
            deleteButton.setOnClickListener { listener.onDeleteClicked(product) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}