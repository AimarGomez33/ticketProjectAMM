package com.example.ticketapp.ui.menu

// no Brush import — flat design only
import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ticketapp.InventoryRefrescoAdapter
import com.example.ticketapp.MainActivity
import com.example.ticketapp.R
import com.example.ticketapp.data.menu.MenuCategory
import com.example.ticketapp.data.menu.MenuItem
import com.example.ticketapp.data.menu.VariationType
import com.example.ticketapp.ui.cart.ShoppingCartDialog
import kotlinx.coroutines.launch

// ── App flat-design palette (matches main activity) ────────────────────────
// CarbonBlack #222823 | CharcoalCool #575A5E | Grayish_lime #A7C1A8 | BrigthSnow #F4F7F5
private val appCarbonBlack = Color(0xFF222823)
private val appCharcoalCool = Color(0xFF575A5E)
private val appBrightSnow = Color(0xFFF4F7F5)
private val black = Color(0xFF000000)
private val lilacAsh = Color(0xFFA7A2A9)
private val brightSnowAlt = Color(0xFFF4F7F5)
private val charcoalCoolAlt = Color(0xFF575A5E)
private val carbonBlackAlt = Color(0xFF222823)

// Example definition if missing:
val categoryPalette =
        listOf(
                appCarbonBlack,
                appCharcoalCool,
                appCarbonBlack,
                appBrightSnow,
                black,
                carbonBlackAlt,
                brightSnowAlt,
                lilacAsh
        )

// ── Data class for pending variation request ─────────────────────────────────
data class VariationRequest(
        val item: MenuItem,
        val isMultiSelect: Boolean,
        val minSelections: Int = 1
)

// ─────────────────────────────────────────────────────────────────────────────
// MenuScreen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
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
        onSaveQuickComment: (String, String) -> Boolean = { _, _ -> false },
        notasExtras: String = "",
        onNotasChanged: (String) -> Unit = {},
        onTerminarPedido: (() -> Unit)? = null,
        existingOrderDetails: String? = null,
        // Variation dialog state (managed by parent)
        pendingVariation: VariationRequest? = null,
        onVariationSelected: (MenuItem, List<String>) -> Unit = { _, _ -> },
        onVariationDismissed: () -> Unit = {},
        // Shopping cart
        cartItems: List<MainActivity.Producto> = emptyList(),
        onCheckout: () -> Unit = {},
        onRemoveCartItem: (MainActivity.Producto) -> Unit = {},
        // Inventory long-click on + button
        onLongClickAddProduct: ((MenuItem) -> Unit)? = null,
) {
        var showCartDialog by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val showMessage: (String) -> Unit = { message ->
                scope.launch {
                        snackbarHostState.showSnackbar(
                                message = message,
                                withDismissAction = true,
                                duration = SnackbarDuration.Short
                        )
                }
        }

        Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                        TopAppBar(
                                title = { Text("Menú") },
                                colors =
                                        TopAppBarDefaults.topAppBarColors(
                                                containerColor = appCarbonBlack,
                                                titleContentColor = Color.White
                                        )
                        )
                }
        ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        // ── VISTA PRINCIPAL: DataTable de categorías expandibles ──────────────
                        LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                                // ── Pedido existente (si aplica) ─────────────────────────────────
                                if (existingOrderDetails != null) {
                                        item {
                                                Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors =
                                                                CardDefaults.cardColors(
                                                                        containerColor =
                                                                                appBrightSnow
                                                                ),
                                                        elevation =
                                                                CardDefaults.cardElevation(
                                                                        defaultElevation = 2.dp
                                                                )
                                                ) {
                                                        Column(modifier = Modifier.padding(16.dp)) {
                                                                Text(
                                                                        "Pedido Existente",
                                                                        fontSize = 16.sp,
                                                                        fontWeight =
                                                                                FontWeight.Bold,
                                                                        color = appCarbonBlack
                                                                )
                                                                Text(
                                                                        existingOrderDetails,
                                                                        fontSize = 14.sp,
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        top = 8.dp
                                                                                )
                                                                )
                                                        }
                                                }
                                        }
                                }

                                // ── Título ───────────────────────────────────────────────────────
                                item {
                                        Text(
                                                text = "Menú",
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = appCarbonBlack,
                                                modifier =
                                                        Modifier.padding(
                                                                horizontal = 4.dp,
                                                                vertical = 4.dp
                                                        )
                                        )
                                }

                                // ── Cards de categorías expandibles ─────────────────────────────
                                item {
                                        CategoryFullScreenGrid(
                                                categories = categories,
                                                quantities = quantities,
                                                onAddProduct = onAddProduct,
                                                onRemoveProduct = onRemoveProduct,
                                                onAddBurgerNormal = onAddBurgerNormal,
                                                onRemoveBurgerNormal = onRemoveBurgerNormal,
                                                onAddBurgerCombo = onAddBurgerCombo,
                                                onRemoveBurgerCombo = onRemoveBurgerCombo,
                                                onOpenComment = onOpenComment,
                                                onLongClickAddProduct = onLongClickAddProduct
                                        )
                                }

                                // ── Notas extras ─────────────────────────────────────────────────
                                item {
                                        OutlinedTextField(
                                                value = notasExtras,
                                                onValueChange = onNotasChanged,
                                                label = { Text("Notas Extras") },
                                                placeholder = {
                                                        Text(
                                                                "Ej: sin salsa, extra servilletas…",
                                                                fontSize = 13.sp
                                                        )
                                                },
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(horizontal = 4.dp),
                                                maxLines = 3,
                                                shape = RoundedCornerShape(12.dp)
                                        )
                                }

                                // ── Botón Terminar Pedido ────────────────────────────────────────
                                if (onTerminarPedido != null) {
                                        item {
                                                Button(
                                                        onClick = onTerminarPedido,
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(
                                                                                horizontal = 4.dp,
                                                                                vertical = 8.dp
                                                                        ),
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors =
                                                                ButtonDefaults.buttonColors(
                                                                        containerColor =
                                                                                appCarbonBlack,
                                                                        contentColor = Color.White
                                                                )
                                                ) {
                                                        Text(
                                                                "Terminar Pedido",
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }
                                        }
                                }
                        }

                        // ── Variation Picker Bottom Sheet overlay ─────────────────────────────
                        if (pendingVariation != null) {
                                VariationPickerBottomSheet(
                                        request = pendingVariation,
                                        onConfirm = { selections ->
                                                onVariationSelected(
                                                        pendingVariation.item,
                                                        selections
                                                )
                                        },
                                        onDismiss = onVariationDismissed
                                )
                        }
                        // ── Shopping Cart Dialog ──────────────────────────────────────────
                        if (showCartDialog) {
                                ShoppingCartDialog(
                                        items = cartItems,
                                        visible = showCartDialog,
                                        onDismiss = { showCartDialog = false },
                                        onRemoveItem = onRemoveCartItem,
                                        onCheckout = {
                                                showMessage("Pedido realizado")
                                                showCartDialog = false
                                                onCheckout()
                                        }
                                )
                        }
                }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryTile — flat-design expandable section header (app palette)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CategoryTile(
        category: MenuCategory,
        colorIndex: Int, // kept for API compat, not used for color
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

        // How many items from this category are in the cart
        val selectedCount =
                category.items.sumOf { item ->
                        val base = quantities[item.name] ?: 0
                        val combo =
                                if (item.isBurger) (quantities["${item.name}_Combo"] ?: 0) else 0
                        base + combo
                }
        val hasItems = selectedCount > 0

        val arrowRotation by
                animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        animationSpec = tween(280),
                        label = "arrow"
                )

        // Outer box that acts as the "recuadro" (rectangle card)
        // — no Card elevation, just a flat bordered box
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                        width = if (hasItems) 2.dp else 1.dp,
                                        color =
                                                if (hasItems) appCarbonBlack
                                                else appCharcoalCool.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(10.dp)
                                )
                                .background(appBrightSnow)
        ) {
                Column {
                        // ── Header row — the tappable tile ──────────────────────────────
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .background(appCarbonBlack)
                                                .clickable { expanded = !expanded }
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                // Category name + count
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = category.name,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                                text = "${category.items.size} platillos",
                                                fontSize = 11.sp,
                                                color = appCarbonBlack
                                        )
                                }

                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                        // Selection badge
                                        if (hasItems) {
                                                Box(
                                                        modifier =
                                                                Modifier.size(28.dp)
                                                                        .clip(CircleShape)
                                                                        .background(appCarbonBlack),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Text(
                                                                text = selectedCount.toString(),
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = appCarbonBlack
                                                        )
                                                }
                                        }
                                        // Animated chevron
                                        Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription =
                                                        if (expanded) "Colapsar" else "Expandir",
                                                tint =
                                                        if (hasItems) appCarbonBlack
                                                        else Color.White.copy(alpha = 0.7f),
                                                modifier =
                                                        Modifier.size(24.dp).rotate(arrowRotation)
                                        )
                                }
                        }

                        // ── Expanding products section ───────────────────────────────────
                        AnimatedVisibility(
                                visible = expanded,
                                enter = expandVertically(animationSpec = tween(280)),
                                exit = shrinkVertically(animationSpec = tween(280))
                        ) {
                                Column {
                                        // Thin Grayish_lime separator line
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .height(1.5.dp)
                                                                .background(
                                                                        appCarbonBlack.copy(
                                                                                alpha = 0.5f
                                                                        )
                                                                )
                                        )
                                        ProductGrid(
                                                items = category.items,
                                                quantities = quantities,
                                                onAddProduct = onAddProduct,
                                                onRemoveProduct = onRemoveProduct,
                                                onAddBurgerNormal = onAddBurgerNormal,
                                                onRemoveBurgerNormal = onRemoveBurgerNormal,
                                                onAddBurgerCombo = onAddBurgerCombo,
                                                onRemoveBurgerCombo = onRemoveBurgerCombo,
                                                onOpenComment = onOpenComment,
                                                accentColor = appCarbonBlack
                                        )
                                }
                        }
                }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// ProductGrid — adaptive columns
// ─────────────────────────────────────────────────────────────────────────────
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ProductGrid(
        items: List<MenuItem>,
        quantities: Map<String, Int>,
        columns: Int = -1,
        onAddProduct: (MenuItem) -> Unit,
        onRemoveProduct: (MenuItem) -> Unit,
        onAddBurgerNormal: (MenuItem) -> Unit,
        onRemoveBurgerNormal: (MenuItem) -> Unit,
        onAddBurgerCombo: (MenuItem) -> Unit,
        onRemoveBurgerCombo: (MenuItem) -> Unit,
        onOpenComment: (String) -> Unit,
        onLongClickAddProduct: ((MenuItem) -> Unit)? = null,
        accentColor: Color = Color.Gray
) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cols =
                        when {
                                columns != -1 -> columns
                                maxWidth >= 600.dp -> 3
                                maxWidth >= 360.dp -> 2
                                else -> 1
                        }

                val chunkedItems = items.chunked(cols)
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(
                                                start = 8.dp,
                                                end = 8.dp,
                                                top = 8.dp,
                                                bottom = 8.dp
                                        ),
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
                                                                quantity = quantities[item.name]
                                                                                ?: 0,
                                                                quantityCombo =
                                                                        if (item.isBurger)
                                                                                quantities[
                                                                                        "${item.name}_Combo"]
                                                                                        ?: 0
                                                                        else 0,
                                                                onAddProduct = {
                                                                        onAddProduct(item)
                                                                },
                                                                onRemoveProduct = {
                                                                        onRemoveProduct(item)
                                                                },
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
                                                                onOpenComment = {
                                                                        onOpenComment(item.name)
                                                                },
                                                                onLongClickAddProduct =
                                                                        onLongClickAddProduct
                                                                                ?.let { cb ->
                                                                                        { cb(item) }
                                                                                },
                                                                accentColor = accentColor
                                                        )
                                                }
                                        }
                                        repeat(cols - rowItems.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                        }
                                }
                        }
                }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// ProductCard — responsive
// ─────────────────────────────────────────────────────────────────────────────
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductCard(
        item: MenuItem,
        quantity: Int,
        quantityCombo: Int,
        accentColor: Color,
        onAddProduct: () -> Unit,
        onRemoveProduct: () -> Unit,
        onAddBurgerNormal: () -> Unit,
        onRemoveBurgerNormal: () -> Unit,
        onAddBurgerCombo: () -> Unit,
        onRemoveBurgerCombo: () -> Unit,
        onOpenComment: () -> Unit,
        onLongClickAddProduct: (() -> Unit)? = null
) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isCompact = maxWidth < 130.dp
                val btnSize = if (isCompact) 28.dp else 36.dp
                val textSizeQty = if (isCompact) 13.sp else 15.sp
                val hPadding = if (isCompact) 4.dp else 8.dp
                val hasSelection = quantity > 0 || quantityCombo > 0

                val cardBorder =
                        if (hasSelection)
                                Modifier.border(1.5.dp, accentColor, RoundedCornerShape(12.dp))
                        else Modifier

                Card(
                        modifier =
                                Modifier.fillMaxWidth().then(cardBorder).clickable {
                                        if (item.isBurger) onAddBurgerNormal() else onAddProduct()
                                },
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = colorResource(id = R.color.BrigthSnow)
                                ),
                        elevation =
                                CardDefaults.cardElevation(
                                        defaultElevation = if (hasSelection) 4.dp else 1.dp
                                ),
                        shape = RoundedCornerShape(12.dp)
                ) {
                        Column(
                                modifier = Modifier.fillMaxWidth().padding(hPadding),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                Text(
                                        text = item.name,
                                        fontSize = if (isCompact) 11.sp else 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color =
                                                if (hasSelection) accentColor
                                                else colorResource(id = R.color.text_secondary),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(bottom = 4.dp).fillMaxWidth()
                                )

                                if (item.isBurger) {
                                        // Hamburguesa UI
                                        BurgerQuantityRow(
                                                label = "Sencilla",
                                                quantity = quantity,
                                                onMinusClick = onRemoveBurgerNormal,
                                                onPlusClick = onAddBurgerNormal,
                                                isCompact = isCompact,
                                                accentColor = accentColor
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        BurgerQuantityRow(
                                                label = "Combo",
                                                quantity = quantityCombo,
                                                onMinusClick = onRemoveBurgerCombo,
                                                onPlusClick = onAddBurgerCombo,
                                                isCompact = isCompact,
                                                accentColor = accentColor
                                        )
                                        IconButton(
                                                onClick = onOpenComment,
                                                modifier =
                                                        Modifier.size(btnSize)
                                                                .align(Alignment.Start)
                                        ) {
                                                Icon(
                                                        painter =
                                                                painterResource(
                                                                        id = R.drawable.ic_comment
                                                                ),
                                                        contentDescription = "Comentario",
                                                        tint = colorResource(id = R.color.Grey)
                                                )
                                        }
                                } else {
                                        // Producto Normal UI (Compact vs Full)
                                        if (isCompact) {
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        FilledIconButton(
                                                                onClick = onRemoveProduct,
                                                                modifier = Modifier.size(btnSize),
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
                                                                                                Color.White
                                                                                ),
                                                                shape = RoundedCornerShape(14.dp)
                                                        ) {
                                                                Text(
                                                                        "-",
                                                                        fontSize = 14.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        }

                                                        Text(
                                                                quantity.toString(),
                                                                fontSize = textSizeQty,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                        horizontal =
                                                                                                4.dp
                                                                                )
                                                                                .defaultMinSize(
                                                                                        minWidth =
                                                                                                18.dp
                                                                                ),
                                                                textAlign = TextAlign.Center
                                                        )

                                                        Box(
                                                                modifier =
                                                                        Modifier.combinedClickable(
                                                                                onClick =
                                                                                        onAddProduct,
                                                                                onLongClick =
                                                                                        onLongClickAddProduct
                                                                        )
                                                        ) {
                                                                FilledIconButton(
                                                                        onClick = {},
                                                                        modifier =
                                                                                Modifier.size(
                                                                                        btnSize
                                                                                ),
                                                                        colors =
                                                                                IconButtonDefaults
                                                                                        .filledIconButtonColors(
                                                                                                containerColor =
                                                                                                        accentColor,
                                                                                                contentColor =
                                                                                                        Color.White
                                                                                        ),
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        14.dp
                                                                                )
                                                                ) {
                                                                        Text(
                                                                                "+",
                                                                                fontSize = 14.sp,
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        )
                                                                }
                                                        }
                                                }
                                        } else {
                                                // Standard Non-Compact Layout
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        IconButton(
                                                                onClick = onOpenComment,
                                                                modifier = Modifier.size(btnSize)
                                                        ) {
                                                                Icon(
                                                                        painter =
                                                                                painterResource(
                                                                                        id =
                                                                                                R.drawable
                                                                                                        .ic_comment
                                                                                ),
                                                                        contentDescription =
                                                                                "Comentario",
                                                                        tint =
                                                                                colorResource(
                                                                                        id =
                                                                                                R.color
                                                                                                        .Grey
                                                                                )
                                                                )
                                                        }
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                // Re-using the same
                                                                // FilledIconButton pattern for
                                                                // consistency
                                                                FilledIconButton(
                                                                        onClick = onRemoveProduct,
                                                                        modifier =
                                                                                Modifier.size(
                                                                                        btnSize
                                                                                ),
                                                                        colors =
                                                                                IconButtonDefaults
                                                                                        .filledIconButtonColors(
                                                                                                containerColor =
                                                                                                        colorResource(
                                                                                                                id =
                                                                                                                        R.color
                                                                                                                                .Grey
                                                                                                        )
                                                                                        ),
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        18.dp
                                                                                )
                                                                ) {
                                                                        Text(
                                                                                "-",
                                                                                fontSize = 15.sp,
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        )
                                                                }

                                                                Text(
                                                                        quantity.toString(),
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                                horizontal =
                                                                                                        6.dp
                                                                                        )
                                                                                        .defaultMinSize(
                                                                                                minWidth =
                                                                                                        22.dp
                                                                                        ),
                                                                        textAlign = TextAlign.Center
                                                                )

                                                                Box(
                                                                        modifier =
                                                                                Modifier.combinedClickable(
                                                                                        onClick =
                                                                                                onAddProduct,
                                                                                        onLongClick =
                                                                                                onLongClickAddProduct
                                                                                )
                                                                ) {
                                                                        FilledIconButton(
                                                                                onClick = {},
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                btnSize
                                                                                        ),
                                                                                colors =
                                                                                        IconButtonDefaults
                                                                                                .filledIconButtonColors(
                                                                                                        containerColor =
                                                                                                                accentColor
                                                                                                ),
                                                                                shape =
                                                                                        RoundedCornerShape(
                                                                                                18.dp
                                                                                        )
                                                                        ) {
                                                                                Text(
                                                                                        "+",
                                                                                        fontSize =
                                                                                                15.sp,
                                                                                        fontWeight =
                                                                                                FontWeight
                                                                                                        .Bold
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
}
// ─────────────────────────────────────────────────────────────────────────────
// BurgerQuantityRow
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BurgerQuantityRow(
        label: String,
        quantity: Int,
        onMinusClick: () -> Unit,
        onPlusClick: () -> Unit,
        isCompact: Boolean = false,
        accentColor: Color = Color.Gray
) {
        val btnSize = if (isCompact) 26.dp else 32.dp
        val fontSize = if (isCompact) 11.sp else 12.sp
        val numFontSize = if (isCompact) 12.sp else 14.sp
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                Text(
                        text = label,
                        fontSize = fontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(
                                onClick = onMinusClick,
                                modifier = Modifier.size(btnSize),
                                colors =
                                        IconButtonDefaults.filledIconButtonColors(
                                                containerColor = colorResource(id = R.color.Grey),
                                                contentColor = Color.White
                                        ),
                                shape = RoundedCornerShape(16.dp)
                        ) { Text("-", fontSize = numFontSize) }
                        Text(
                                text = quantity.toString(),
                                fontSize = numFontSize,
                                fontWeight = FontWeight.Bold,
                                modifier =
                                        Modifier.padding(horizontal = if (isCompact) 3.dp else 6.dp)
                                                .defaultMinSize(minWidth = 16.dp),
                                textAlign = TextAlign.Center
                        )
                        FilledIconButton(
                                onClick = onPlusClick,
                                modifier = Modifier.size(btnSize),
                                colors =
                                        IconButtonDefaults.filledIconButtonColors(
                                                containerColor = accentColor,
                                                contentColor = Color.White
                                        ),
                                shape = RoundedCornerShape(16.dp)
                        ) { Text("+", fontSize = numFontSize) }
                }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryGridView — Muestra grid de categorías con botones inferiores
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CategoryGridView(
        categories: List<MenuCategory>,
        existingOrderDetails: String?,
        notasExtras: String,
        onNotasChanged: (String) -> Unit,
        onCategoryClick: (MenuCategory) -> Unit,
        onShowCart: () -> Unit,
        onTerminarPedido: (() -> Unit)?
) {
        LazyColumn(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
                // ── Existing Order Card ──────────────────────────────────────────
                if (existingOrderDetails != null) {
                        item {
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                colorResource(
                                                                        id = R.color.app_surface
                                                                )
                                                ),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                        "Pedido Existente",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color =
                                                                colorResource(
                                                                        id = R.color.app_primary
                                                                )
                                                )
                                                Text(
                                                        existingOrderDetails,
                                                        fontSize = 14.sp,
                                                        modifier = Modifier.padding(top = 8.dp)
                                                )
                                        }
                                }
                        }
                }

                // ── Título de Categorías ──────────────────────────────────────────
                item {
                        Text(
                                text = "Categorías de Platillos",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = appCarbonBlack,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                }

                // ── Grid de Categorías ────────────────────────────────────────────
                item {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val cols =
                                        when {
                                                maxWidth >= 900.dp -> 4
                                                maxWidth >= 600.dp -> 3
                                                maxWidth >= 360.dp -> 2
                                                else -> 1
                                        }
                                LazyVerticalGrid(
                                        columns = GridCells.Fixed(cols),
                                        contentPadding = PaddingValues(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
                                ) {
                                        items(categories) { category ->
                                                AnimatedCategoryCard(category = category) {
                                                        onCategoryClick(category)
                                                }
                                        }
                                }
                        }
                }

                // ── Notas Extras ──────────────────────────────────────────────────
                item {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                                value = notasExtras,
                                onValueChange = onNotasChanged,
                                label = { Text("Notas Extras") },
                                placeholder = {
                                        Text("Ej: sin salsa, extra servilletas…", fontSize = 13.sp)
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                maxLines = 3,
                                shape = RoundedCornerShape(12.dp)
                        )
                }

                // ── Ver Carrito de Compra ─────────────────────────────────────────
                item {
                        Button(
                                onClick = onShowCart,
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = appCarbonBlack,
                                                contentColor = Color.White
                                        )
                        ) {
                                Text(
                                        "Ver Carrito de Compra",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                )
                        }
                }

                // ── Terminar Pedido ────────────────────────────────────────────────
                if (onTerminarPedido != null) {
                        item {
                                Button(
                                        onClick = onTerminarPedido,
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(
                                                                horizontal = 4.dp,
                                                                vertical = 8.dp
                                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = appCarbonBlack,
                                                        contentColor = Color.White
                                                )
                                ) {
                                        Text(
                                                "Terminar Pedido",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                }
                        }
                }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// AnimatedCategoryCard — Card de categoría con animación de presionado
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AnimatedCategoryCard(category: MenuCategory, onClick: () -> Unit) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by
                animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "scale")

        Card(
                modifier =
                        Modifier.fillMaxWidth()
                                .scale(scale)
                                .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = onClick
                                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF6FB))
        ) {
                Column {
                        // Hero limpio sin iconografia para un look elegante.
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(122.dp)
                                                .background(appCarbonBlack.copy(alpha = 0.16f))
                        ) {
                                Box(
                                        modifier =
                                                Modifier.align(Alignment.TopEnd)
                                                        .padding(12.dp)
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White.copy(alpha = 0.32f))
                                )
                                Box(
                                        modifier =
                                                Modifier.align(Alignment.BottomStart)
                                                        .fillMaxWidth()
                                                        .height(3.dp)
                                                        .background(
                                                                appCarbonBlack.copy(alpha = 0.65f)
                                                        )
                                )
                        }

                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = category.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = appCarbonBlack
                                        )
                                        Text(
                                                text = "${category.items.size} platillos",
                                                fontSize = 12.sp,
                                                color = appCharcoalCool
                                        )
                                }
                        }
                }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// ProductDetailList — Lista de productos con cards detalladas
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ProductDetailList(
        category: MenuCategory,
        quantities: Map<String, Int>,
        onAddProduct: (MenuItem) -> Unit,
        onRemoveProduct: (MenuItem) -> Unit,
        onAddBurgerNormal: (MenuItem) -> Unit,
        onRemoveBurgerNormal: (MenuItem) -> Unit,
        onAddBurgerCombo: (MenuItem) -> Unit,
        onRemoveBurgerCombo: (MenuItem) -> Unit,
        onOpenComment: (String) -> Unit,
        onSaveQuickComment: (String, String) -> Boolean,
        onShowSnackbar: (String) -> Unit
) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                val totalItems = category.items.size
                val maxRows = 3
                val columns =
                        if (totalItems == 0) 1
                        else ((totalItems + maxRows - 1) / maxRows).coerceAtLeast(1)
                val grouped = category.items.chunked(columns)

                Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        grouped.forEach { rowItems ->
                                Row(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        rowItems.forEach { item ->
                                                ProductDetailCard(
                                                        item = item,
                                                        quantity = quantities[item.name] ?: 0,
                                                        quantityCombo =
                                                                if (item.isBurger)
                                                                        (quantities[
                                                                                "${item.name}_Combo"]
                                                                                ?: 0)
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
                                                        onOpenComment = {
                                                                onOpenComment(item.name)
                                                        },
                                                        onSaveQuickComment = { comment ->
                                                                onSaveQuickComment(
                                                                        item.name,
                                                                        comment
                                                                )
                                                        },
                                                        onShowSnackbar = onShowSnackbar,
                                                        modifier =
                                                                Modifier.weight(1f).fillMaxHeight()
                                                )
                                        }
                                        repeat(columns - rowItems.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                        }
                                }
                        }
                        repeat((maxRows - grouped.size).coerceAtLeast(0)) {
                                Spacer(modifier = Modifier.weight(1f))
                        }
                }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// ProductDetailCard — Card detallada de producto con imagen superior
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ProductDetailCard(
        item: MenuItem,
        quantity: Int,
        quantityCombo: Int,
        onAddProduct: () -> Unit,
        onRemoveProduct: () -> Unit,
        onAddBurgerNormal: () -> Unit,
        onRemoveBurgerNormal: () -> Unit,
        onAddBurgerCombo: () -> Unit,
        onRemoveBurgerCombo: () -> Unit,
        onOpenComment: () -> Unit,
        onSaveQuickComment: (String) -> Boolean,
        onShowSnackbar: (String) -> Unit,
        modifier: Modifier = Modifier
) {
        var quickComment by remember(item.name) { mutableStateOf("") }
        val canAttachComment = (quantity + quantityCombo) > 0

        Card(
                modifier = modifier,
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                        ) {
                                Text(
                                        text = item.name,
                                        color = appCarbonBlack,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                        onClick = onOpenComment,
                                        modifier = Modifier.size(30.dp)
                                ) {
                                        Icon(
                                                painter =
                                                        painterResource(id = R.drawable.ic_comment),
                                                contentDescription = "Comentario",
                                                tint = appCharcoalCool,
                                                modifier = Modifier.size(16.dp)
                                        )
                                }
                        }

                        if (item.isBurger) {
                                DetailedBurgerControls(
                                        quantityNormal = quantity,
                                        quantityCombo = quantityCombo,
                                        onMinusNormal = onRemoveBurgerNormal,
                                        onPlusNormal = onAddBurgerNormal,
                                        onMinusCombo = onRemoveBurgerCombo,
                                        onPlusCombo = onAddBurgerCombo,
                                        compact = true
                                )
                        } else {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        FilledIconButton(
                                                onClick = onRemoveProduct,
                                                modifier = Modifier.size(34.dp),
                                                colors =
                                                        IconButtonDefaults.filledIconButtonColors(
                                                                containerColor =
                                                                        colorResource(
                                                                                id = R.color.Grey
                                                                        ),
                                                                contentColor = Color.White
                                                        ),
                                                shape = RoundedCornerShape(10.dp)
                                        ) {
                                                Text(
                                                        "-",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }

                                        Text(
                                                text = quantity.toString(),
                                                modifier = Modifier.padding(horizontal = 10.dp),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = appCarbonBlack
                                        )

                                        FilledIconButton(
                                                onClick = onAddProduct,
                                                modifier = Modifier.size(34.dp),
                                                colors =
                                                        IconButtonDefaults.filledIconButtonColors(
                                                                containerColor = appCarbonBlack,
                                                                contentColor = Color.White
                                                        ),
                                                shape = RoundedCornerShape(10.dp)
                                        ) {
                                                Text(
                                                        "+",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }
                                }
                        }

                        OutlinedTextField(
                                value = quickComment,
                                onValueChange = { quickComment = it.take(200) },
                                placeholder = {
                                        Text(
                                                "Comentario especial (detalles importantes)",
                                                fontSize = 11.sp,
                                                color = appCharcoalCool
                                        )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                minLines = 2,
                                maxLines = 3,
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )

                        Button(
                                onClick = {
                                        val note = quickComment.trim()
                                        if (note.isEmpty()) return@Button
                                        if (!canAttachComment) {
                                                onShowSnackbar(
                                                        "Agrega cantidad antes de comentar ${item.name}"
                                                )
                                                return@Button
                                        }
                                        val saved = onSaveQuickComment(note)
                                        if (saved) {
                                                onShowSnackbar("Comentario guardado")
                                                quickComment = ""
                                        } else {
                                                onShowSnackbar("No se pudo guardar comentario")
                                        }
                                },
                                enabled = quickComment.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = appCarbonBlack,
                                                contentColor = Color.White
                                        )
                        ) {
                                Text(
                                        "Guardar comentario",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                )
                        }
                }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// DetailedBurgerControls — Controles de hamburguesa para card detallada
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DetailedBurgerControls(
        quantityNormal: Int,
        quantityCombo: Int,
        onMinusNormal: () -> Unit,
        onPlusNormal: () -> Unit,
        onMinusCombo: () -> Unit,
        onPlusCombo: () -> Unit,
        compact: Boolean = false
) {
        val buttonSize = if (compact) 34.dp else 40.dp
        val labelFont = if (compact) 11.sp else 16.sp
        val qtyFont = if (compact) 14.sp else 18.sp
        val innerPadding = if (compact) 6.dp else 16.dp

        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
                // Hamburguesa Sencilla
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Text(
                                text = "Sencilla",
                                fontSize = labelFont,
                                fontWeight = FontWeight.SemiBold,
                                color = appCarbonBlack
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledIconButton(
                                        onClick = onMinusNormal,
                                        modifier = Modifier.size(buttonSize),
                                        colors =
                                                IconButtonDefaults.filledIconButtonColors(
                                                        containerColor =
                                                                colorResource(id = R.color.Grey)
                                                ),
                                        shape = RoundedCornerShape(10.dp)
                                ) { Text("-", fontSize = qtyFont, fontWeight = FontWeight.Bold) }

                                Text(
                                        text = quantityNormal.toString(),
                                        fontSize = qtyFont,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = innerPadding),
                                        color = appCarbonBlack
                                )

                                FilledIconButton(
                                        onClick = onPlusNormal,
                                        modifier = Modifier.size(buttonSize),
                                        colors =
                                                IconButtonDefaults.filledIconButtonColors(
                                                        containerColor = appCarbonBlack
                                                ),
                                        shape = RoundedCornerShape(10.dp)
                                ) { Text("+", fontSize = qtyFont, fontWeight = FontWeight.Bold) }
                        }
                }

                // Hamburguesa Combo
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Text(
                                text = "Combo",
                                fontSize = labelFont,
                                fontWeight = FontWeight.SemiBold,
                                color = appCarbonBlack
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledIconButton(
                                        onClick = onMinusCombo,
                                        modifier = Modifier.size(buttonSize),
                                        colors =
                                                IconButtonDefaults.filledIconButtonColors(
                                                        containerColor =
                                                                colorResource(id = R.color.Grey)
                                                ),
                                        shape = RoundedCornerShape(10.dp)
                                ) { Text("-", fontSize = qtyFont, fontWeight = FontWeight.Bold) }

                                Text(
                                        text = quantityCombo.toString(),
                                        fontSize = qtyFont,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = innerPadding),
                                        color = appCarbonBlack
                                )

                                FilledIconButton(
                                        onClick = onPlusCombo,
                                        modifier = Modifier.size(buttonSize),
                                        colors =
                                                IconButtonDefaults.filledIconButtonColors(
                                                        containerColor = appCarbonBlack
                                                ),
                                        shape = RoundedCornerShape(10.dp)
                                ) { Text("+", fontSize = qtyFont, fontWeight = FontWeight.Bold) }
                        }
                }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// VariationPickerBottomSheet — modal, scrollable guisado selector
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VariationPickerBottomSheet(
        request: VariationRequest,
        onConfirm: (List<String>) -> Unit,
        onDismiss: () -> Unit
) {
        val item = request.item
        val variations = item.variations
        val isMulti = request.isMultiSelect
        val minSelections = request.minSelections
        val context = LocalContext.current

        var selectedItems by remember(item.name) { mutableStateOf(emptyList<String>()) }

        // Keep accent color by variation type to preserve quick visual context.
        val headerColor =
                when (item.variationType) {
                        VariationType.MULTIPLE_SELECTION -> Color(0xFFB71C1C)
                        VariationType.SINGLE_SELECTION -> Color(0xFF2E7D32)
                        else -> Color(0xFF1565C0)
                }

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                containerColor = appBrightSnow,
                dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 16.dp)
                ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = "Elige tu opción",
                                                fontSize = 13.sp,
                                                color = appCharcoalCool
                                        )
                                        Text(
                                                text = item.name,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = headerColor
                                        )
                                        if (isMulti) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                        text =
                                                                "Selecciona al menos $minSelections guisado(s)",
                                                        fontSize = 13.sp,
                                                        color = appCharcoalCool,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                )
                                        }
                                }
                                IconButton(onClick = onDismiss) {
                                        Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Cerrar",
                                                tint = appCharcoalCool
                                        )
                                }
                        }
                        Text(
                                item.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = headerColor,
                                modifier =
                                        Modifier.combinedClickable(
                                                onClick = {},
                                                onLongClick = {
                                                        if (item.name.contains(
                                                                        "refresco",
                                                                        ignoreCase = true
                                                                ) ||
                                                                        item.name.contains(
                                                                                "aguas",
                                                                                ignoreCase = true
                                                                        )
                                                        ) {

                                                                val intent =
                                                                        Intent(
                                                                                context,
                                                                                InventoryRefrescoAdapter::class
                                                                                        .java
                                                                        )
                                                                context.startActivity(intent)
                                                        }
                                                }
                                        )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                                Column(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        if (variations.isEmpty()) {
                                                Text(
                                                        text = "No hay opciones disponibles",
                                                        color = appCharcoalCool,
                                                        fontSize = 14.sp,
                                                        modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                        } else {
                                                variations.chunked(2).forEach { rowItems ->
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement =
                                                                        Arrangement.spacedBy(8.dp)
                                                        ) {
                                                                rowItems.forEach { variant ->
                                                                        val isSelected =
                                                                                selectedItems
                                                                                        .contains(
                                                                                                variant
                                                                                        )
                                                                        VariationChip(
                                                                                label = variant,
                                                                                selected =
                                                                                        isSelected,
                                                                                accentColor =
                                                                                        headerColor,
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        ),
                                                                                onClick = {
                                                                                        if (isMulti
                                                                                        ) {
                                                                                                selectedItems =
                                                                                                        if (isSelected
                                                                                                        ) {
                                                                                                                selectedItems -
                                                                                                                        variant
                                                                                                        } else {
                                                                                                                selectedItems +
                                                                                                                        variant
                                                                                                        }
                                                                                        } else {
                                                                                                onConfirm(
                                                                                                        listOf(
                                                                                                                variant
                                                                                                        )
                                                                                                )
                                                                                        }
                                                                                }
                                                                        )
                                                                }
                                                                if (rowItems.size == 1) {
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        )
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }

                        if (isMulti) {
                                Divider(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp))
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        OutlinedButton(
                                                onClick = onDismiss,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp)
                                        ) { Text("Cancelar") }

                                        Button(
                                                onClick = {
                                                        if (selectedItems.size >= minSelections) {
                                                                onConfirm(selectedItems)
                                                        }
                                                },
                                                enabled = selectedItems.size >= minSelections,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor = headerColor
                                                        )
                                        ) {
                                                val label =
                                                        if (selectedItems.size < minSelections) {
                                                                "Elige mín. $minSelections"
                                                        } else {
                                                                "Listo (${selectedItems.size})"
                                                        }
                                                Text(label, fontSize = 13.sp)
                                        }
                                }
                        }
                }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// VariationChip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun VariationChip(
        label: String,
        selected: Boolean,
        accentColor: Color,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
) {
        val bgColor by
                animateColorAsState(
                        targetValue = if (selected) accentColor else Color.Transparent,
                        animationSpec = tween(200),
                        label = "chip_bg"
                )
        val textColor by
                animateColorAsState(
                        targetValue = if (selected) Color.White else accentColor,
                        animationSpec = tween(200),
                        label = "chip_text"
                )

        Box(
                modifier =
                        modifier.clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .border(1.5.dp, accentColor, RoundedCornerShape(12.dp))
                                .clickable(onClick = onClick)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
        ) {
                Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                )
        }
}



