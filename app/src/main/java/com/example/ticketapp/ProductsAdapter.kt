package com.example.ticketapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

sealed class GroupedItem {
    data class Header(val category: String) : GroupedItem()
    data class ProductItem(val product: Product, val quantity: Int) : GroupedItem()
}

class ProductsAdapter(
    private val listener: ProductClickListener
) : ListAdapter<GroupedItem, RecyclerView.ViewHolder>(GroupedDiffCallback()) {

    interface ProductClickListener {
        fun onQuantityChanged(product: Product, change: Int)
        fun onEditPriceClicked(product: Product)
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PRODUCT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is GroupedItem.Header -> TYPE_HEADER
            is GroupedItem.ProductItem -> TYPE_PRODUCT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_product, parent, false)
                ProductViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is GroupedItem.Header -> (holder as HeaderViewHolder).bind(item)
            is GroupedItem.ProductItem -> (holder as ProductViewHolder).bind(item.product, item.quantity, listener)
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.headerText)
        fun bind(item: GroupedItem.Header) {
            headerText.text = item.category
        }
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

    class GroupedDiffCallback : DiffUtil.ItemCallback<GroupedItem>() {
        override fun areItemsTheSame(oldItem: GroupedItem, newItem: GroupedItem): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: GroupedItem, newItem: GroupedItem): Boolean {
            return oldItem == newItem
        }
    }
}
