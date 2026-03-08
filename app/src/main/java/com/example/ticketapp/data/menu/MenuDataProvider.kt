package com.example.ticketapp.data.menu

object MenuDataProvider {
    // The variations array from the original MainActivity
    val standardGuisados =
            listOf(
                    "Chorizo",
                    "Huevo",
                    "Mole Verde",
                    "Huitlacoche",
                    "Bisteck",
                    "Pollo",
                    "Champiñones",
                    "Tinga",
                    "Picadillo",
                    "Papa con chorizo",
                    "Chicharrón Prensado",
                    "Queso",
                    "Molleja",
                    "panza"
            )

    val pambazosGuisados =
            listOf(
                    "Chorizo",
                    "Mole Verde",
                    "Bisteck",
                    "Huevo",
                    "Pollo",
                    "Champiñones",
                    "Huitlacoche",
                    "Tinga",
                    "Picadillo",
                    "Papa con chorizo",
                    "Chicharrón Prensado",
                    "Queso",
                    "Molleja",
                    "panza"
            )

    val tacosOpciones =
            listOf(
                    "Costilla",
                    "Arrachera",
                    "Cecina",
                    "Chorizo Argentino",
                    "chistorra",
                    "pollo",
                    "bisteck"
            )
    val tacosQuesoOpciones =
            listOf("Costilla", "Arrachera", "Cecina", "Chorizo Argentino", "chistorra")

    val alitasSalsas = listOf("BBQ", "BBQ Hot", "Búfalo", "Mango-Habanero", "Macha")
    val pozoleOpciones = listOf("pollo", "puerco", "combinado")

    fun getMenuCategories(): List<MenuCategory> {
        return listOf(
                MenuCategory(
                        name = "Platillos Principales",
                        items =
                                listOf(
                                        MenuItem(
                                                "Quesadillas",
                                                30.0,
                                                VariationType.SINGLE_SELECTION,
                                                standardGuisados
                                        ),
                                        MenuItem(
                                                "Quesadilla/Queso",
                                                30.0,
                                                VariationType.SINGLE_SELECTION,
                                                standardGuisados
                                        ),
                                        MenuItem(
                                                "Pozole Grande",
                                                110.0,
                                                VariationType.SINGLE_SELECTION,
                                                pozoleOpciones
                                        ),
                                        MenuItem(
                                                "Pozole Chico",
                                                90.0,
                                                VariationType.SINGLE_SELECTION,
                                                pozoleOpciones
                                        ),
                                        MenuItem(
                                                "Tostadas",
                                                35.0,
                                                VariationType.SINGLE_SELECTION,
                                                standardGuisados
                                        ),
                                        MenuItem(
                                                "Volcanes",
                                                60.0,
                                                VariationType.SINGLE_SELECTION,
                                                standardGuisados
                                        ),
                                        MenuItem(
                                                "Volcan Queso",
                                                72.0,
                                                VariationType.SINGLE_SELECTION,
                                                standardGuisados
                                        ),
                                        MenuItem(
                                                "Guisado Extra",
                                                72.0,
                                                VariationType.SINGLE_SELECTION,
                                                standardGuisados
                                        )
                                )
                ),
                MenuCategory(
                        name = "Pambazos",
                        items =
                                listOf(
                                        MenuItem(
                                                "Pambazos Naturales",
                                                35.0,
                                                VariationType.SINGLE_SELECTION,
                                                pambazosGuisados
                                        ),
                                        MenuItem(
                                                "Pambazos Naturales Combinados",
                                                42.0,
                                                VariationType.MULTIPLE_SELECTION,
                                                pambazosGuisados
                                        ),
                                        MenuItem(
                                                "Pambazos Naturales Combinados con Queso",
                                                54.0,
                                                VariationType.MULTIPLE_SELECTION,
                                                pambazosGuisados
                                        ),
                                        MenuItem(
                                                "Pambazos Naturales Extra",
                                                47.0,
                                                VariationType.SINGLE_SELECTION,
                                                pambazosGuisados
                                        ),
                                        MenuItem(
                                                "Pambazos Adobados",
                                                40.0,
                                                VariationType.SINGLE_SELECTION,
                                                pambazosGuisados
                                        ),
                                        MenuItem(
                                                "Pambazos Adobados Combinados",
                                                47.0,
                                                VariationType.MULTIPLE_SELECTION,
                                                pambazosGuisados
                                        ),
                                        MenuItem(
                                                "Pambazos Adobados Combinados con Queso",
                                                59.0,
                                                VariationType.MULTIPLE_SELECTION,
                                                pambazosGuisados
                                        ),
                                        MenuItem(
                                                "Pambazos Adobados Extra",
                                                52.0,
                                                VariationType.SINGLE_SELECTION,
                                                pambazosGuisados
                                        )
                                )
                ),
                MenuCategory(
                        name = "Guajoloyets",
                        items =
                                listOf(
                                        MenuItem("Guajoloyets Naturales", 60.0, VariationType.NONE),
                                        MenuItem(
                                                "Guajoloyets Naturales Extra",
                                                72.0,
                                                VariationType.SINGLE_SELECTION,
                                                standardGuisados
                                        ),
                                        MenuItem("Guajoloyets Adobados", 65.0, VariationType.NONE),
                                        MenuItem(
                                                "Guajoloyets Adobados Extra",
                                                77.0,
                                                VariationType.SINGLE_SELECTION,
                                                standardGuisados
                                        )
                                )
                ),
                MenuCategory(
                        name = "Entradas y Extras",
                        items =
                                listOf(
                                        MenuItem("Chalupas", 5.0, VariationType.NONE),
                                        MenuItem("Alones", 25.0, VariationType.NONE),
                                        MenuItem("Mollejas", 25.0, VariationType.NONE),
                                        MenuItem("Higados", 22.0, VariationType.NONE),
                                        MenuItem("Patitas", 22.0, VariationType.NONE),
                                        MenuItem("Huevos", 20.0, VariationType.NONE),
                                        MenuItem(
                                                "Orden de Papas Sencillas",
                                                50.0,
                                                VariationType.NONE
                                        ),
                                        MenuItem(
                                                "Orden de Papas Queso y Tocino",
                                                65.0,
                                                VariationType.NONE
                                        ),
                                        MenuItem("Combo", 30.0, VariationType.NONE)
                                )
                ),
                MenuCategory(
                        name = "Hamburguesas",
                        items =
                                listOf(
                                        MenuItem("Hamburguesa Clasica", 65.0, isBurger = true),
                                        MenuItem("Hamburguesa Hawaiana", 80.0, isBurger = true),
                                        MenuItem("Hamburguesa Pollo", 70.0, isBurger = true),
                                        MenuItem("Hamburguesa Champinones", 90.0, isBurger = true),
                                        MenuItem("Hamburguesa Arrachera", 105.0, isBurger = true),
                                        MenuItem("Hamburguesa Maggy", 100.0, isBurger = true),
                                        MenuItem("Hamburguesa Doble", 110.0, isBurger = true)
                                )
                ),
                MenuCategory(
                        name = "Tacos",
                        items =
                                listOf(
                                        MenuItem(
                                                "Taco (c/u)",
                                                25.0,
                                                VariationType.SINGLE_SELECTION,
                                                tacosOpciones
                                        ),
                                        MenuItem(
                                                "Taco con Queso (c/u)",
                                                30.0,
                                                VariationType.SINGLE_SELECTION,
                                                tacosQuesoOpciones
                                        )
                                )
                ),
                MenuCategory(
                        name = "Alitas",
                        items =
                                listOf(
                                        MenuItem(
                                                "Alitas 6 pzas",
                                                65.0,
                                                VariationType.SINGLE_SELECTION,
                                                alitasSalsas
                                        ),
                                        MenuItem(
                                                "Alitas 10 pzas",
                                                100.0,
                                                VariationType.SINGLE_SELECTION,
                                                alitasSalsas
                                        ),
                                        MenuItem(
                                                "Alitas 15 pzas",
                                                140.0,
                                                VariationType.SINGLE_SELECTION,
                                                alitasSalsas
                                        )
                                )
                ),
                MenuCategory(
                        name = "Bebidas",
                        items =
                                listOf(
                                        MenuItem("Refrescos", 26.0, VariationType.TEXT_INPUT),
                                        MenuItem("Cafe", 22.0, VariationType.NONE),
                                        MenuItem("Aguas de Sabor", 25.0, VariationType.TEXT_INPUT),
                                        MenuItem("Agua Natural", 20.0, VariationType.NONE),
                                        MenuItem("Agua para Te", 20.0, VariationType.NONE)
                                )
                )
        )
    }
}
