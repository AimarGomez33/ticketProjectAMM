
import re

main_xml = 'app/src/main/res/layout/activity_main.xml'
main_kt = 'app/src/main/java/com/example/ticketapp/MainActivity.kt'

# 1. Update Layout
try:
    with open(main_xml, 'r', encoding='utf-8') as f:
        xml_content = f.read()
    
    # Locate btnImprimir. It's usually a MaterialButton in a LL or RL.
    # Looking for: <com.google.android.material.button.MaterialButton android:id="@+id/btnImprimir"
    # We want to add the Edit button BEFORE it.
    
    if 'android:id="@+id/btnEditOrder"' not in xml_content:
        # We need a robust target. Replaced by button code.
        # Target: btnImprimir definition validation.
        # It's likely inside a layout.
        
        btn_print_regex = r'(<com\.google\.android\.material\.button\.MaterialButton\s+android:id="\@\+id/btnImprimir")'
        
        # New Button Code
        btn_edit = (
            '\n                    <com.google.android.material.button.MaterialButton\n'
            '                        android:id="@+id/btnEditOrder"\n'
            '                        android:layout_width="0dp"\n'
            '                        android:layout_height="60dp"\n'
            '                        android:layout_weight="1"\n'
            '                        android:layout_marginEnd="8dp"\n'
            '                        android:text="Editar Pedido"\n'
            '                        android:textColor="@color/Grayish_lime"\n'
            '                        android:textSize="18sp"\n'
            '                        app:backgroundTint="@color/Grayish_green"\n'
            '                        app:cornerRadius="12dp"\n'
            '                        app:icon="@drawable/ic_comment"\n' 
            '                        app:iconGravity="textStart" />\n'
        )
        # Note: Icon can be ic_comment or standard edit icon. I'll reuse ic_comment or null.
        # But wait, btnImprimir might be "match_parent" or specific weight.
        # If it's in a horizontal LL, we need to share weight.
        # Let's inspect the surrounding layout of btnImprimir first?
        # Creating a blind replacement might break layout. 
        # But using regex replacement allows checking context.
        pass # Defer layout update until I see grep result.

except Exception as e:
    print(f"Error reading xml: {e}")

# 2. Update MainActivity Logic
try:
    with open(main_kt, 'r', encoding='utf-8') as f:
        kt_content = f.read()

    # Add showCartDialog function
    if 'private fun showCartDialog()' not in kt_content:
        # Add imports
        if 'import androidx.recyclerview.widget.LinearLayoutManager' not in kt_content:
            kt_content = kt_content.replace(
                'import androidx.recyclerview.widget.RecyclerView', 
                'import androidx.recyclerview.widget.RecyclerView\nimport androidx.recyclerview.widget.LinearLayoutManager'
            )
        
        cart_logic = (
            '\n    private fun showCartDialog() {\n'
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
            '        // Get current products\n'
            '        val currentProducts = obtenerProductosDesdeInputs().toMutableList()\n'
            '\n'
            '        val adapter = CartAdapter(currentProducts, \n'
            '            onUpdate = { product, newQuantity ->\n'
            '                // Update quantities map\n'
            '                if (productVariations.containsKey(product.nombre)) {\n'
            '                    // Complex logic for variations unimplemented in quick edit? \n'
            '                    // For now, simpler handling: variations usually stored in selectedVariations list.\n'
            '                    // Updating quantity of a variation means removing/adding to that list?\n'
            '                    // This is tricky. Let\'s stick to normal products first or handle variations carefully.\n'
            '                    // If quantity becomes 0, remove from selectedVariations.\n'
            '                    // If variations items are unique?\n'
            '                } else {\n'
            '                    quantities[product.nombre] = newQuantity\n'
            '                }\n'
            '                // Handle Variations specifically if needed\n'
            '                // Logic refactor: obtenerProductosDesdeInputs creates NEW objects.\n'
            '                // We need to write back to Source of Truth (`quantities` and `selectedVariations`).\n'
            '                \n'
            '                // Simple implementation for normal products:\n'
            '                if (!product.esCombo && !productVariations.containsKey(product.nombre)) {\n'
            '                     quantities[product.nombre] = newQuantity\n'
            '                }\n'
            '                // Re-calculate list\n'
            '                val updated = obtenerProductosDesdeInputs()\n'
            '                // Update Adapter\n'
            '                // (This requires casting adapter or just setting data)\n'
            '                // But wait, we need to refresh the specific item or list.\n'
            '                // For simplicity, let\'s refresh UI textviews in background or on dismiss?\n'
            '                // We need to refresh Main UI Textviews immediately so "obtenerProductos" works next time? \n'
            '                // Actually "obtenerProductos" reads from "quantities" map. \n'
            '                // And "quantities" map drives the Main UI TextViews? No, logic is reverse usually.\n'
            '                // setupProductViews sets listeners that update "quantities" AND TextView.\n'
            '                // If we update "quantities" here, we must also update Main UI TextViews.\n'
            '                updateMainUI(product.nombre, newQuantity)\n'
            '                \n'
            '                // Recalculate for adapter\n'
            '                val newList = obtenerProductosDesdeInputs()\n'
            '                // We can\'t easily access the adapter instance inside lambda unless declared before.\n'
            '                // Using a refresh helper.\n'
            '            },\n'
            '            onCommentUpdate = { product, newComment ->\n'
            '                if (newComment != null) {\n'
            '                     productComments[product.nombre] = newComment\n'
            '                } else {\n'
            '                     productComments.remove(product.nombre)\n'
            '                }\n'
            '            }\n'
            '        )\n'
            '        // We need a wrapper to handle adapter updates\n'
            '        // Let\'s simplify: Just allow editing Quantities of "Normal" products.\n'
            '        // For variations, maybe just Delete?\n'
            '\n'
            '        recycler.layoutManager = LinearLayoutManager(this)\n'
            '        recycler.adapter = adapter\n'
            '\n'
            '        // Initialization of Total\n'
            '        val total = currentProducts.sumOf { it.precio * it.cantidad }\n'
            '        tvTotal.text = "Total: $$total"\n'
            '\n'
            '        btnClear.setOnClickListener {\n'
            '            limpiarCantidades()\n'
            '            dialog.dismiss()\n'
            '        }\n'
            '        btnClose.setOnClickListener {\n'
            '            dialog.dismiss()\n'
            '        }\n'
            '        dialog.show()\n'
            '    }\n'
            '\n'
            '    private fun updateMainUI(productName: String, quantity: Int) {\n'
            '        // Find the ProductData and update TextView\n'
            '        val data = products[productName]\n'
            '        data?.cantidadTV?.text = quantity.toString()\n'
            '        // Update map\n'
            '        quantities[productName] = quantity\n'
            '        // Also update summary?\n'
            '        val list = obtenerProductosDesdeInputs()\n'
            '        mostrarResumen(list)\n'
            '    }\n'
        )
        
        # Insert before setupButtons
        content = kt_content.replace('private fun setupButtons() {', cart_logic + '    private fun setupButtons() {')
        
        # Add listener in setupButtons
        # Look for btnImprimir listener or similar place.
        # Or Just find "setupButtons() {" and append "findViewById<Button>(R.id.btnEditOrder).setOnClickListener { showCartDialog() }"
        
        content = content.replace(
            'private fun setupButtons() {', 
            'private fun setupButtons() {\n        findViewById<Button>(R.id.btnEditOrder).setOnClickListener { showCartDialog() }'
        )

    with open(main_kt, 'w', encoding='utf-8') as f:
        f.write(content)

except Exception as e:
    print(f"Error logic: {e}")
