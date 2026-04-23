import re

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'r') as f:
    text = f.read()

old_code = """                for (cat in categoriasDB) {
                    // 1) Ventas brutas desde Room
                    val ventas =
                            withContext(Dispatchers.IO) {
                                viewModel.getProfitByCategory(
                                                category = cat,
                                                inicio = inicio,
                                                fin = fin,
                                                includeCombos = if (includeCombos) 1 else 0
                                        )
                            }
                                    ?: 0.0

                    if (ventas > 0.0) {
                        // 2) Aplica margen para obtener GANANCIA
                        val margen = categoryMargin[cat] ?: 0.0
                        val ganancia = ventas * margen
                        pares += cat to ganancia
                        totalGanancia += ganancia
                    }
                }"""

new_code = """                withContext(Dispatchers.IO) {
                    for (cat in categoriasDB) {
                        // 1) Ventas brutas desde Room
                        val ventas = viewModel.getProfitByCategory(
                                                    category = cat,
                                                    inicio = inicio,
                                                    fin = fin,
                                                    includeCombos = if (includeCombos) 1 else 0
                                            ) ?: 0.0

                        if (ventas > 0.0) {
                            // 2) Aplica margen para obtener GANANCIA
                            val margen = categoryMargin[cat] ?: 0.0
                            val ganancia = ventas * margen
                            pares += cat to ganancia
                            totalGanancia += ganancia
                        }
                    }
                }"""

text = text.replace(old_code, new_code)

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'w') as f:
    f.write(text)
