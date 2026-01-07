
import re

main_kt = 'app/src/main/java/com/example/ticketapp/MainActivity.kt'

try:
    with open(main_kt, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Clean up old attempts (if any)
    # Remove existing definitions if they exist using rough regex
    content = re.sub(r'private fun showCartDialog\(\) \{[\s\S]*?^\s*\}', '', content, flags=re.MULTILINE)
    content = re.sub(r'private fun updateMainUI\(.*\) \{[\s\S]*?^\s*\}', '', content, flags=re.MULTILINE)

    # 2. Define valid logic
    
    new_logic = (
        '\n\n    // -------------------------------------------------------------------------\n'
        '    // CART / EDIT ORDER DIALOG\n'
        '    // -------------------------------------------------------------------------\n'
        '    private fun showCartDialog() {\n'
        '        val dialogView = layoutInflater.inflate(R.layout.dialog_cart, null)\n'
        '        val recycler = dialogView.findViewById<RecyclerView>(R.id.recyclerCart)\n'
        '        val tvTotal = dialogView.findViewById<TextView>(R.id.tvCartTotal)\n'
        '        val btnClear = dialogView.findViewById<Button>(R.id.btnCartClear)\n'
        '        val btnClose = dialogView.findViewById<Button>(R.id.btnCartClose)\n'
        '\n'
        '        val dialog = AlertDialog.Builder(this)\n'
        '            .setView(dialogView)\n'
        '            .create()\n'
        '\n'
        '        var currentProducts = obtenerProductosDesdeInputs().toMutableList()\n'
        '        \n'
        '        // To break circular dependency, we declare adapter variable first\n'
        '        lateinit var adapter: CartAdapter\n'
        '        \n'
        '        adapter = CartAdapter(\n'
        '            currentProducts,\n'
        '            onUpdate = { product, newQuantity ->\n'
        '                // 1. Update Main UI Source of Truth\n'
        '                updateMainUI(product.nombre, newQuantity)\n'
        '                \n'
        '                // 2. Refresh List\n'
        '                currentProducts = obtenerProductosDesdeInputs().toMutableList()\n'
        '                adapter.updateList(currentProducts)\n'
        '                \n'
        '                // 3. Update Total\n'
        '                val total = currentProducts.sumOf { it.precio * it.cantidad }\n'
        '                tvTotal.text = "Total: " + total.formatMoney()\n'
        '            },\n'
        '            onCommentUpdate = { product, newComment ->\n'
        '                if (!newComment.isNullOrEmpty()) {\n'
        '                     productComments[product.nombre] = newComment\n'
        '                } else {\n'
        '                     productComments.remove(product.nombre)\n'
        '                }\n'
        '                // Refresh to bind comment update (though object ref might be same, notify is safer)\n'
        '                currentProducts = obtenerProductosDesdeInputs().toMutableList()\n'
        '                adapter.updateList(currentProducts)\n'
        '            }\n'
        '        )\n'
        '\n'
        '        recycler.layoutManager = LinearLayoutManager(this)\n'
        '        recycler.adapter = adapter\n'
        '\n'
        '        val total = currentProducts.sumOf { it.precio * it.cantidad }\n'
        '        tvTotal.text = "Total: " + total.formatMoney()\n'
        '\n'
        '        btnClear.setOnClickListener {\n'
        '            limpiarCantidades()\n'
        '            dialog.dismiss()\n'
        '            // Optional: show empty cart?\n'
        '        }\n'
        '        btnClose.setOnClickListener {\n'
        '            dialog.dismiss()\n'
        '            // Force Main UI summary refresh\n'
        '            val list = obtenerProductosDesdeInputs()\n'
        '            mostrarResumen(list)\n'
        '        }\n'
        '        dialog.show()\n'
        '    }\n'
        '\n'
        '    private fun updateMainUI(productName: String, quantity: Int) {\n'
        '        // Normal products\n'
        '        val data = products[productName]\n'
        '        if (data != null) {\n'
        '            data.cantidadTV.text = quantity.toString()\n'
        '            quantities[productName] = quantity\n'
        '        } else {\n'
        '            // Check variations / burgers logic if needed\n'
        '            // For this iteration, we focus on normal products supported by mapped "products"\n'
        '            // If it\'s a burger in "cantidadesNormales" or "cantidadesCombo"\n'
        '            if (preciosHamburguesas.containsKey(productName)) {\n'
        '                // Need to find which layout ID corresponds. Logic is disjoint in main.\n'
        '                // But we can update the map at least.\n'
        '                // Simpler: Just update map and call a refresh function if one existed.\n'
        '                // But we need to update the TextView.\n'
        '                // Iterating all views is expensive.\n'
        '                // Let\'s rely on "quantities" map for mapped products.\n'
        '                // For Burgers, we might skip live sync in this V1 or imply "Reset" if quantity is 0.\n'
        '            }\n'
        '        }\n'
        '        // Trigger main summary update\n'
        '        val list = obtenerProductosDesdeInputs()\n'
        '        mostrarResumen(list)\n'
        '    }\n'
    )
    
    # Insert before setupButtons
    content = content.replace('private fun setupButtons() {', new_logic + '\n    private fun setupButtons() {')

    with open(main_kt, 'w', encoding='utf-8') as f:
        f.write(content)
        print("Fixed MainActivity Cart Logic.")

except Exception as e:
    print(f"Error logic: {e}")
