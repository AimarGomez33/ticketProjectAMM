package com.example.ticketapp.data.menu

// Represents an option for a product that requires a variation (e.g., Guisado)
data class VariationOption(val name: String, val isAvailable: Boolean = true)

// Represents the type of variation required
enum class VariationType {
    NONE, // Regular products (e.g., Tostadas, Pozole)
    SINGLE_SELECTION, // Requires exactly one choice (e.g., Quesadillas)
    MULTIPLE_SELECTION, // Pambazos Combinados (requires 2+ choices)
    TEXT_INPUT // Refrescos, where the waiter types the name
}

// Represents a single menu item as shown in the UI
data class MenuItem(
        val name: String,
        val price: Double,
        val variationType: VariationType = VariationType.NONE,
        val variations: List<String> = emptyList(), // e.g., ["Chorizo", "Huevo", ...]
        val isBurger: Boolean = false // If true, requires special Normal/Combo UI
)

// Represents a category of menu items (e.g., "Platillos", "Bebidas")
data class MenuCategory(val name: String, val items: List<MenuItem>)
