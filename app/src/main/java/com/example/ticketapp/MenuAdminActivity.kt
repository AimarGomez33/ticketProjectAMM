package com.example.ticketapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
            // La lógica para añadir un nuevo producto vendrá aquí
            Toast.makeText(this, "Añadir nuevo producto...", Toast.LENGTH_SHORT).show()
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
        // La lógica para editar un producto vendrá aquí
        Toast.makeText(this, "Editar: ${product.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteClicked(product: Product) {
        // La lógica para borrar un producto vendrá aquí
        Toast.makeText(this, "Borrar: ${product.name}", Toast.LENGTH_SHORT).show()
    }
}