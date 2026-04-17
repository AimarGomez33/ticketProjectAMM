package com.example.ticketapp

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class CatalogEditorActivity : AppCompatActivity() {

    private data class RowBinding(
        val sourceName: String?,
        val isCustom: Boolean,
        val root: View,
        val etName: EditText,
        val etPrice: EditText,
        val spinnerCategory: Spinner
    )

    private lateinit var spinnerCategoryOrder: Spinner
    private lateinit var btnCategoryUp: Button
    private lateinit var btnCategoryDown: Button
    private lateinit var tvCategoryOrderPreview: TextView

    private lateinit var etNewProductName: EditText
    private lateinit var etNewProductPrice: EditText
    private lateinit var spinnerNewProductCategory: Spinner
    private lateinit var btnAddNewProduct: Button

    private lateinit var containerCatalogProducts: LinearLayout
    private lateinit var btnSaveCatalog: Button

    private val defaultsBySource = CatalogConfigStore.defaultProducts.associateBy { it.sourceName }
    private val categoryOrder = CatalogConfigStore.defaultCategories.toMutableList()
    private val rowBindings = mutableListOf<RowBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog_editor)

        spinnerCategoryOrder = findViewById(R.id.spinnerCategoryOrder)
        btnCategoryUp = findViewById(R.id.btnCategoryUp)
        btnCategoryDown = findViewById(R.id.btnCategoryDown)
        tvCategoryOrderPreview = findViewById(R.id.tvCategoryOrderPreview)

        etNewProductName = findViewById(R.id.etNewProductName)
        etNewProductPrice = findViewById(R.id.etNewProductPrice)
        spinnerNewProductCategory = findViewById(R.id.spinnerNewProductCategory)
        btnAddNewProduct = findViewById(R.id.btnAddNewProduct)

        containerCatalogProducts = findViewById(R.id.containerCatalogProducts)
        btnSaveCatalog = findViewById(R.id.btnSaveCatalog)

        val config = CatalogConfigStore.load(this)

        categoryOrder.clear()
        categoryOrder.addAll(config.categoryOrder)

        setupCategoryOrderControls()
        setupNewProductControls()
        renderRows(config)

        btnSaveCatalog.setOnClickListener {
            saveChanges()
        }
    }

    private fun setupCategoryOrderControls() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryOrder)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoryOrder.adapter = adapter

        val categoryAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, CatalogConfigStore.defaultCategories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNewProductCategory.adapter = categoryAdapter

        btnCategoryUp.setOnClickListener {
            val idx = spinnerCategoryOrder.selectedItemPosition
            if (idx > 0) {
                val item = categoryOrder.removeAt(idx)
                categoryOrder.add(idx - 1, item)
                adapter.notifyDataSetChanged()
                spinnerCategoryOrder.setSelection(idx - 1)
                updateOrderPreview()
            }
        }

        btnCategoryDown.setOnClickListener {
            val idx = spinnerCategoryOrder.selectedItemPosition
            if (idx >= 0 && idx < categoryOrder.lastIndex) {
                val item = categoryOrder.removeAt(idx)
                categoryOrder.add(idx + 1, item)
                adapter.notifyDataSetChanged()
                spinnerCategoryOrder.setSelection(idx + 1)
                updateOrderPreview()
            }
        }

        updateOrderPreview()
    }

    private fun setupNewProductControls() {
        btnAddNewProduct.setOnClickListener {
            val name = etNewProductName.text.toString().trim()
            val price = etNewProductPrice.text.toString().toDoubleOrNull()
            val category = spinnerNewProductCategory.selectedItem?.toString().orEmpty()

            if (name.isBlank() || price == null || price <= 0.0 || category.isBlank()) {
                Toast.makeText(this, "Nombre, precio y categoría son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isDuplicatedName(name, null)) {
                Toast.makeText(this, "Ya existe un producto con ese nombre", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addRow(sourceName = null, isCustom = true, name = name, price = price, category = category)
            etNewProductName.setText("")
            etNewProductPrice.setText("")
        }
    }

    private fun renderRows(config: CatalogConfig) {
        containerCatalogProducts.removeAllViews()
        rowBindings.clear()

        val defaults = CatalogConfigStore.defaultProducts

        defaults.forEach { base ->
            val override = config.productOverrides[base.sourceName]
            val resolvedName = override?.name ?: base.name
            val price = override?.price ?: base.price
            val category = override?.category ?: base.category
            addRow(base.sourceName, false, resolvedName, price, category)
        }

        config.customProducts.forEach { custom ->
            addRow(null, true, custom.name, custom.price, custom.category)
        }
    }

    private fun addRow(
        sourceName: String?,
        isCustom: Boolean,
        name: String,
        price: Double,
        category: String
    ) {
        val rowView = layoutInflater.inflate(R.layout.item_catalog_editor_product, containerCatalogProducts, false)

        val tvSource = rowView.findViewById<TextView>(R.id.tvSourceName)
        val etName = rowView.findViewById<EditText>(R.id.etProductName)
        val etPrice = rowView.findViewById<EditText>(R.id.etProductPrice)
        val spinnerCategory = rowView.findViewById<Spinner>(R.id.spinnerProductCategory)
        val btnDelete = rowView.findViewById<Button>(R.id.btnDeleteProduct)

        tvSource.text =
            when {
                sourceName == null -> "Producto nuevo"
                else -> "Base: $sourceName"
            }

        etName.setText(name)
        etName.isEnabled = true
        etName.isFocusable = true
        etName.isFocusableInTouchMode = true
        etName.alpha = 1f

        etPrice.setText(String.format(Locale.US, "%.2f", price))

        val categoryAdapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, CatalogConfigStore.defaultCategories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter
        val selectedIndex = CatalogConfigStore.defaultCategories.indexOf(category).coerceAtLeast(0)
        spinnerCategory.setSelection(selectedIndex)

        if (isCustom) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                containerCatalogProducts.removeView(rowView)
                rowBindings.removeAll { it.root == rowView }
            }
        }

        rowBindings.add(
            RowBinding(
                sourceName = sourceName,
                isCustom = isCustom,
                root = rowView,
                etName = etName,
                etPrice = etPrice,
                spinnerCategory = spinnerCategory
            )
        )

        containerCatalogProducts.addView(rowView)
    }

    private fun saveChanges() {
        val overrides = mutableMapOf<String, CatalogProductConfig>()
        val custom = mutableListOf<CatalogProductConfig>()
        val finalNames = mutableSetOf<String>()

        for (row in rowBindings) {
            val sourceName = row.sourceName
            val name = row.etName.text.toString().trim()
            val price = row.etPrice.text.toString().toDoubleOrNull()
            val category = row.spinnerCategory.selectedItem?.toString().orEmpty()

            if (name.isBlank() || price == null || price <= 0.0 || category.isBlank()) {
                Toast.makeText(this, "Revisa campos vacíos o precios inválidos", Toast.LENGTH_SHORT).show()
                return
            }

            val normalized = name.lowercase(Locale.getDefault())
            if (!finalNames.add(normalized)) {
                Toast.makeText(this, "Hay nombres de productos duplicados", Toast.LENGTH_SHORT).show()
                return
            }

            if (row.isCustom) {
                custom.add(
                    CatalogProductConfig(
                        sourceName = null,
                        name = name,
                        price = price,
                        category = category,
                        isCustom = true
                    )
                )
            } else {
                val source = sourceName ?: continue
                val baseProduct = defaultsBySource[source] ?: continue

                val changed =
                    name != baseProduct.name ||
                        kotlin.math.abs(price - baseProduct.price) > 0.0001 ||
                        category != baseProduct.category

                if (changed) {
                    overrides[source] =
                        CatalogProductConfig(
                            sourceName = source,
                            name = name,
                            price = price,
                            category = category,
                            isCustom = false
                        )
                }
            }
        }

        val finalOrder =
            categoryOrder.filter { CatalogConfigStore.defaultCategories.contains(it) }
                .toMutableList()
                .apply {
                    CatalogConfigStore.defaultCategories.forEach {
                        if (!contains(it)) add(it)
                    }
                }

        CatalogConfigStore.save(
            this,
            CatalogConfig(
                categoryOrder = finalOrder,
                productOverrides = overrides,
                customProducts = custom
            )
        )

        Toast.makeText(this, "Catálogo actualizado", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun isDuplicatedName(name: String, excludeRow: View?): Boolean {
        val normalized = name.trim().lowercase(Locale.getDefault())
        if (normalized.isBlank()) return false

        return rowBindings.any { row ->
            if (excludeRow != null && row.root == excludeRow) return@any false
            val rowName = row.etName.text.toString().trim()
            rowName.lowercase(Locale.getDefault()) == normalized
        }
    }

    private fun updateOrderPreview() {
        tvCategoryOrderPreview.text = categoryOrder.joinToString("  >  ")
    }
}
