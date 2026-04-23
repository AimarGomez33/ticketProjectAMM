import re

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'r') as f:
    text = f.read()

# Refactor calculateAndShowCategoryGains to do parsing in a single IO block
old_calculate = """    private fun calculateAndShowCategoryGains(
            startDate: Long,
            endDate: Long,
            includeCombos: Boolean = true
    ) {
        lifecycleScope.launch {
            val includeCombosFlag = if (includeCombos) 1 else 0
            val pares = mutableListOf<Pair<String, Double>>() // (categoria, ganancia)
            var totalGanancia = 0.0

            for (cat in categoriasDB) {
                val ventas =
                        withContext(Dispatchers.IO) {
                            viewModel.getProfitByCategory(
                                            category = cat,
                                            inicio = startDate,
                                            fin = endDate,
                                            includeCombos = includeCombosFlag
                                    )
                        }
                                ?: 0.0

                if (ventas > 0.0) {
                    val margen = categoryMargin[cat] ?: 0.0
                    val ganancia = ventas * margen
                    pares += cat to ganancia
                    totalGanancia += ganancia
                }
            }"""

new_calculate = """    private fun calculateAndShowCategoryGains(
            startDate: Long,
            endDate: Long,
            includeCombos: Boolean = true
    ) {
        lifecycleScope.launch {
            val includeCombosFlag = if (includeCombos) 1 else 0
            val pares = mutableListOf<Pair<String, Double>>() // (categoria, ganancia)
            var totalGanancia = 0.0

            withContext(Dispatchers.IO) {
                for (cat in categoriasDB) {
                    val ventas = viewModel.getProfitByCategory(
                                                category = cat,
                                                inicio = startDate,
                                                fin = endDate,
                                                includeCombos = includeCombosFlag
                                        ) ?: 0.0

                    if (ventas > 0.0) {
                        val margen = categoryMargin[cat] ?: 0.0
                        val ganancia = ventas * margen
                        pares += cat to ganancia
                        totalGanancia += ganancia
                    }
                }
            }"""

text = text.replace(old_calculate, new_calculate)

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'w') as f:
    f.write(text)
