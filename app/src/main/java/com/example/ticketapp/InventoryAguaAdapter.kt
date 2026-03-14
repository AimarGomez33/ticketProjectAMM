package com.example.ticketapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class InventoryAguaAdapter(
    private val listener: OnItemClickListener,
    private val longClickListener: OnAguaClickListener? = null
) :
        ListAdapter<AguaSaborEntity, InventoryAguaAdapter.ViewHolder>(DiffCallback()) {

    interface OnItemClickListener {
        fun onEditClick(agua: AguaSaborEntity)
        fun onDeleteClick(agua: AguaSaborEntity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_agua_inventory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFlavor: TextView = itemView.findViewById(R.id.tvFlavorName)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)

        fun bind(item: AguaSaborEntity) {
            tvFlavor.text = "Sabor: ${item.flavorName}"
            tvQuantity.text = "Disponible: ${item.quantityAvailable}"

            btnEdit.setOnClickListener { listener.onEditClick(item) }
            btnDelete.setOnClickListener { listener.onDeleteClick(item) }
            
            // Long click en el card completo
            itemView.setOnLongClickListener {
                longClickListener?.onAguaLongClick(item)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AguaSaborEntity>() {
        override fun areItemsTheSame(oldItem: AguaSaborEntity, newItem: AguaSaborEntity) =
                oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AguaSaborEntity, newItem: AguaSaborEntity) =
                oldItem == newItem
    }
}
