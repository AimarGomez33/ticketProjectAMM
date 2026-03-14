package com.example.ticketapp

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InventoryRefrescoActivity :
        AppCompatActivity(), InventoryRefrescoAdapter.OnItemClickListener, OnRefrescoClickListener {

    private lateinit var adapter: InventoryRefrescoAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory_refresco)

        // Inicializar DB
        db = AppDatabase.getDatabase(this, lifecycleScope)

        val rv = findViewById<RecyclerView>(R.id.rvRefrescos)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = InventoryRefrescoAdapter(this, this)
        rv.adapter = adapter

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAgregarRefresco).setOnClickListener { showAddDialog() }

        lifecycleScope.launch {
            db.refrescoDao().getAllFlow().collectLatest { list -> adapter.submitList(list) }
        }
    }

    override fun onRefrescoLongClick(refresco: RefrescoEntity) {
        // Abre la pantalla de agregar múltiples unidades al carrito
        val intent = android.content.Intent(this, DynamicItemCartActivity::class.java).apply {
            putExtra("product_name", refresco.flavorName)
            putExtra("product_type", "refresco")
        }
        startActivity(intent)
    }

    private fun showAddDialog() {
        val layout =
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(60, 40, 60, 10)
                }

        val etName =
                EditText(this).apply {
                    hint = "Nombre del Refresco (ej. Coca Cola)"
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                }
        layout.addView(etName)

        val etQty =
                EditText(this).apply {
                    hint = "Cantidad Inicial"
                    inputType = InputType.TYPE_CLASS_NUMBER
                }
        layout.addView(etQty)

        AlertDialog.Builder(this)
                .setTitle("Nuevo Refresco")
                .setView(layout)
                .setPositiveButton("Guardar") { _, _ ->
                    val name = etName.text.toString().trim()
                    val qtyStr = etQty.text.toString().trim()
                    if (name.isNotEmpty() && qtyStr.isNotEmpty()) {
                        saveRefresco(name, qtyStr.toIntOrNull() ?: 0)
                    } else {
                        Toast.makeText(this, "Datos inválidos", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
    }

    private fun showEditDialog(refresco: RefrescoEntity) {
        val layout =
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(60, 40, 60, 10)
                }

        val etName =
                EditText(this).apply {
                    hint = "Nombre"
                    setText(refresco.flavorName)
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                }
        layout.addView(etName)

        val etQty =
                EditText(this).apply {
                    hint = "Cantidad Disponible"
                    setText(refresco.quantityAvailable.toString())
                    inputType = InputType.TYPE_CLASS_NUMBER
                }
        layout.addView(etQty)

        AlertDialog.Builder(this)
                .setTitle("Editar Refresco")
                .setView(layout)
                .setPositiveButton("Actualizar") { _, _ ->
                    val name = etName.text.toString().trim()
                    val qty = etQty.text.toString().trim().toIntOrNull() ?: 0
                    updateRefresco(refresco.copy(flavorName = name, quantityAvailable = qty))
                }
                .setNegativeButton("Cancelar", null)
                .show()
    }

    private fun saveRefresco(name: String, qty: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.refrescoDao().insert(RefrescoEntity(flavorName = name, quantityAvailable = qty))
            withContext(Dispatchers.Main) {
                Toast.makeText(this@InventoryRefrescoActivity, "Guardado", Toast.LENGTH_SHORT)
                        .show()
            }
        }
    }

    private fun updateRefresco(refresco: RefrescoEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.refrescoDao().update(refresco)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@InventoryRefrescoActivity, "Actualizado", Toast.LENGTH_SHORT)
                        .show()
            }
        }
    }

    override fun onEditClick(refresco: RefrescoEntity) {
        showEditDialog(refresco)
    }

    override fun onDeleteClick(refresco: RefrescoEntity) {
        AlertDialog.Builder(this)
                .setTitle("Eliminar ${refresco.flavorName}?")
                .setMessage("¿Estás seguro de eliminar este refresco?")
                .setPositiveButton("Sí") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.refrescoDao().delete(refresco)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                            this@InventoryRefrescoActivity,
                                            "Eliminado",
                                            Toast.LENGTH_SHORT
                                    )
                                    .show()
                        }
                    }
                }
                .setNegativeButton("No", null)
                .show()
    }
}
