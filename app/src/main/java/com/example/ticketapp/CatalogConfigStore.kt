package com.example.ticketapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CatalogProductConfig(
    val sourceName: String?,
    val name: String,
    val price: Double,
    val category: String,
    val isCustom: Boolean
)

data class CatalogConfig(
    val categoryOrder: MutableList<String>,
    val productOverrides: MutableMap<String, CatalogProductConfig>,
    val customProducts: MutableList<CatalogProductConfig>
)

object CatalogConfigStore {
    private const val PREFS_NAME = "catalog_config"
    private const val KEY_JSON = "catalog_json"

    val defaultCategories: List<String> =
        listOf(
            "Platillos",
            "Entradas",
            "Pambazos",
            "Guajoloyets",
            "Papas",
            "Tacos",
            "Alitas",
            "Bebidas",
            "Postres",
            "Hamburguesas"
        )

    val defaultProducts: List<CatalogProductConfig> =
        listOf(
            CatalogProductConfig("Quesadillas", "Quesadillas", 30.0, "Platillos", false),
            CatalogProductConfig("Tostadas", "Tostadas", 35.0, "Platillos", false),
            CatalogProductConfig("Chalupas", "Chalupas", 5.0, "Platillos", false),
            CatalogProductConfig("Volcanes", "Volcanes", 60.0, "Platillos", false),
            CatalogProductConfig("Volcan Queso/Guisado Extra", "Volcan Queso/Guisado Extra", 72.0, "Platillos", false),
            CatalogProductConfig("Pozole Grande", "Pozole Grande", 110.0, "Platillos", false),
            CatalogProductConfig("Pozole Chico", "Pozole Chico", 90.0, "Platillos", false),

            CatalogProductConfig("Alones", "Alones", 25.0, "Entradas", false),
            CatalogProductConfig("Mollejas", "Mollejas", 25.0, "Entradas", false),
            CatalogProductConfig("Higados", "Higados", 22.0, "Entradas", false),
            CatalogProductConfig("Patitas", "Patitas", 22.0, "Entradas", false),
            CatalogProductConfig("Huevos", "Huevos", 20.0, "Entradas", false),

            CatalogProductConfig("Pambazos Naturales", "Pambazos Naturales", 35.0, "Pambazos", false),
            CatalogProductConfig("Pambazos Naturales Combinados", "Pambazos Naturales Combinados", 42.0, "Pambazos", false),
            CatalogProductConfig("Pambazos Naturales Combinados con Queso", "Pambazos Naturales Combinados con Queso", 54.0, "Pambazos", false),
            CatalogProductConfig("Pambazos Naturales Extra", "Pambazos Naturales Extra", 47.0, "Pambazos", false),
            CatalogProductConfig("Pambazos Adobados", "Pambazos Adobados", 40.0, "Pambazos", false),
            CatalogProductConfig("Pambazos Adobados Combinados", "Pambazos Adobados Combinados", 47.0, "Pambazos", false),
            CatalogProductConfig("Pambazos Adobados Combinados con Queso", "Pambazos Adobados Combinados con Queso", 59.0, "Pambazos", false),
            CatalogProductConfig("Pambazos Adobados Extra", "Pambazos Adobados Extra", 52.0, "Pambazos", false),

            CatalogProductConfig("Guajoloyets Naturales", "Guajoloyets Naturales", 60.0, "Guajoloyets", false),
            CatalogProductConfig("Guajoloyets Naturales Extra", "Guajoloyets Naturales Extra", 72.0, "Guajoloyets", false),
            CatalogProductConfig("Guajoloyets Adobados", "Guajoloyets Adobados", 65.0, "Guajoloyets", false),
            CatalogProductConfig("Guajoloyets Adobados Extra", "Guajoloyets Adobados Extra", 77.0, "Guajoloyets", false),

            CatalogProductConfig("Orden de Papas Sencillas", "Orden de Papas Sencillas", 50.0, "Papas", false),
            CatalogProductConfig("Orden de Papas Queso y Tocino", "Orden de Papas Queso y Tocino", 65.0, "Papas", false),

            CatalogProductConfig("Taco (c/u)", "Taco (c/u)", 25.0, "Tacos", false),
            CatalogProductConfig("Taco con Queso (c/u)", "Taco con Queso (c/u)", 30.0, "Tacos", false),

            CatalogProductConfig("Alitas 6 pzas", "Alitas 6 pzas", 65.0, "Alitas", false),
            CatalogProductConfig("Alitas 10 pzas", "Alitas 10 pzas", 100.0, "Alitas", false),
            CatalogProductConfig("Alitas 15 pzas", "Alitas 15 pzas", 140.0, "Alitas", false),
            CatalogProductConfig("Combo", "Combo", 30.0, "Alitas", false),

            CatalogProductConfig("Refrescos", "Refrescos", 26.0, "Bebidas", false),
            CatalogProductConfig("Cafe", "Cafe", 22.0, "Bebidas", false),
            CatalogProductConfig("Aguas de Sabor", "Aguas de Sabor", 25.0, "Bebidas", false),
            CatalogProductConfig("Agua Natural", "Agua Natural", 20.0, "Bebidas", false),
            CatalogProductConfig("Agua para Te", "Agua para Te", 20.0, "Bebidas", false),

            CatalogProductConfig("Hamburguesa Clasica", "Hamburguesa Clasica", 65.0, "Hamburguesas", false),
            CatalogProductConfig("Hamburguesa Hawaiana", "Hamburguesa Hawaiana", 80.0, "Hamburguesas", false),
            CatalogProductConfig("Hamburguesa Pollo", "Hamburguesa Pollo", 70.0, "Hamburguesas", false),
            CatalogProductConfig("Hamburguesa Champinones", "Hamburguesa Champinones", 90.0, "Hamburguesas", false),
            CatalogProductConfig("Hamburguesa Arrachera", "Hamburguesa Arrachera", 105.0, "Hamburguesas", false),
            CatalogProductConfig("Hamburguesa Maggy", "Hamburguesa Maggy", 100.0, "Hamburguesas", false),
            CatalogProductConfig("Hamburguesa Doble", "Hamburguesa Doble", 110.0, "Hamburguesas", false)
        )

    fun buildResolvedProducts(config: CatalogConfig): List<CatalogProductConfig> {
        val resolvedDefaults =
            defaultProducts.map { base ->
                val override = config.productOverrides[base.sourceName]
                CatalogProductConfig(
                    sourceName = base.sourceName,
                    name = override?.name?.trim().orEmpty().ifBlank { base.name },
                    price = override?.price?.takeIf { it > 0.0 } ?: base.price,
                    category = override?.category?.trim().orEmpty().ifBlank { base.category },
                    isCustom = false
                )
            }

        val resolvedCustoms =
            config.customProducts.mapNotNull { custom ->
                val name = custom.name.trim()
                val category = custom.category.trim()
                val price = custom.price

                if (name.isBlank() || category.isBlank() || price <= 0.0) {
                    null
                } else {
                    custom.copy(name = name, category = category, price = price, isCustom = true)
                }
            }

        return resolvedDefaults + resolvedCustoms
    }

    fun buildAvailableCategories(config: CatalogConfig): List<String> {
        val categories =
            linkedSetOf<String>().apply {
                addAll(defaultCategories)
                addAll(config.categoryOrder.map { it.trim() }.filter { it.isNotBlank() })
                addAll(buildResolvedProducts(config).map { it.category.trim() }.filter { it.isNotBlank() })
            }

        return buildList {
            addAll(config.categoryOrder.filter { categories.contains(it) })
            addAll(categories.filterNot { contains(it) }.sorted())
        }
    }

    fun isHamburgerSource(sourceName: String?): Boolean {
        if (sourceName.isNullOrBlank()) return false
        return defaultProducts.firstOrNull { it.sourceName == sourceName }
            ?.category
            .equals("Hamburguesas", ignoreCase = true)
    }

    fun load(context: Context): CatalogConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_JSON, null)

        if (json.isNullOrBlank()) {
            return CatalogConfig(
                categoryOrder = defaultCategories.toMutableList(),
                productOverrides = mutableMapOf(),
                customProducts = mutableListOf()
            )
        }

        return try {
            val root = JSONObject(json)
            val orderArray = root.optJSONArray("categoryOrder") ?: JSONArray()
            val categoryOrder = mutableListOf<String>()
            for (i in 0 until orderArray.length()) {
                val v = orderArray.optString(i)
                if (v.isNotBlank()) categoryOrder.add(v)
            }
            if (categoryOrder.isEmpty()) {
                categoryOrder.addAll(defaultCategories)
            }

            val overridesObj = root.optJSONObject("productOverrides") ?: JSONObject()
            val productOverrides = mutableMapOf<String, CatalogProductConfig>()
            val it = overridesObj.keys()
            while (it.hasNext()) {
                val key = it.next()
                val obj = overridesObj.optJSONObject(key) ?: continue
                val name = obj.optString("name", key)
                val price = obj.optDouble("price", 0.0)
                val category = obj.optString("category", "Otros")
                productOverrides[key] =
                    CatalogProductConfig(
                        sourceName = key,
                        name = name,
                        price = price,
                        category = category,
                        isCustom = false
                    )
            }

            val customArray = root.optJSONArray("customProducts") ?: JSONArray()
            val customProducts = mutableListOf<CatalogProductConfig>()
            for (i in 0 until customArray.length()) {
                val obj = customArray.optJSONObject(i) ?: continue
                customProducts.add(
                    CatalogProductConfig(
                        sourceName = null,
                        name = obj.optString("name", ""),
                        price = obj.optDouble("price", 0.0),
                        category = obj.optString("category", "Otros"),
                        isCustom = true
                    )
                )
            }

            CatalogConfig(
                categoryOrder = categoryOrder,
                productOverrides = productOverrides,
                customProducts = customProducts
            )
        } catch (_: Exception) {
            CatalogConfig(
                categoryOrder = defaultCategories.toMutableList(),
                productOverrides = mutableMapOf(),
                customProducts = mutableListOf()
            )
        }
    }

    fun save(context: Context, config: CatalogConfig) {
        val root = JSONObject()

        val orderArray = JSONArray()
        config.categoryOrder.forEach { orderArray.put(it) }
        root.put("categoryOrder", orderArray)

        val overridesObj = JSONObject()
        config.productOverrides.forEach { (source, product) ->
            val obj = JSONObject()
            obj.put("name", product.name)
            obj.put("price", product.price)
            obj.put("category", product.category)
            overridesObj.put(source, obj)
        }
        root.put("productOverrides", overridesObj)

        val customArray = JSONArray()
        config.customProducts.forEach { p ->
            val obj = JSONObject()
            obj.put("name", p.name)
            obj.put("price", p.price)
            obj.put("category", p.category)
            customArray.put(obj)
        }
        root.put("customProducts", customArray)

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_JSON, root.toString())
            .apply()
    }
}
