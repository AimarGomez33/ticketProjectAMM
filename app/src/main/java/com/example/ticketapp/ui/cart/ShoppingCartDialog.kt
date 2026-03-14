package com.example.ticketapp.ui.cart

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.core.Dialog
import com.composables.core.DialogPanel
import com.composables.core.Scrim
import com.composables.core.rememberDialogState
import com.example.ticketapp.MainActivity.Producto
import com.example.ticketapp.R
import kotlinx.coroutines.delay

@Composable
fun ShoppingCartDialog(
    items: List<Producto>,
    visible: Boolean,
    onDismiss: () -> Unit,
    onCheckout: () -> Unit,
    onRemoveItem: ((Producto) -> Unit)? = null
) {
    val state = rememberDialogState(initiallyVisible = visible)

    LaunchedEffect(visible) {
        state.visible = visible
    }

    LaunchedEffect(state.visible) {
        if (!state.visible) {
            delay(300)
            onDismiss()
        }
    }

    Dialog(state = state) {
        Scrim(
            enter = fadeIn(),
            exit = fadeOut(),
            scrimColor = Color.Black.copy(alpha = 0.5f)
        )
        val dialogShape = MaterialTheme.shapes.extraLarge
        DialogPanel(
            modifier = Modifier
                .systemBarsPadding()
                .widthIn(min = 280.dp, max = 520.dp)
                .shadow(8.dp, dialogShape)
                .background(MaterialTheme.colorScheme.surface, dialogShape)
                .padding(8.dp),
            enter = slideInVertically { it / 2 } + fadeIn(tween(durationMillis = 250)),
            exit = slideOutVertically { it } + fadeOut(tween(durationMillis = 150))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { state.visible = false }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar carrito",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "Carrito de Compra",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                LazyColumn(
                    Modifier
                        .height(420.dp)
                        .fillMaxWidth()
                ) {
                    if (items.isEmpty()) {
                        item {
                            Text(
                                text = "No hay productos en el carrito",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        itemsIndexed(items) { i, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Placeholder for product icon (removed images)
                                Column(
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            MaterialTheme.shapes.small
                                        )
                                        .background(colorResource(id = R.color.app_background))
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "🍽️",
                                        fontSize = 32.sp
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = item.nombre,
                                                modifier = Modifier.weight(1f),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "$${String.format("%.2f", item.precio * item.cantidad)}",
                                                maxLines = 1,
                                                modifier = Modifier.padding(start = 16.dp),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 15.sp
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (item.esCombo) "Combo" else "Individual",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!item.comment.isNullOrBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "📝 ${item.comment}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            text = "Cantidad ${item.cantidad}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (onRemoveItem != null) {
                                            IconButton(
                                                onClick = { onRemoveItem(item) },
                                                modifier = Modifier
                                                    .offset(x = 6.dp, y = 2.dp)
                                                    .size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "Eliminar del carrito",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val subtotal = items.sumOf { it.precio * it.cantidad }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subtotal",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "$${String.format("%.2f", subtotal)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            state.visible = false
                            onCheckout()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = items.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.app_primary)
                        )
                    ) {
                        Text("Generar Compra")
                    }
                }
            }
        }
    }
}
