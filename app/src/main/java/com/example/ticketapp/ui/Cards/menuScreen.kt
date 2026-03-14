import androidx.compose.runtime.Composable

@Composable
fun MenuScreen() {

    val menu = listOf(
        MenuItem(1,"Hamburguesa",R.drawable.burger,80.0),
        MenuItem(2,"Pizza",R.drawable.pizza,120.0),
        MenuItem(3,"Refresco",R.drawable.soda,25.0),
        MenuItem(4,"Papas",R.drawable.fries,50.0)
    )

    MenuGrid(
        items = menu,
        onItemClick = {
            println("Clicked ${it.name}")
        }
    )
}