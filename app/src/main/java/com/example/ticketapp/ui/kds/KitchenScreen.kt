package com.example.ticketapp.ui.kds

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ticketapp.data.kds.KdsOrder
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

// ─── Elapsed time helpers ─────────────────────────────────────────────────────

/** Returns a human-readable elapsed time string and a color-coded urgency level */
private fun elapsedInfo(timestamp: Long): Pair<String, Color> {
    val elapsedMs = System.currentTimeMillis() - timestamp
    val minutes = (elapsedMs / 60_000).toInt()
    val seconds = ((elapsedMs % 60_000) / 1000).toInt()

    return when {
        minutes < 10 ->
                Pair(
                        "$minutes:${seconds.toString().padStart(2,'0')} min",
                        Color(0xFF4CAF50)
                ) // verde — en tiempo
        minutes < 15 ->
                Pair(
                        "$minutes:${seconds.toString().padStart(2,'0')} min",
                        Color(0xFFFF9800)
                ) // naranja — atención
        else -> Pair("${minutes}+ min", Color(0xFFF44336)) // rojo — urgente
    }
}

// ─── Screens ─────────────────────────────────────────────────────────────────

@Composable
fun KitchenScreen(viewModel: KitchenViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // A ticker that forces the elapsed-time labels to recompose every second
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            tick = System.currentTimeMillis()
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {

        // ── Left Panel: Pending Orders (2/3 width) ──────────────────────────
        Column(modifier = Modifier.weight(2f).fillMaxHeight().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                        text = "Pedidos por Hacer",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(12.dp))
                if (uiState.pendingOrders.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF44336)) {
                        Text(
                                text = " ${uiState.pendingOrders.size} ",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.pendingOrders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin pedidos pendientes ✅", color = Color.Gray, fontSize = 20.sp)
                }
            } else {
                LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 340.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.pendingOrders, key = { it.id }) { order ->
                        KdsOrderCard(
                                order = order,
                                isPending = true,
                                tick = tick,
                                onItemReady = { index ->
                                    viewModel.markItemAsReady(order.id, index)
                                },
                                onOrderCompleted = { viewModel.markOrderAsCompleted(order.id) }
                        )
                    }
                }
            }
        }

        // ── Right Panel: Completed Orders / History (1/3 width) ─────────────
        Column(
                modifier =
                        Modifier.weight(1f).fillMaxHeight().background(Color.White).padding(16.dp)
        ) {
            Text(
                    text = "Historial (Completados)",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
            )

            if (uiState.completedOrders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin historial", color = Color.Gray)
                }
            } else {
                LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.completedOrders, key = { it.id }) { order ->
                        KdsOrderCard(
                                order = order,
                                isPending = false,
                                tick = tick,
                                onItemReady = {},
                                onOrderCompleted = {}
                        )
                    }
                }
            }
        }
    }
}

// ─── Order Card ──────────────────────────────────────────────────────────────

@Composable
fun KdsOrderCard(
        order: KdsOrder,
        isPending: Boolean,
        tick: Long, // forces recomposition for live elapsed time
        onItemReady: (Int) -> Unit,
        onOrderCompleted: () -> Unit
) {
    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(order.timestamp))
    val allItemsReady = order.items.all { it.status == "READY" }

    // Live elapsed time updates whenever tick changes
    val (elapsedText, elapsedColor) =
            remember(tick) { if (isPending) elapsedInfo(order.timestamp) else Pair("", Color.Gray) }

    // Card border color pulses red when the order is over 10 min old
    val cardBorderColor by
            animateColorAsState(
                    targetValue =
                            if (isPending && elapsedColor == Color(0xFFF44336)) Color(0xFFF44336)
                            else Color.Transparent,
                    animationSpec = tween(600),
                    label = "borderAnim"
            )

    Card(
            elevation = if (isPending) 6.dp else 2.dp,
            shape = RoundedCornerShape(12.dp),
            backgroundColor = if (isPending) Color.White else Color(0xFFF0F0F0),
            border =
                    if (cardBorderColor != Color.Transparent)
                            androidx.compose.foundation.BorderStroke(2.dp, cardBorderColor)
                    else null,
            modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header ────────────────────────────────────────────────────
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = "Mesa ${order.tableNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.Black
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = timeString, color = Color.Gray, fontSize = 14.sp)
                    if (isPending && elapsedText.isNotEmpty()) {
                        Text(
                                text = "⏱ $elapsedText",
                                color = elapsedColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(text = "Mesero: ${order.waiterName}", color = Color.DarkGray, fontSize = 14.sp)

            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            // ── Items ─────────────────────────────────────────────────────
            order.items.forEachIndexed { index, item ->
                Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = "${item.quantity}x ${item.productName}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (item.status == "READY") Color.Gray else Color.Black
                        )
                        if (item.notes.isNotEmpty()) {
                            Text(
                                    text = "Notas: ${item.notes}",
                                    color = Color.Red,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (isPending) {
                        Button(
                                onClick = { onItemReady(index) },
                                colors =
                                        ButtonDefaults.buttonColors(
                                                backgroundColor =
                                                        if (item.status == "READY")
                                                                Color(0xFF4CAF50)
                                                        else Color(0xFFE3F2FD),
                                                contentColor =
                                                        if (item.status == "READY") Color.White
                                                        else Color(0xFF1976D2)
                                        ),
                                shape = RoundedCornerShape(8.dp)
                        ) { Text(if (item.status == "READY") "✓ LISTO" else "PREPARAR") }
                    }
                }
            }

            // ── Footer: Complete button ───────────────────────────────────
            if (isPending) {
                Spacer(Modifier.height(16.dp))
                Button(
                        onClick = onOrderCompleted,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors =
                                ButtonDefaults.buttonColors(
                                        backgroundColor =
                                                if (allItemsReady) Color(0xFF4CAF50)
                                                else Color(0xFFFF9800),
                                        contentColor = Color.White
                                ),
                        shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                            text = if (allItemsReady) "🍽 ENTREGAR PEDIDO" else "⚡ FORZAR ENTREGA",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
