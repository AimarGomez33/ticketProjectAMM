package com.example.ticketapp.ui.menu

import androidx.lifecycle.ViewModel
import com.example.ticketapp.data.menu.MenuCategory
import com.example.ticketapp.data.menu.MenuItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MenuState(
        val categories: List<MenuCategory> = emptyList(),
        val quantities: Map<String, Int> = emptyMap(),
        val notasExtras: String = "",
        val existingOrderDetails: String? = null
)

class MenuViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MenuState())
    val uiState: StateFlow<MenuState> = _uiState.asStateFlow()

    fun loadMenu(categories: List<MenuCategory>) {
        val initialQuantities = mutableMapOf<String, Int>()
        categories.forEach { category ->
            category.items.forEach { item ->
                initialQuantities[item.name] = 0
                if (item.isBurger) {
                    initialQuantities["${item.name}_Combo"] = 0
                }
            }
        }

        _uiState.update { it.copy(categories = categories, quantities = initialQuantities) }
    }

    fun addProduct(item: MenuItem) {
        _uiState.update { state ->
            val currentQty = state.quantities[item.name] ?: 0
            val newQuantities = state.quantities.toMutableMap()
            newQuantities[item.name] = currentQty + 1
            state.copy(quantities = newQuantities)
        }
    }

    fun removeProduct(item: MenuItem) {
        _uiState.update { state ->
            val currentQty = state.quantities[item.name] ?: 0
            if (currentQty > 0) {
                val newQuantities = state.quantities.toMutableMap()
                newQuantities[item.name] = currentQty - 1
                state.copy(quantities = newQuantities)
            } else {
                state
            }
        }
    }

    fun syncQuantity(productName: String, quantity: Int) {
        _uiState.update { state ->
            val newQuantities = state.quantities.toMutableMap()
            newQuantities[productName] = quantity
            state.copy(quantities = newQuantities)
        }
    }

    fun addBurgerNormal(item: MenuItem) {
        addProduct(item)
    }

    fun removeBurgerNormal(item: MenuItem) {
        removeProduct(item)
    }

    fun addBurgerCombo(item: MenuItem) {
        val comboKey = "${item.name}_Combo"
        _uiState.update { state ->
            val currentQty = state.quantities[comboKey] ?: 0
            val newQuantities = state.quantities.toMutableMap()
            newQuantities[comboKey] = currentQty + 1
            state.copy(quantities = newQuantities)
        }
    }

    fun removeBurgerCombo(item: MenuItem) {
        val comboKey = "${item.name}_Combo"
        _uiState.update { state ->
            val currentQty = state.quantities[comboKey] ?: 0
            if (currentQty > 0) {
                val newQuantities = state.quantities.toMutableMap()
                newQuantities[comboKey] = currentQty - 1
                state.copy(quantities = newQuantities)
            } else {
                state
            }
        }
    }

    fun updateNotas(text: String) {
        _uiState.update { it.copy(notasExtras = text) }
    }

    fun setExistingOrderDetails(details: String?) {
        _uiState.update { it.copy(existingOrderDetails = details) }
    }

    fun clearAll() {
        _uiState.update { state ->
            val resetQuantities = state.quantities.mapValues { 0 }
            state.copy(quantities = resetQuantities, notasExtras = "", existingOrderDetails = null)
        }
    }
}
