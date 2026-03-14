package com.example.ticketapp.ui.Cards

// ─────────────────────────────────────────────────────────────────────────────
// MenuCard — Composable de tarjeta de producto para el overlay de categoría.
// Usa la paleta de colors.xml: CarbonBlack, BrigthSnow, Grayish_lime, Grey.
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ticketapp.R
import com.example.ticketapp.ui.Cards.MenuItem

/**
 * MenuCard — muestra un platillo con nombre, precio y controles +/−.
 *
 * @param item           Datos del platillo.
 * @param quantity       Cantidad actual en el pedido.
 * @param accentColor    Color de acento del tile de categoría padre.
 * @param onAdd          Callback al pulsar "+".
 * @param onRemove       Callback al pulsar "−".
 * @param onLongClickAdd Callback para long-press en "+" (ej. inventario).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuCard(
    item: MenuItem,
    quantity: Int,
    accentColor: Color,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onLongClickAdd: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val carbonBlack  = colorResource(R.color.CarbonBlack)
    val brightSnow   = colorResource(R.color.BrigthSnow)
    val charcoalCool = colorResource(R.color.CharcoalCool)
    val grayishLime  = colorResource(R.color.Grayish_lime)
    val grey         = colorResource(R.color.Grey)

    val hasItems = quantity > 0

    val cardBg by animateColorAsState(
        targetValue   = if (hasItems) grayishLime.copy(alpha = 0.18f) else brightSnow,
        animationSpec = tween(300),
        label         = "cardBg"
    )

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasItems) 4.dp else 1.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Nombre y precio ───────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Text(
                    text      = item.name,
                    fontSize  = 14.sp,
                    fontWeight= FontWeight.SemiBold,
                    color     = carbonBlack,
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis,
                    modifier  = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text      = "$${String.format("%.0f", item.price)}",
                    fontSize  = 13.sp,
                    fontWeight= FontWeight.Bold,
                    color     = charcoalCool
                )
            }

            // ── Controles +/− ─────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Botón −
                FilledIconButton(
                    onClick  = onRemove,
                    modifier = Modifier.size(36.dp),
                    colors   = IconButtonDefaults.filledIconButtonColors(
                        containerColor = grey,
                        contentColor   = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

                // Contador circular
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (hasItems) grayishLime else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = quantity.toString(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 14.sp,
                        color      = if (hasItems) carbonBlack else charcoalCool,
                        textAlign  = TextAlign.Center
                    )
                }

                // Botón + con long-click
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor)
                        .then(
                            if (onLongClickAdd != null)
                                Modifier.combinedClickable(
                                    onClick     = onAdd,
                                    onLongClick = onLongClickAdd
                                )
                            else
                                Modifier.clickable(onClick = onAdd)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }
        }
    }
}
