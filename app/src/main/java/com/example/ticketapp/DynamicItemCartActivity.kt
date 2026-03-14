package com.example.ticketapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

data class CartItem(
    val id: String = System.currentTimeMillis().toString(),
    val quantity: Int = 1,
    val comment: String = ""
)

class DynamicItemCartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val productName = intent.getStringExtra("product_name") ?: "Producto"
        val productType = intent.getStringExtra("product_type") ?: "item"
        
        setContent {
            DynamicItemCartScreen(
                productName = productName,
                productType = productType,
                onBack = { finish() },
                onAddToCart = { items ->
                    // Aquí se agregarían los items al carrito real
                    finish()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicItemCartScreen(
    productName: String,
    productType: String,
    onBack: () -> Unit,
    onAddToCart: (List<CartItem>) -> Unit
) {
    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var currentQuantity by remember { mutableStateOf("1") }
    var currentComment by remember { mutableStateOf("") }
    var totalPrice by remember { mutableStateOf(0.0) }
    
    val pricePerUnit = 2.5 // Precio ejemplo
    
    val appCarbonBlack = Color(0xFF222823)
    val appCharcoalCool = Color(0xFF575A5E)
    val appGrayishLime = Color(0xFFA7C1A8)
    val appBrightSnow = Color(0xFFF4F7F5)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = appBrightSnow
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Agregar $productName",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Múltiples cantidades",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appCarbonBlack
                )
            )

            // Contenido scrollable
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Card de entrada: cantidad y comentario
                    InputCard(
                        currentQuantity = currentQuantity,
                        onQuantityChange = { currentQuantity = it },
                        currentComment = currentComment,
                        onCommentChange = { currentComment = it },
                        appGrayishLime = appGrayishLime,
                        appCharcoalCool = appCharcoalCool,
                        onAddItem = {
                            if (currentQuantity.toIntOrNull() != null && currentQuantity.toInt() > 0) {
                                cartItems = cartItems + CartItem(
                                    quantity = currentQuantity.toInt(),
                                    comment = currentComment
                                )
                                currentQuantity = "1"
                                currentComment = ""
                            }
                        }
                    )

                    // Lista de items agregados
                    if (cartItems.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Items Agregados (${cartItems.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = appCarbonBlack,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 300.dp)
                            ) {
                                items(cartItems) { item ->
                                    CartItemCard(
                                        item = item,
                                        index = cartItems.indexOf(item),
                                        appBrightSnow = appBrightSnow,
                                        appCharcoalCool = appCharcoalCool,
                                        appGrayishLime = appGrayishLime,
                                        onRemove = {
                                            cartItems = cartItems.filterNot { it.id == item.id }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Resumen de totales
                    if (cartItems.isNotEmpty()) {
                        SummaryCard(
                            itemCount = cartItems.size,
                            totalQuantity = cartItems.sumOf { it.quantity },
                            totalPrice = cartItems.sumOf { it.quantity } * pricePerUnit,
                            appCharcoalCool = appCharcoalCool,
                            appBrightSnow = appBrightSnow
                        )
                    }
                }
            }

            // Footer con botones de acción
            if (cartItems.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, appCharcoalCool.copy(alpha = 0.2f)),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, appCharcoalCool)
                        ) {
                            Text("Cancelar", color = appCharcoalCool, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = { onAddToCart(cartItems) },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = appGrayishLime
                            )
                        ) {
                            Text(
                                "Agregar al Carrito",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InputCard(
    currentQuantity: String,
    onQuantityChange: (String) -> Unit,
    currentComment: String,
    onCommentChange: (String) -> Unit,
    appGrayishLime: Color,
    appCharcoalCool: Color,
    onAddItem: () -> Unit
) {
    val appCarbonBlack = Color(0xFF222823)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, appCharcoalCool.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
        color = Color.White,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Detalles de Unidad",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = appCarbonBlack
            )

            // Input de cantidad
            OutlinedTextField(
                value = currentQuantity,
                onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) onQuantityChange(it) },
                label = { Text("Cantidad") },
                placeholder = { Text("Ej: 1, 5, 10...") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = appGrayishLime,
                    unfocusedBorderColor = appCharcoalCool.copy(alpha = 0.2f)
                )
            )

            // Input de comentario
            OutlinedTextField(
                value = currentComment,
                onValueChange = { if (it.length <= 100) onCommentChange(it) },
                label = { Text("Comentario") },
                placeholder = { Text("Ej: Sin cebolla, salsa al lado...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                minLines = 2,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = appGrayishLime,
                    unfocusedBorderColor = appCharcoalCool.copy(alpha = 0.2f)
                )
            )

            // Botón agregar
            Button(
                onClick = onAddItem,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = appGrayishLime),
                enabled = currentQuantity.isNotEmpty() && currentQuantity.toIntOrNull() != null && currentQuantity.toInt() > 0
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp), tint = Color.White)
                    Text("Agregar a Lista", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    index: Int,
    appBrightSnow: Color,
    appCharcoalCool: Color,
    appGrayishLime: Color,
    onRemove: () -> Unit
) {
    val appCarbonBlack = Color(0xFF222823)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, appGrayishLime.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
        color = appBrightSnow,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(24.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = appGrayishLime.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${index + 1}",
                            modifier = Modifier.fillMaxSize(),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = appCharcoalCool
                        )
                    }
                    
                    Text(
                        text = "x${item.quantity}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = appCarbonBlack,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                if (item.comment.isNotEmpty()) {
                    Text(
                        text = item.comment,
                        fontSize = 12.sp,
                        color = appCharcoalCool,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    itemCount: Int,
    totalQuantity: Int,
    totalPrice: Double,
    appCharcoalCool: Color,
    appBrightSnow: Color
) {
    val appCarbonBlack = Color(0xFF222823)
    val appGrayishLime = Color(0xFFA7C1A8)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, appGrayishLime.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        color = appBrightSnow,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Resumen",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = appCarbonBlack
            )

            Divider(color = appCharcoalCool.copy(alpha = 0.1f), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Items agregados:", fontSize = 12.sp, color = appCharcoalCool)
                Text("$itemCount", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = appCarbonBlack)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Cantidad total:", fontSize = 12.sp, color = appCharcoalCool)
                Text("$totalQuantity unidades", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = appCarbonBlack)
            }

            Divider(color = appCharcoalCool.copy(alpha = 0.1f), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total estimado:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appCarbonBlack)
                Text("$${String.format("%.2f", totalPrice)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appGrayishLime)
            }
        }
    }
}
