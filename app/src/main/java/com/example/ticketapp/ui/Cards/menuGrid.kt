import androidx.compose.runtime.Composable

@Composable
fun MenuGrid(
    items: List<MenuItem>,
    onItemClick: (MenuItem) -> Unit
) {

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp)
    ) {

        items(items) { item ->
            MenuCard(
                item = item,
                onClick = onItemClick
            )
        }

    }
}