// MenuAdminActivity.kt (Modificaciones para añadir categorías)
package com.example.ticketapp

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
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

    private var categoriesList = mutableListOf<String>()
    private lateinit var categoriesAdapter: ArrayAdapter<String>

    private val database by lazy { AppDatabase.getDatabase(this, lifecycleScope) }
    private val productDao by lazy { database.productDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_admin)

        try {
            // Inicializar adaptador de categorías ANTES de que se observe
            // Añadiremos una opción placeholder que se eliminará si hay categorías reales
            categoriesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoriesList)
            categoriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            toolbar = findViewById(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

            recyclerView = findViewById(R.id.adminProductsRecyclerView)
            adapter = MenuAdminAdapter(this) // 'this' es el AdminProductClickListener
            recyclerView.adapter = adapter

            fab = findViewById(R.id.fabAddProduct)
            fab.setOnClickListener { showAddProductDialog() }

            observeProducts()
            observeCategories()
        } catch (e: Exception) {
            Log.e("MenuAdmin", "Error en onCreate", e)
            Toast.makeText(this, "Error inicializando pantalla", Toast.LENGTH_LONG).show()
        }
    }

    private fun observeProducts() {
        lifecycleScope.launch {
            try {
                productDao.getAllProducts().collectLatest { products ->
                    adapter.submitList(products)
                }
            } catch (e: Exception) {
                Log.e("MenuAdmin", "Error observando productos", e)
            }
        }
    }

    private fun observeCategories() {
        lifecycleScope.launch {
            try {
                productDao.getAllCategories().collectLatest { fetchedCategories ->
                    val currentSelection = if (categoriesList.isNotEmpty() && categoriesList.firstOrNull() != getString(R.string.placeholder_select_category) && categoriesList.firstOrNull() != getString(R.string.no_categories_available)) {
                        // Lógica para mantener la selección si es posible, aunque en diálogos puede ser menos crítico
                        null // Simplificamos por ahora para los diálogos
                    } else null

                    categoriesList.clear()
                    if (fetchedCategories.isEmpty()) {
                        categoriesList.add(getString(R.string.no_categories_available)) // Placeholder si no hay categorías
                    } else {
                        categoriesList.add(getString(R.string.placeholder_select_category)) // Placeholder para selección
                        categoriesList.addAll(fetchedCategories)
                    }
                    categoriesAdapter.notifyDataSetChanged()

                    // Intentar restaurar la selección (más relevante si el spinner estuviera siempre visible)
                    // currentSelection?.let { sel -> categoriesSpinner.setSelection(categoriesList.indexOf(sel)) }
                }
            } catch (e: Exception) {
                Log.e("MenuAdmin", "Error observando categorías", e)
                // Añadir un estado por defecto si falla la carga
                categoriesList.clear()
                categoriesList.add(getString(R.string.error_loading_categories))
                categoriesAdapter.notifyDataSetChanged()
            }
        }
    }


    override fun onEditClicked(product: Product) {
        showEditProductDialog(product)
    }

    override fun onDeleteClicked(product: Product) {
        showDeleteConfirmationDialog(product)
    }

    private fun showAddProductDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Añadir Producto")

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20) // Ajusta el padding según necesites
        }

        val nameInput = EditText(this).apply { hint = "Nombre del producto" }
        val priceInput = EditText(this).apply {
            hint = "Precio"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        container.addView(nameInput)
        container.addView(priceInput)

        // Sección de Categoría
        container.addView(TextView(this).apply {
            text = "Categoría Existente:"
            textSize = 16f // Un poco más grande para el título de la sección
            setPadding(0, 16, 0, 4)
        })
        val categorySpinner = Spinner(this).apply {
            adapter = categoriesAdapter
            // Si la primera opción es un placeholder, no la hagas seleccionable por defecto
            if (categoriesList.isNotEmpty() && (categoriesList.first() == getString(R.string.placeholder_select_category) || categoriesList.first() == getString(R.string.no_categories_available))) {
                // No hacer nada o setSelection(0, false)
            }
        }
        container.addView(categorySpinner)

        container.addView(TextView(this).apply {
            text = "O Crear Nueva Categoría:"
            textSize = 16f
            setPadding(0, 16, 0, 4)
        })
        val newCategoryInput = EditText(this).apply {
            hint = "Nombre de la nueva categoría"
        }
        container.addView(newCategoryInput)

        builder.setView(container)

        builder.setPositiveButton("Añadir") { dialog, _ ->
            val name = nameInput.text.toString().trim()
            val priceText = priceInput.text.toString()
            val newCategoryName = newCategoryInput.text.toString().trim()
            var selectedCategoryName: String? = null

            if (categorySpinner.selectedItemPosition > 0 || (categoriesList.isNotEmpty() && categoriesList.first() != getString(R.string.placeholder_select_category) && categoriesList.first() != getString(R.string.no_categories_available)) ) {
                // Si la primera opción NO es un placeholder, la posición 0 es válida.
                // O si se seleccionó algo diferente al placeholder.
                if (categorySpinner.selectedItemPosition >= 0 && categorySpinner.selectedItem.toString() != getString(R.string.placeholder_select_category) && categorySpinner.selectedItem.toString() != getString(R.string.no_categories_available)) {
                    selectedCategoryName = categorySpinner.selectedItem.toString()
                }
            }


            val finalCategory: String
            if (newCategoryName.isNotEmpty()) {
                finalCategory = newCategoryName
            } else if (!selectedCategoryName.isNullOrEmpty()) {
                finalCategory = selectedCategoryName
            } else {
                Toast.makeText(this, "Debes seleccionar o crear una categoría.", Toast.LENGTH_LONG).show()
                return@setPositiveButton
            }

            val price = priceText.toDoubleOrNull()

            if (name.isEmpty()) {
                Toast.makeText(this, "El nombre del producto no puede estar vacío.", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (name.length > 50) {
                Toast.makeText(this, "El nombre del producto es demasiado largo (máx 50).", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (price == null) {
                Toast.makeText(this, "El precio no es válido.", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (price !in 0.0..9999.99) {
                Toast.makeText(this, "El precio debe estar entre \$0.00 y \$9999.99.", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    productDao.insert(Product(name = name, price = price, category = finalCategory))
                    // No es necesario llamar a observeCategories aquí explícitamente si se está recolectando el Flow.
                    // El cambio en la base de datos debería disparar la actualización.
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@MenuAdminActivity, "Producto '$name' añadido a '$finalCategory'", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("MenuAdmin", "Error añadiendo producto", e)
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@MenuAdminActivity, "Error al añadir producto", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showEditProductDialog(product: Product) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar Producto: ${product.name}")

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }

        val nameInput = EditText(this).apply {
            setText(product.name)
            hint = "Nombre del producto"
        }
        val priceInput = EditText(this).apply {
            setText(product.price.toString())
            hint = "Precio"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        container.addView(nameInput)
        container.addView(priceInput)

        // Sección de Categoría
        container.addView(TextView(this).apply {
            text = "Categoría Existente:"
            textSize = 16f
            setPadding(0, 16, 0, 4)
        })
        val categorySpinner = Spinner(this).apply {
            adapter = categoriesAdapter
            val currentCategoryPosition = categoriesList.indexOf(product.category)
            if (currentCategoryPosition >= 0) {
                setSelection(currentCategoryPosition)
            } else if (categoriesList.isNotEmpty() && (categoriesList.first() == getString(R.string.placeholder_select_category) || categoriesList.first() == getString(R.string.no_categories_available))) {
                setSelection(0) // Seleccionar el placeholder si la categoría actual no está en la lista
            }
        }
        container.addView(categorySpinner)

        container.addView(TextView(this).apply {
            text = "O Cambiar a Nueva Categoría:"
            textSize = 16f
            setPadding(0, 16, 0, 4)
        })
        val newCategoryInput = EditText(this).apply {
            hint = "Nombre de la nueva categoría"
        }
        container.addView(newCategoryInput)

        builder.setView(container)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val name = nameInput.text.toString().trim()
            val priceText = priceInput.text.toString()
            val newCategoryName = newCategoryInput.text.toString().trim()
            var selectedCategoryName: String? = null

            if (categorySpinner.selectedItemPosition >= 0 && categorySpinner.selectedItem.toString() != getString(R.string.placeholder_select_category) && categorySpinner.selectedItem.toString() != getString(R.string.no_categories_available)) {
                selectedCategoryName = categorySpinner.selectedItem.toString()
            }


            val finalCategory: String
            if (newCategoryName.isNotEmpty()) {
                finalCategory = newCategoryName
            } else if (!selectedCategoryName.isNullOrEmpty()) {
                finalCategory = selectedCategoryName
            } else {
                // Si ni se crea una nueva ni se selecciona una válida, mantener la original del producto
                finalCategory = product.category
                if (finalCategory.isEmpty()) { // Aunque category no debería ser empty si se valida bien al crear
                    Toast.makeText(this, "La categoría no puede estar vacía. Se mantuvo la original.", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
            }

            val price = priceText.toDoubleOrNull()

            // Validaciones (similar a showAddProductDialog)
            if (name.isEmpty() || name.length > 50 || price == null || price !in 0.0..9999.99) {
                Toast.makeText(this, "Datos inválidos (nombre, precio).", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    productDao.update(product.copy(name = name, price = price, category = finalCategory))
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@MenuAdminActivity, "Producto actualizado", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("MenuAdmin", "Error actualizando producto", e)
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@MenuAdminActivity, "Error al actualizar producto", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showDeleteConfirmationDialog(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Producto")
            .setMessage("¿Estás seguro de que quieres eliminar \"${product.name}\" de la categoría \"${product.category}\"?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        productDao.delete(product)
                        launch(Dispatchers.Main) {
                            Toast.makeText(this@MenuAdminActivity, "Producto eliminado", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("MenuAdmin", "Error eliminando producto", e)
                        launch(Dispatchers.Main) {
                            Toast.makeText(this@MenuAdminActivity, "Error al eliminar producto", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
            .show()
    }
}

