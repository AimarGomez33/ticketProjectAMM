import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ticketapp.ui.Cards.MenuCard
import com.example.ticketapp.ui.Cards.MenuItem
import androidx.compose.foundation.layout.fillMaxWidth


@Composable
fun CompactCard(card: CardItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = card.src,
                contentDescription = card.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = card.title,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = card.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ExpandedCard(card: CardItem, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.9f) // md:max-h-[90%]
            .padding(vertical = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = card.src,
                    contentDescription = card.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = card.title,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = card.description,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Botón Call To Action (Equivalente al <a> tag)
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(card.ctaLink))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)), // bg-green-500
                        shape = CircleShape
                    ) {
                        Text(text = card.ctaText, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Contenido Scrolleable (Equivalente a overflow-auto)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 40.dp)
                ) {
                    Text(
                        text = card.content,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            // Icono de Cerrar (Equivalente a <CloseIcon />)
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.White.copy(alpha = 0.8f), CircleShape)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}