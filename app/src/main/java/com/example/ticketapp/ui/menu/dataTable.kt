package com.example.ticketapp.ui.menu

// ─────────────────────────────────────────────────────────────────────────────
// CategoryCardGrid
// Muestra las categorías como tarjetas visuales en una cuadrícula.
// Al tocar una card se expande con animación mostrando los productos.
// Paleta moderna: gradientes suaves sobre la base flat del proyecto.
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ticketapp.data.menu.MenuCategory
import com.example.ticketapp.data.menu.MenuItem

// ── Paleta de acento por categoría (cíclica) ────────────────────────────────
// Se usa solo para el color de encabezado de cada card.
// El resto sigue la paleta flat del proyecto.
private val cardAccents =
        listOf(
                Color(0xFF3D6B5F), // verde oscuro musgo
                Color(0xFF4A5568), // slate gris
                Color(0xFF5C4033), // marrón cálido
                Color(0xFF2D5A8E), // azul acero
                Color(0xFF6B4226), // terracota
                Color(0xFF3B5E4F), // verde bosque
                Color(0xFF5B4A6B), // morado suave
                Color(0xFF375A7F), // azul marino
        )

private val dtCarbonBlack = Color(0xFF222823)
private val dtCharcoalCool = Color(0xFF575A5E)
private val dtGrayishLime = Color(0xFFA7C1A8)
private val dtBrightSnow = Color(0xFFF4F7F5)
private val dtSurface = Color(0xFFFFFFFF)

// ─────────────────────────────────────────────────────────────────────────────
// CategoryCardGrid — punto de entrada
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CategoryCardGrid(
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
        // Sólo una tarjeta expandida a la vez (-1 = ninguna)
        var expandedIndex by remember { mutableStateOf(-1) }

        // Column simple (no lazy), ya que vivirá dentro de un LazyColumn padre
        Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
                categories.forEachIndexed { index, category ->
                        val accentColor = cardAccents[index % cardAccents.size]
                        val isExpanded = expandedIndex == index

                        val selectedCount =
                                category.items.sumOf { item ->
                                        val base = quantities[item.name] ?: 0
                                        val combo =
                                                if (item.isBurger)
                                                        (quantities["${item.name}_Combo"] ?: 0)
                                                else 0
                                        base + combo
                                }

                        ExpandableCategoryCard(
                                category = category,
                                accentColor = accentColor,
                                isExpanded = isExpanded,
                                selectedCount = selectedCount,
                                quantities = quantities,
                                onAddProduct = onAddProduct,
                                onRemoveProduct = onRemoveProduct,
                                onAddBurgerNormal = onAddBurgerNormal,
                                onRemoveBurgerNormal = onRemoveBurgerNormal,
                                onAddBurgerCombo = onAddBurgerCombo,
                                onRemoveBurgerCombo = onRemoveBurgerCombo,
                                onOpenComment = onOpenComment,
                                onLongClickAddProduct = onLongClickAddProduct,
                                onToggle = { expandedIndex = if (isExpanded) -1 else index }
                        )
                }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// ExpandableCategoryCard — tarjeta individual
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ExpandableCategoryCard(
        category: MenuCategory,
        accentColor: Color,
        isExpanded: Boolean,
        selectedCount: Int,
        quantities: Map<String, Int>,
        onAddProduct: (MenuItem) -> Unit,
        onRemoveProduct: (MenuItem) -> Unit,
        onAddBurgerNormal: (MenuItem) -> Unit,
        onRemoveBurgerNormal: (MenuItem) -> Unit,
        onAddBurgerCombo: (MenuItem) -> Unit,
        onRemoveBurgerCombo: (MenuItem) -> Unit,
        onOpenComment: (String) -> Unit,
        onLongClickAddProduct: ((MenuItem) -> Unit)? = null,
        onToggle: () -> Unit
) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        // Escala suave al presionar el header
        val headerScale by
                animateFloatAsState(
                        targetValue = if (isPressed) 0.98f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "headerScale"
                )

        // Rotación de la flecha
        val arrowAngle by
                animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec =
                                spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                ),
                        label = "arrow"
                )

        val hasItems = selectedCount > 0

        Card(
                shape = RoundedCornerShape(16.dp),
                elevation =
                        CardDefaults.cardElevation(
                                defaultElevation = if (isExpanded) 6.dp else 2.dp
                        ),
                colors = CardDefaults.cardColors(containerColor = dtSurface),
                modifier = Modifier.fillMaxWidth()
        ) {
                Column {
                        // ── HEADER de la tarjeta ─────────────────────────────────────────
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .scale(headerScale)
                                                .clickable(
                                                        interactionSource = interactionSource,
                                                        indication = null,
                                                        onClick = onToggle
                                                )
                                                .background(
                                                        brush =
                                                                Brush.horizontalGradient(
                                                                        colors =
                                                                                listOf(
                                                                                        accentColor,
                                                                                        accentColor
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.75f
                                                                                                )
                                                                                )
                                                                ),
                                                        shape =
                                                                if (isExpanded)
                                                                        RoundedCornerShape(
                                                                                topStart = 16.dp,
                                                                                topEnd = 16.dp
                                                                        )
                                                                else RoundedCornerShape(16.dp)
                                                )
                                                .padding(horizontal = 18.dp, vertical = 16.dp)
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        // Textos
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
                                                        fontSize = 12.sp,
                                                        color = Color.White.copy(alpha = 0.72f)
                                                )
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        // Badge de selección
                                        if (hasItems) {
                                                Box(
                                                        modifier =
                                                                Modifier.size(32.dp)
                                                                        .clip(CircleShape)
                                                                        .background(
                                                                                Color.White.copy(
                                                                                        alpha =
                                                                                                0.25f
                                                                                )
                                                                        ),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Text(
                                                                text = selectedCount.toString(),
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color.White
                                                        )
                                                }
                                                Spacer(Modifier.width(8.dp))
                                        }

                                        // Flecha animada
                                        Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription =
                                                        if (isExpanded) "Colapsar" else "Expandir",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp).rotate(arrowAngle)
                                        )
                                }
                        }

                        // ── CONTENIDO EXPANDIDO ─────────────────────────────────────────
                        AnimatedVisibility(
                                visible = isExpanded,
                                enter =
                                        fadeIn(tween(160)) +
                                                expandVertically(
                                                        animationSpec =
                                                                spring(
                                                                        dampingRatio =
                                                                                Spring.DampingRatioMediumBouncy,
                                                                        stiffness =
                                                                                Spring.StiffnessMediumLow
                                                                )
                                                ),
                                exit = fadeOut(tween(120)) + shrinkVertically(tween(200))
                        ) {
                                Column(
                                        modifier = Modifier.fillMaxWidth().background(dtBrightSnow)
                                ) {
                                        // Barra de acento inferior del header
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .height(3.dp)
                                                                .background(
                                                                        brush =
                                                                                Brush.horizontalGradient(
                                                                                        colors =
                                                                                                listOf(
                                                                                                        accentColor,
                                                                                                        accentColor
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.3f
                                                                                                                )
                                                                                                )
                                                                                )
                                                                )
                                        )

                                        // Grid de productos (reutiliza ProductGrid existente)
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
                                                onLongClickAddProduct = onLongClickAddProduct,
                                                accentColor = accentColor
                                        )

                                        // Pie de la tarjeta
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .height(2.dp)
                                                                .background(
                                                                        accentColor.copy(
                                                                                alpha = 0.18f
                                                                        )
                                                                )
                                        )
                                }
                        }
                }
        }
}
