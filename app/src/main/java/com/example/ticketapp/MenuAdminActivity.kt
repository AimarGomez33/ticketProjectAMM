package com.example.ticketapp

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MenuAdminActivity : AppCompatActivity(), MenuAdminAdapter.AdminProductClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MenuAdminAdapter
    private lateinit var fab: FloatingActionButton
    private lateinit var toolbar: MaterialToolbar

    // Acceso a la base de datos
    private val database by lazy { AppDatabase.getDatabase(this, lifecycleScope) }
    private val productDao by lazy { database.productDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_admin)

        // Configurar la Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Configurar RecyclerView y Adaptador
        recyclerView = findViewById(R.id.adminProductsRecyclerView)
        adapter = MenuAdminAdapter(this)
        recyclerView.adapter = adapter

        // Configurar el Botón Flotante de Añadir
        fab = findViewById(R.id.fabAddProduct)
        fab.setOnClickListener {
            showAddProductDialog()
        }

        // Observar los productos de la base de datos
        observeProducts()
    }

    private fun observeProducts() {
        lifecycleScope.launch {
            productDao.getAllProducts().collectLatest { products ->
                adapter.submitList(products)
            }
        }
    }

    // --- Implementación de los clics del adaptador ---

    override fun onEditClicked(product: Product) {
        showEditProductDialog(product)
    }

    override fun onDeleteClicked(product: Product) {
        showDeleteConfirmationDialog(product)
    }

    private fun showEditProductDialog(product: Product) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar Producto")

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }

        val nameInput = EditText(this).apply {
            hint = "Nombre del producto"
            setText(product.name)
            selectAll()
        }

        val priceInput = EditText(this).apply {
            hint = "Precio"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(product.price.toString())
        }

        container.addView(nameInput)
        container.addView(priceInput)
        builder.setView(container)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val newName = nameInput.text.toString().trim()
            val newPrice = priceInput.text.toString().toDoubleOrNull()

            if (newName.isNotEmpty() && newPrice != null && newPrice >= 0) {
                lifecycleScope.launch(Dispatchers.IO) {
                    productDao.update(product.copy(name = newName, price = newPrice))
                }
                dialog.dismiss()
                Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Por favor, introduce datos válidos", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showDeleteConfirmationDialog(product: Product) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Eliminar Producto")
        builder.setMessage("¿Estás seguro de que quieres eliminar \"${product.name}\"?")

        builder.setPositiveButton("Eliminar") { dialog, _ ->
            lifecycleScope.launch(Dispatchers.IO) {
                productDao.delete(product)
            }
            dialog.dismiss()
            Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showAddProductDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Añadir Producto")

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }

        val nameInput = EditText(this).apply {
            hint = "Nombre del producto"
        }

        val priceInput = EditText(this).apply {
            hint = "Precio"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        container.addView(nameInput)
        container.addView(priceInput)
        builder.setView(container)

        builder.setPositiveButton("Añadir") { dialog, _ ->
            val name = nameInput.text.toString().trim()
            val price = priceInput.text.toString().toDoubleOrNull()

            if (name.isNotEmpty() && price != null && price >= 0) {
                lifecycleScope.launch(Dispatchers.IO) {
                    productDao.insert(Product(name = name, price = price))
                }
                dialog.dismiss()
                Toast.makeText(this, "Producto añadido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Por favor, introduce datos válidos", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }
}