package com.example.ticketapp.ui.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ticketapp.R
import com.example.ticketapp.data.menu.MenuCategory
import com.example.ticketapp.data.menu.MenuItem

@Composable
fun MenuScreen(
        categories: List<MenuCategory>,
        quantities: Map<String, Int>,
        onAddProduct: (MenuItem) -> Unit,
        onRemoveProduct: (MenuItem) -> Unit,
        onAddBurgerNormal: (MenuItem) -> Unit,
        onRemoveBurgerNormal: (MenuItem) -> Unit,
        onAddBurgerCombo: (MenuItem) -> Unit,
        onRemoveBurgerCombo: (MenuItem) -> Unit,
        onOpenComment: (String) -> Unit,
        notasExtras: String = "",
        onNotasChanged: (String) -> Unit = {},
        onTerminarPedido: (() -> Unit)? = null,
        existingOrderDetails: String? = null
) {
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                // ── Existing Order Details ──────────────────────────────────────────
                if (existingOrderDetails != null) {
                        item {
                                androidx.compose.material3.Card(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(
                                                                horizontal = 4.dp,
                                                                vertical = 4.dp
                                                        ),
                                        colors =
                                                androidx.compose.material3.CardDefaults.cardColors(
                                                        containerColor =
                                                                colorResource(
                                                                        id = R.color.app_surface
                                                                )
                                                ),
                                        elevation =
                                                androidx.compose.material3.CardDefaults
                                                        .cardElevation(defaultElevation = 2.dp)
                                ) {
                                        androidx.compose.foundation.layout.Column(
                                                modifier = Modifier.padding(16.dp)
                                        ) {
                                                androidx.compose.material3.Text(
                                                        "Pedido Existente",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color =
                                                                colorResource(
                                                                        id = R.color.app_primary
                                                                )
                                                )
                                                androidx.compose.material3.Text(
                                                        existingOrderDetails,
                                                        fontSize = 14.sp,
                                                        modifier = Modifier.padding(top = 8.dp)
                                                )
                                        }
                                }
                        }
                }

                items(categories) { category ->
                        CategoryCard(
                                category = category,
                                quantities = quantities,
                                onAddProduct = onAddProduct,
                                onRemoveProduct = onRemoveProduct,
                                onAddBurgerNormal = onAddBurgerNormal,
                                onRemoveBurgerNormal = onRemoveBurgerNormal,
                                onAddBurgerCombo = onAddBurgerCombo,
                                onRemoveBurgerCombo = onRemoveBurgerCombo,
                                onOpenComment = onOpenComment
                        )
                }

                // ── Notas Extras ────────────────────────────────────────────────────
                item {
                        androidx.compose.material3.OutlinedTextField(
                                value = notasExtras,
                                onValueChange = onNotasChanged,
                                label = { androidx.compose.material3.Text("Notas Extras") },
                                placeholder = {
                                        androidx.compose.material3.Text(
                                                "Ej: sin salsa, extra servilletas…",
                                                fontSize = 13.sp
                                        )
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp)
                        )
                }

                // ── Terminar Pedido ─────────────────────────────────────────────────
                if (onTerminarPedido != null) {
                        item {
                                androidx.compose.material3.Button(
                                        onClick = onTerminarPedido,
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(
                                                                horizontal = 4.dp,
                                                                vertical = 8.dp
                                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        colors =
                                                androidx.compose.material3.ButtonDefaults
                                                        .buttonColors(
                                                                containerColor =
                                                                        colorResource(
                                                                                id =
                                                                                        R.color
                                                                                                .app_primary
                                                                        )
                                                        )
                                ) {
                                        androidx.compose.material3.Text(
                                                "✅  Terminar Pedido",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                }
                        }
                }
        }
}

@Composable
fun CategoryCard(
        category: MenuCategory,
        quantities: Map<String, Int>,
        onAddProduct: (MenuItem) -> Unit,
        onRemoveProduct: (MenuItem) -> Unit,
        onAddBurgerNormal: (MenuItem) -> Unit,
        onRemoveBurgerNormal: (MenuItem) -> Unit,
        onAddBurgerCombo: (MenuItem) -> Unit,
        onRemoveBurgerCombo: (MenuItem) -> Unit,
        onOpenComment: (String) -> Unit
) {
        var expanded by remember { mutableStateOf(false) }

        Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        CardDefaults.cardColors(
                                containerColor = colorResource(id = R.color.BrigthSnow)
                        ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
        ) {
                Column {
                        // Header
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .clickable { expanded = !expanded }
                                                .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = category.name,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorResource(id = R.color.text_primary)
                                )
                                Icon(
                                        imageVector =
                                                if (expanded) Icons.Default.KeyboardArrowUp
                                                else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (expanded) "Collapse" else "Expand",
                                        tint = colorResource(id = R.color.text_primary)
                                )
                        }

                        // Expanded Content (Products Grid)
                        AnimatedVisibility(
                                visible = expanded,
                                enter = expandVertically(animationSpec = tween(300)),
                                exit = shrinkVertically(animationSpec = tween(300))
                        ) {
                                // We use a fixed height for the grid or let it calculate based on
                                // rows.
                                // In Compose, nesting LazyVerticalGrid inside LazyColumn is not
                                // allowed with
                                // unbounded height.
                                // It is better to use a FlowRow or calculate rows manually, but
                                // since we know items
                                // list isn't infinite,
                                // we can use a custom grid implementation or a Column of Rows.
                                ProductGrid(
                                        items = category.items,
                                        quantities = quantities,
                                        onAddProduct = onAddProduct,
                                        onRemoveProduct = onRemoveProduct,
                                        onAddBurgerNormal = onAddBurgerNormal,
                                        onRemoveBurgerNormal = onRemoveBurgerNormal,
                                        onAddBurgerCombo = onAddBurgerCombo,
                                        onRemoveBurgerCombo = onRemoveBurgerCombo,
                                        onOpenComment = onOpenComment
                                )
                        }
                }
        }
}

// Simple Grid layout using Column and Row to avoid nested Lazy measuring issues
@Composable
fun ProductGrid(
        items: List<MenuItem>,
        quantities: Map<String, Int>,
        columns: Int = 3,
        onAddProduct: (MenuItem) -> Unit,
        onRemoveProduct: (MenuItem) -> Unit,
        onAddBurgerNormal: (MenuItem) -> Unit,
        onRemoveBurgerNormal: (MenuItem) -> Unit,
        onAddBurgerCombo: (MenuItem) -> Unit,
        onRemoveBurgerCombo: (MenuItem) -> Unit,
        onOpenComment: (String) -> Unit
) {
        val chunkedItems = items.chunked(columns)
        Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                chunkedItems.forEach { rowItems ->
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                for (item in rowItems) {
                                        Box(modifier = Modifier.weight(1f)) {
                                                ProductCard(
                                                        item = item,
                                                        quantity = quantities[item.name] ?: 0,
                                                        quantityCombo =
                                                                if (item.isBurger)
                                                                        quantities[
                                                                                "${item.name}_Combo"]
                                                                                ?: 0
                                                                else 0,
                                                        onAddProduct = { onAddProduct(item) },
                                                        onRemoveProduct = { onRemoveProduct(item) },
                                                        onAddBurgerNormal = {
                                                                onAddBurgerNormal(item)
                                                        },
                                                        onRemoveBurgerNormal = {
                                                                onRemoveBurgerNormal(item)
                                                        },
                                                        onAddBurgerCombo = {
                                                                onAddBurgerCombo(item)
                                                        },
                                                        onRemoveBurgerCombo = {
                                                                onRemoveBurgerCombo(item)
                                                        },
                                                        onOpenComment = { onOpenComment(item.name) }
                                                )
                                        }
                                }
                                // Fill remaining empty columns if row is not full
                                repeat(columns - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                }
                        }
                }
        }
}

@Composable
fun ProductCard(
        item: MenuItem,
        quantity: Int,
        quantityCombo: Int = 0,
        onAddProduct: () -> Unit,
        onRemoveProduct: () -> Unit,
        onAddBurgerNormal: () -> Unit,
        onRemoveBurgerNormal: () -> Unit,
        onAddBurgerCombo: () -> Unit,
        onRemoveBurgerCombo: () -> Unit,
        onOpenComment: () -> Unit,
) {
        Card(
                modifier =
                        Modifier.fillMaxWidth().clickable {
                                if (item.isBurger) {
                                        onAddBurgerNormal()
                                } else {
                                        onAddProduct()
                                }
                        },
                colors =
                        CardDefaults.cardColors(
                                containerColor = colorResource(id = R.color.BrigthSnow)
                        ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
        ) {
                Column(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                        // Product Name
                        Text(
                                text = item.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(id = R.color.text_secondary),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()
                        )

                        if (item.isBurger) {
                                // Layout for Burgers (Normal AND Combo rows)
                                BurgerQuantityRow(
                                        label = "Sencilla",
                                        quantity = quantity,
                                        onMinusClick = onRemoveBurgerNormal,
                                        onPlusClick = onAddBurgerNormal
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                BurgerQuantityRow(
                                        label = "Combo",
                                        quantity = quantityCombo,
                                        onMinusClick = onRemoveBurgerCombo,
                                        onPlusClick = onAddBurgerCombo
                                )

                                // Comment Button for Burgers
                                IconButton(
                                        onClick = onOpenComment,
                                        modifier = Modifier.align(Alignment.Start)
                                ) {
                                        Icon(
                                                painter =
                                                        painterResource(
                                                                id = R.drawable.ic_comment
                                                        ), // Ensure this exists
                                                contentDescription = "Comentario",
                                                tint = colorResource(id = R.color.Grey)
                                        )
                                }
                        } else {
                                // Layout for Standard Products
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        IconButton(onClick = onOpenComment) {
                                                Icon(
                                                        painter =
                                                                painterResource(
                                                                        id = R.drawable.ic_comment
                                                                ),
                                                        contentDescription = "Comentario",
                                                        tint = colorResource(id = R.color.Grey)
                                                )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Minus Button
                                                FilledIconButton(
                                                        onClick = onRemoveProduct,
                                                        modifier = Modifier.size(40.dp),
                                                        colors =
                                                                IconButtonDefaults
                                                                        .filledIconButtonColors(
                                                                                containerColor =
                                                                                        colorResource(
                                                                                                id =
                                                                                                        R.color
                                                                                                                .Grey
                                                                                        ),
                                                                                contentColor =
                                                                                        colorResource(
                                                                                                id =
                                                                                                        R.color
                                                                                                                .white
                                                                                        )
                                                                        ),
                                                        shape = RoundedCornerShape(20.dp)
                                                ) {
                                                        Text(
                                                                "-",
                                                                fontSize = 18.sp,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }

                                                // Quantity
                                                Text(
                                                        text = quantity.toString(),
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color =
                                                                colorResource(
                                                                        id = R.color.text_primary
                                                                ),
                                                        modifier =
                                                                Modifier.padding(horizontal = 8.dp)
                                                                        .defaultMinSize(
                                                                                minWidth = 24.dp
                                                                        ),
                                                        textAlign = TextAlign.Center
                                                )

                                                // Plus Button
                                                FilledIconButton(
                                                        onClick = onAddProduct,
                                                        modifier = Modifier.size(40.dp),
                                                        colors =
                                                                IconButtonDefaults
                                                                        .filledIconButtonColors(
                                                                                containerColor =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primary,
                                                                                contentColor =
                                                                                        colorResource(
                                                                                                id =
                                                                                                        R.color
                                                                                                                .white
                                                                                        )
                                                                        ),
                                                        shape = RoundedCornerShape(20.dp)
                                                ) {
                                                        Text(
                                                                "+",
                                                                fontSize = 18.sp,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
fun BurgerQuantityRow(
        label: String,
        quantity: Int,
        onMinusClick: () -> Unit,
        onPlusClick: () -> Unit
) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(
                                onClick = onMinusClick,
                                modifier = Modifier.size(32.dp),
                                colors =
                                        IconButtonDefaults.filledIconButtonColors(
                                                containerColor = colorResource(id = R.color.Grey),
                                                contentColor = colorResource(id = R.color.white)
                                        ),
                                shape = RoundedCornerShape(16.dp)
                        ) { Text("-", fontSize = 14.sp) }
                        Text(
                                text = quantity.toString(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier =
                                        Modifier.padding(horizontal = 6.dp)
                                                .defaultMinSize(minWidth = 18.dp),
                                textAlign = TextAlign.Center
                        )
                        FilledIconButton(
                                onClick = onPlusClick,
                                modifier = Modifier.size(32.dp),
                                colors =
                                        IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = colorResource(id = R.color.white)
                                        ),
                                shape = RoundedCornerShape(16.dp)
                        ) { Text("+", fontSize = 14.sp) }
                }
        }
}
