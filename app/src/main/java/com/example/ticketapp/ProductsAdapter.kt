package com.example.ticketapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ProductsAdapter(
    private val listener: ProductClickListener
) : ListAdapter<Pair<Product, Int>, ProductsAdapter.ProductViewHolder>(ProductDiffCallback()) {

    // Interfaz para comunicar los clics a la MainActivity
    interface ProductClickListener {
        fun onQuantityChanged(product: Product, change: Int)
        fun onEditPriceClicked(product: Product)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val (product, quantity) = getItem(position)
        holder.bind(product, quantity, listener)
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.productName)
        private val priceTextView: TextView = itemView.findViewById(R.id.productPrice)
        private val quantityTextView: TextView = itemView.findViewById(R.id.productQuantity)
        private val btnMenos: Button = itemView.findViewById(R.id.btnMenos)
        private val btnMas: Button = itemView.findViewById(R.id.btnMas)
        private val cardLayout: View = itemView.findViewById(R.id.cardLayout)

        fun bind(product: Product, quantity: Int, listener: ProductClickListener) {
            nameTextView.text = product.name
            priceTextView.text = "$${"%.2f".format(product.price)}"
            quantityTextView.text = quantity.toString()

            btnMenos.setOnClickListener {
                listener.onQuantityChanged(product, -1)
            }
            btnMas.setOnClickListener {
                listener.onQuantityChanged(product, 1)
            }
            cardLayout.setOnLongClickListener {
                listener.onEditPriceClicked(product)
                true
            }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Pair<Product, Int>>() {
        override fun areItemsTheSame(oldItem: Pair<Product, Int>, newItem: Pair<Product, Int>): Boolean {
            return oldItem.first.id == newItem.first.id
        }

        override fun areContentsTheSame(oldItem: Pair<Product, Int>, newItem: Pair<Product, Int>): Boolean {
            return oldItem == newItem
        }
    }
}