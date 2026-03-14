package com.example.ticketapp.ui.Cards

// ─────────────────────────────────────────────────────────────────────────────
// MenuCardGrid — Grid 2-columnas de MenuCard dentro del overlay de categoría.
// Se usa desde CategoryFullScreenGrid como reemplazo de ProductGrid.
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ticketapp.data.menu.MenuItem as DomainMenuItem

/**
 * Muestra los platillos de una categoría en un grid de 2 columnas
 * usando [MenuCard] para cada tarjeta.
 *
 * @param items           Lista de platillos del dominio.
 * @param quantities      Mapa nombre → cantidad en el pedido actual.
 * @param accentColor     Color de acento heredado del tile de categoría.
 * @param onAddProduct    Callback al pulsar "+".
 * @param onRemoveProduct Callback al pulsar "−".
 * @param onLongClickAdd  Long-press en "+" (ej. inventario de refrescos).
 */
@Composable
fun MenuCardGrid(
    items: List<DomainMenuItem>,
    quantities: Map<String, Int>,
    accentColor: Color,
    onAddProduct: (DomainMenuItem) -> Unit,
    onRemoveProduct: (DomainMenuItem) -> Unit,
    onLongClickAdd: ((DomainMenuItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Partimos los ítems en filas de 2
    val rows = items.chunked(2)

    Column(
        modifier            = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { domainItem ->
                    val card = domainItem.toCardItem()
                    MenuCard(
                        item          = card,
                        quantity      = quantities[domainItem.name] ?: 0,
                        accentColor   = accentColor,
                        onAdd         = { onAddProduct(domainItem) },
                        onRemove      = { onRemoveProduct(domainItem) },
                        onLongClickAdd = onLongClickAdd?.let { cb -> { cb(domainItem) } },
                        modifier      = Modifier.weight(1f)
                    )
                }
                // Relleno si la fila tiene un solo ítem
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
