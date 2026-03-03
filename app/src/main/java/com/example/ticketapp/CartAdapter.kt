package com.example.ticketapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.ticketapp.MainActivity.Producto
import com.google.android.material.button.MaterialButton

class CartAdapter(
        private var items: MutableList<Producto>,
        private val onUpdate: (Producto, Int) -> Unit, // Int: newQuantity. If 0, delete.
        private val onCommentUpdate: (Producto, String?) -> Unit
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvCartProductName)
        val tvPrice: TextView = view.findViewById(R.id.tvCartProductPrice)
        val btnMinus: MaterialButton = view.findViewById(R.id.btnCartMinus)
        val tvQuantity: TextView = view.findViewById(R.id.tvCartQuantity)
        val btnPlus: MaterialButton = view.findViewById(R.id.btnCartPlus)
        val btnDelete: ImageButton = view.findViewById(R.id.btnCartDelete)
        val btnComment: ImageButton = view.findViewById(R.id.btnCartComment)
        val tvComment: TextView = view.findViewById(R.id.tvCartComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvName.text = item.nombre
        holder.tvPrice.visibility = View.GONE // precio oculto — lógica interna sigue activa
        holder.tvQuantity.text = item.cantidad.toString()

        if (!item.comment.isNullOrEmpty()) {
            holder.tvComment.text = item.comment
            holder.tvComment.visibility = View.VISIBLE
        } else {
            holder.tvComment.text = ""
            holder.tvComment.visibility = View.GONE
        }

        holder.btnMinus.setOnClickListener {
            if (item.cantidad > 1) {
                onUpdate(item, item.cantidad - 1)
            } else {
                // Confirm deletion? Or just delete?
                onUpdate(item, 0)
            }
        }

        holder.btnPlus.setOnClickListener { onUpdate(item, item.cantidad + 1) }

        holder.btnDelete.setOnClickListener { onUpdate(item, 0) }

        holder.btnComment.setOnClickListener {
            val context = holder.itemView.context
            val input = EditText(context)
            input.setText(item.comment ?: "")
            AlertDialog.Builder(context)
                    .setTitle("Comentario para ${item.nombre}")
                    .setView(input)
                    .setPositiveButton("Guardar") { _, _ ->
                        val text = input.text.toString().trim()
                        onCommentUpdate(item, if (text.isNotEmpty()) text else null)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
        }
    }

    fun updateList(newItems: List<Producto>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size
}
