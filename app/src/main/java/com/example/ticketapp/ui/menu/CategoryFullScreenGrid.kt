package com.example.ticketapp.ui.menu

// ─────────────────────────────────────────────────────────────────────────────
// CategoryFullScreenGrid — versión 3
// FIX: El overlay ahora se renderiza via Dialog (fullscreen) para salir del
//      árbol de LazyColumn y evitar el crash "infinity height constraints".
// ─────────────────────────────────────────────────────────────────────────────

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ticketapp.R
import com.example.ticketapp.data.menu.MenuCategory
import com.example.ticketapp.data.menu.MenuItem
import com.example.ticketapp.ui.Cards.MenuCardGrid

// ── Paleta de acento derivada de colors.xml ───────────────────────────────────
private val tileAccents = listOf(
    Color(0xFF222823), // CarbonBlack
    Color(0xFF575A5E), // CharcoalCool
    Color(0xFF819A91), // Grey
    Color(0xFF3D5A52), // variante verde oscuro
    Color(0xFF454849), // CharcoalCool oscuro
    Color(0xFF6B887E), // Grey aclarado
    Color(0xFF2E3D2F), // Carbon + Grayish_lime
    Color(0xFF4A5568), // slate neutro
)

// ─────────────────────────────────────────────────────────────────────────────
// CategoryFullScreenGrid — punto de entrada
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CategoryFullScreenGrid(
    categories: List<MenuCategory>,
    quantities: Map<String, Int>,
    onAddProduct: (MenuItem) -> Unit,
    onRemoveProduct: (MenuItem) -> Unit,
    onAddBurgerNormal: (MenuItem) -> Unit,
    onRemoveBurgerNormal: (MenuItem) -> Unit,
    onAddBurgerCombo: (MenuItem) -> Unit,
    onRemoveBurgerCombo: (MenuItem) -> Unit,
    onOpenComment: (String) -> Unit,
    onLongClickAddProduct: ((MenuItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val brightSnow  = colorResource(R.color.BrigthSnow)
    val grayishLime = colorResource(R.color.Grayish_lime)

    var selectedCategory by remember { mutableStateOf<MenuCategory?>(null) }
    var selectedAccent   by remember { mutableStateOf(tileAccents[0]) }

    // ── Grid de categorías (2 columnas) ─────────────────────────────────────
    LazyVerticalGrid(
        columns               = GridCells.Fixed(2),
        modifier              = modifier
            .fillMaxWidth()
            .heightIn(max = 900.dp),
        contentPadding        = PaddingValues(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement   = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(categories) { index, category ->
            val accent = tileAccents[index % tileAccents.size]
            val selectedCount = category.items.sumOf { item ->
                val base  = quantities[item.name] ?: 0
                val combo = if (item.isBurger) (quantities["${item.name}_Combo"] ?: 0) else 0
                base + combo
            }
            CategoryGridTile(
                category       = category,
                accentColor    = accent,
                highlightColor = grayishLime,
                selectedCount  = selectedCount,
                onSelect       = {
                    selectedCategory = category
                    selectedAccent   = accent
                }
            )
        }
    }

    // ── Overlay de pantalla completa — Dialog (sale del árbol LazyColumn) ────
    // Se usa Dialog en lugar de AnimatedVisibility/Box para evitar el crash
    // "infinity height constraints" que ocurre al anidar verticalScroll dentro
    // de un LazyColumn.item con altura no acotada.
    if (selectedCategory != null) {
        val cat = selectedCategory!!

        BackHandler { selectedCategory = null }

        Dialog(
            onDismissRequest = { selectedCategory = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,   // diálogo a pantalla completa
                dismissOnBackPress      = true,
                dismissOnClickOutside   = false
            )
        ) {
            CategoryFullScreenOverlay(
                category              = cat,
                accentColor           = selectedAccent,
                surfaceColor          = brightSnow,
                highlightColor        = grayishLime,
                quantities            = quantities,
                onBack                = { selectedCategory = null },
                onAddProduct          = onAddProduct,
                onRemoveProduct       = onRemoveProduct,
                onLongClickAddProduct = onLongClickAddProduct
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryGridTile — tarjeta de categoría en la cuadrícula
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryGridTile(
    category: MenuCategory,
    accentColor: Color,
    highlightColor: Color,
    selectedCount: Int,
    onSelect: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "tileScale"
    )

    val hasItems = selectedCount > 0

    Card(
        onClick           = onSelect,
        interactionSource = interactionSource,
        modifier          = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .scale(scale),
        shape             = RoundedCornerShape(18.dp),
        elevation         = CardDefaults.cardElevation(
            defaultElevation = if (hasItems) 6.dp else 2.dp
        ),
        colors            = CardDefaults.cardColors(containerColor = accentColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Degradado sutil para profundidad
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color.Black.copy(alpha = 0.18f)
                            )
                        )
                    )
            )

            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Badge de ítems seleccionados (esquina superior derecha)
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (hasItems) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(highlightColor.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = selectedCount.toString(),
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color(0xFF222823)
                            )
                        }
                    }
                }

                // Nombre y número de platillos
                Column {
                    Text(
                        text       = category.name,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text     = "${category.items.size} platillos",
                        fontSize = 12.sp,
                        color    = Color.White.copy(alpha = 0.72f)
                    )
                }
            }

            // Barra inferior cuando hay ítems seleccionados
            if (hasItems) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(highlightColor)
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryFullScreenOverlay — pantalla completa de una categoría
// Se muestra dentro de un Dialog para salir del árbol de LazyColumn.
// verticalScroll aquí funciona correctamente porque Dialog tiene altura acotada.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryFullScreenOverlay(
    category: MenuCategory,
    accentColor: Color,
    surfaceColor: Color,
    highlightColor: Color,
    quantities: Map<String, Int>,
    onBack: () -> Unit,
    onAddProduct: (MenuItem) -> Unit,
    onRemoveProduct: (MenuItem) -> Unit,
    onLongClickAddProduct: ((MenuItem) -> Unit)? = null
) {
    val selectedCount = category.items.sumOf { item ->
        val base  = quantities[item.name] ?: 0
        val combo = if (item.isBurger) (quantities["${item.name}_Combo"] ?: 0) else 0
        base + combo
    }

    // fillMaxSize es seguro aquí porque Dialog provee restricciones acotadas
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(accentColor, accentColor.copy(alpha = 0.80f))
                    )
                )
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector        = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint               = Color.White
                    )
                }
                Text(
                    text       = category.name,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White,
                    modifier   = Modifier.weight(1f),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (selectedCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(highlightColor.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = selectedCount.toString(),
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color(0xFF222823)
                        )
                    }
                }
            }
        }

        // Línea de acento
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(highlightColor, highlightColor.copy(alpha = 0.25f))
                    )
                )
        )

        // ── Productos scrollables ─────────────────────────────────────────────
        // verticalScroll funciona porque Dialog tiene altura máxima = pantalla
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            MenuCardGrid(
                items           = category.items,
                quantities      = quantities,
                accentColor     = accentColor,
                onAddProduct    = onAddProduct,
                onRemoveProduct = onRemoveProduct,
                onLongClickAdd  = onLongClickAddProduct
            )
        }
    }
}
