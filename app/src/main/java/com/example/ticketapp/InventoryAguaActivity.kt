package com.example.ticketapp

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup // Añadido para LayoutParams
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout // Añadido para mejorar el diseño del diálogo
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.text2.input.delete
import androidx.compose.foundation.text2.input.insert
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ticketapp.AguaSaborDao
import com.example.ticketapp.AguaSaborEntity
import com.example.ticketapp.AppDatabase
import androidx.room.RoomDatabase


// Asegúrate de que estas clases existan en tu proyecto
// import com.example.ticketapp.database.AppDatabase

class InventoryAguaActivity : AppCompatActivity(), InventoryAguaAdapter.OnItemClickListener {

    private lateinit var adapter: InventoryAguaAdapter
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory_agua)

        // Inicializar DB
        db = AppDatabase.getDatabase(this, lifecycleScope)

        val rv = findViewById<RecyclerView>(R.id.rvSabores)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = InventoryAguaAdapter(this)
        rv.adapter = adapter

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAgregarSabor).setOnClickListener { showAddDialog() }


        lifecycleScope.launch {
            db.aguassaborDao().getAllFlow().collectLatest { list ->
                adapter.submitList(list)
            }
        }
    }

    private fun showAddDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 10)
        }

        val etName = EditText(this).apply {
            hint = "Nombre del Sabor (ej. Horchata)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        layout.addView(etName)

        val etQty = EditText(this).apply {
            hint = "Cantidad Inicial"
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(etQty)

        AlertDialog.Builder(this)
            .setTitle("Nuevo Sabor")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val name = etName.text.toString().trim()
                val qtyStr = etQty.text.toString().trim()
                val id = adapter.currentList.size + 1
                if (name.isNotEmpty() && qtyStr.isNotEmpty()) {
                    saveSabor(name, qtyStr.toIntOrNull() ?: 0)
                } else {
                    Toast.makeText(this, "Datos inválidos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditDialog(agua: AguaSaborEntity) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 10)
        }

        val etName = EditText(this).apply {
            hint = "Nombre"
            setText(agua.flavorName)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        layout.addView(etName)

        val etQty = EditText(this).apply {
            hint = "Cantidad Disponible"
            setText(agua.quantityAvailable.toString())
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(etQty)

        AlertDialog.Builder(this)
            .setTitle("Editar Sabor")
            .setView(layout)
            .setPositiveButton("Actualizar") { _, _ ->
                val name = etName.text.toString().trim()
                val qty = etQty.text.toString().trim().toIntOrNull() ?: 0
                val id = agua.id
                updateSabor(agua.copy(flavorName = name, quantityAvailable = qty))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveSabor(name: String, qty: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.aguassaborDao().insert(
                AguaSaborEntity(
                    flavorName = name,
                    quantityAvailable = qty,
                    id = adapter.currentList.size + 1
                )
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(this@InventoryAguaActivity, "Guardado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSabor(agua: AguaSaborEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.aguassaborDao().update(agua)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@InventoryAguaActivity, "Actualizado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onEditClick(agua: AguaSaborEntity) {
        showEditDialog(agua)
    }

    override fun onDeleteClick(agua: AguaSaborEntity) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar ${agua.flavorName}?")
            .setMessage("¿Estás seguro de eliminar este sabor?")
            .setPositiveButton("Sí") { _, _ ->
                // ✅ CORRECCIÓN: Añadido feedback al usuario tras eliminar
                lifecycleScope.launch(Dispatchers.IO) {
                    db.aguassaborDao().delete(agua)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@InventoryAguaActivity, "Eliminado", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()

    }

    class InventoryAguaActivity : AppCompatActivity(),
        InventoryAguaAdapter.OnItemClickListener,
        OnAguaClickListener {

        private lateinit var adapter: InventoryAguaAdapter
        private lateinit var db: AppDatabase

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_inventory_agua)

            db = AppDatabase.getDatabase(this, lifecycleScope)

            val rv = findViewById<RecyclerView>(R.id.rvSabores)
            rv.layoutManager = LinearLayoutManager(this)

            // Ensure your adapter constructor and interface implementation match
            adapter = InventoryAguaAdapter(this)
            rv.adapter = adapter

            findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
            findViewById<Button>(R.id.btnAgregarSabor).setOnClickListener { showAddDialog() }

            lifecycleScope.launch {
                db.aguassaborDao().getAllFlow().collectLatest { list ->
                    adapter.submitList(list)
                }
            }
        }

        // Implementation of OnAguaClickListener (Fixes the error)
        override fun onAguaClick(agua: AguaSaborEntity) {
            // Define what happens when the item is clicked, e.g., show numeric keypad
            showNumericKeypad(agua.flavorName, agua.quantityAvailable) { nuevaCantidad ->
                actualizarCantidad(agua, nuevaCantidad)
            }
        }

        override fun onAguaLongClick(agua: AguaSaborEntity) {
            TODO("Not yet implemented")
        }

        private fun showAddDialog() {
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(60, 40, 60, 10)
            }

            val etName = EditText(this).apply {
                hint = "Nombre del Sabor"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }
            layout.addView(etName)

            val etQty = EditText(this).apply {
                hint = "Cantidad Inicial"
                inputType = InputType.TYPE_CLASS_NUMBER
            }
            layout.addView(etQty)

            AlertDialog.Builder(this)
                .setTitle("Nuevo Sabor")
                .setView(layout)
                .setPositiveButton("Guardar") { _, _ ->
                    val name = etName.text.toString().trim()
                    val qtyStr = etQty.text.toString().trim()
                    val id = adapter.currentList.size + 1
                    if (name.isNotEmpty() && qtyStr.isNotEmpty()) {
                        saveSabor(name, qtyStr.toIntOrNull() ?: 0)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        private fun showNumericKeypad(
            flavorName: String,
            currentQty: Int,
            onConfirm: (Int) -> Unit
        ) {
            val inputField = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                setText(currentQty.toString())
                selectAll()
                gravity = Gravity.CENTER
                textSize = 24f
            }

            val container = FrameLayout(this).apply {
                val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(50, 40, 50, 40) }
                inputField.layoutParams = params
                addView(inputField)
            }

            AlertDialog.Builder(this)
                .setTitle("Inventario: $flavorName")
                .setMessage("Ingrese la cantidad total disponible:")
                .setView(container)
                .setPositiveButton("Guardar") { _, _ ->
                    val valor = inputField.text.toString().toIntOrNull() ?: currentQty
                    onConfirm(valor)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        private fun saveSabor(name: String, qty: Int) {
            lifecycleScope.launch(Dispatchers.IO) {
                db.aguassaborDao()
                    .insert(AguaSaborEntity(flavorName = name, quantityAvailable = qty, id = 0))
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@InventoryAguaActivity, "Guardado", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        private fun actualizarCantidad(agua: AguaSaborEntity, nuevaCantidad: Int) {
            lifecycleScope.launch(Dispatchers.IO) {
                val aguaActualizada = agua.copy(quantityAvailable = nuevaCantidad)
                db.aguassaborDao().update(aguaActualizada)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@InventoryAguaActivity, "Actualizado", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        override fun onEditClick(agua: AguaSaborEntity) {
            showNumericKeypad(agua.flavorName, agua.quantityAvailable) { nuevaCantidad ->
                actualizarCantidad(agua, nuevaCantidad)
            }
        }

        override fun onDeleteClick(agua: AguaSaborEntity) {
            AlertDialog.Builder(this)
                .setTitle("Eliminar ${agua.flavorName}?")
                .setPositiveButton("Sí") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.aguassaborDao().delete(agua)
                    }
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

}