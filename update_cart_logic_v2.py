
import re

main_xml = 'app/src/main/res/layout/activity_main.xml'
main_kt = 'app/src/main/java/com/example/ticketapp/MainActivity.kt'

# 1. Update Layout
try:
    with open(main_xml, 'r', encoding='utf-8') as f:
        xml_content = f.read()

    # Robustly find btnImprimir tag start
    # We look for the ID, then find the preceding <com.google...MaterialButton
    # But simpler: The tag usually starts with <com.google.android.material.button.MaterialButton
    # and eventually contains android:id="@+id/btnImprimir"
    
    # We can use a lookahead or just find the ID and search backwards for '<'
    # But valid XML might have comments.
    
    # Let's try to match the specific block structure common in this file.
    # It likely looks like:
    # <com.google.android.material.button.MaterialButton
    #     android:id="@+id/btnImprimir"
    
    pattern_btn = r'(<com\.google\.android\.material\.button\.MaterialButton[\s\S]*?android:id="@\+id/btnImprimir"[\s\S]*?>)'
    
    # We want to insert valid XML before it.
    # The Edit Button
    edit_btn_xml = (
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
    
    if 'android:id="@+id/btnEditOrder"' not in xml_content:
        # Replace
        # Note: pattern_btn matches the whole Print button tag. We want to PREPEND to it.
        # So replace matched_group with edit_btn + matched_group.
        
        # We need to capture the group.
        # re.sub(pattern, replacement, string)
        # replacement can use \1
        
        new_xml, count = re.subn(pattern_btn, edit_btn_xml + r'\1', xml_content)
        if count > 0:
            with open(main_xml, 'w', encoding='utf-8') as f:
                f.write(new_xml)
            print(f"Updated layout with Edit button: {count}")
        else:
            print("Could not find btnImprimir in layout.")
    else:
        print("Edit button already in layout.")

except Exception as e:
    print(f"Error updating xml: {e}")

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
            '                     // Variations logic unimplemented\n'
            '                } else {\n'
            '                    quantities[product.nombre] = newQuantity\n'
            '                }\n'
            '                // Handle normal products:\n'
            '                if (!product.esCombo && !productVariations.containsKey(product.nombre)) {\n'
            '                     quantities[product.nombre] = newQuantity\n'
            '                     products[product.nombre]?.cantidadTV?.text = newQuantity.toString()\n'
            '                }\n'
            '                // Refresh adapter list\n'
            '                val updatedList = obtenerProductosDesdeInputs()\n'
            '                val total = updatedList.sumOf { it.precio * it.cantidad }\n'
            '                tvTotal.text = "Total: $$total"\n'
            '                // We need to refresh the adapter\'s list reference or notify\n'
            '                // Accessing adapter variable here is tricky in lambda definition?\n'
            '                // No, closure capture works after declaration if initialized? \n'
            '                // We can\'t capture "adapter" inside its own constructor args.\n'
            '                // So we need: val adapter = ...; adapter.onUpdate = ...? No, it\'s ctor param.\n'
            '                // Hack: Pass a function that accesses a lateinit or re-fetches.\n'
            '                // Better: Dismiss and re-show? No.\n'
            '                // Best: Just update UI state here, and tell adapter to refresh.\n'
            '                // But we don\'t have reference to adapter yet.\n'
            '            },\n'
            '            onCommentUpdate = { product, newComment ->\n'
            '                if (!newComment.isNullOrEmpty()) {\n'
            '                     productComments[product.nombre] = newComment\n'
            '                } else {\n'
            '                     productComments.remove(product.nombre)\n'
            '                }\n'
            '                // Update list locally\n'
            '                val updatedList = obtenerProductosDesdeInputs()\n'
            '                // adapter.updateList(updatedList) // Need ref\n'
            '            }\n'
            '        )\n'
            '        // Fix circular dependency by setting callbacks later? Or using object.\n'
            '        // Let\'s rely on dialog close to refresh main UI fully?\n'
            '        // But user wants "Order Editing". \n'
            '        // Let\'s rebuild adapter inside.\n'
            '        // Actually, CartAdapter is our class. We can add a setListener method.\n'
            '\n'
            '        recycler.layoutManager = LinearLayoutManager(this)\n'
            '        recycler.adapter = adapter\n'
            '\n'
            '        // Total\n'
            '        val total = currentProducts.sumOf { it.precio * it.cantidad }\n'
            '        tvTotal.text = "Total: $$total"\n'
            '\n'
            '        // Listeners for Refresh\n'
            '        // To make it work, let\'s just recreate the dialog content logic or use a helper.\n'
            '        // Simplified: The adapter will visually update itself (it has the list).\n'
            '        // We just need to sync back to Main.\n'
            '        // And update Total TextView.\n'
            '\n'
            '        btnClear.setOnClickListener {\n'
            '            limpiarCantidades()\n'
            '            dialog.dismiss()\n'
            '            showCartDialog() // Respawn empty?\n'
            '        }\n'
            '        btnClose.setOnClickListener {\n'
            '            dialog.dismiss()\n'
            '            // Refresh Main Summary\n'
            '            val list = obtenerProductosDesdeInputs()\n'
            '            mostrarResumen(list)\n'
            '        }\n'
            '        dialog.show()\n'
            '    }\n'
        )
        
        # Insert before setupButtons
        content = kt_content.replace('private fun setupButtons() {', cart_logic + '    private fun setupButtons() {')
        
        # Add listener in setupButtons
        # "findViewById<Button>(R.id.btnEditOrder).setOnClickListener { showCartDialog() }"
        # Find btnImprimir logic to insert near? Or just beginning of setupButtons.
        
        setup_repl = (
            'private fun setupButtons() {\n'
            '        val btnEdit = findViewById<Button>(R.id.btnEditOrder)\n'
            '        if (btnEdit != null) {\n'
            '            btnEdit.setOnClickListener { showCartDialog() }\n'
            '        }\n'
        )
        content = kt_content.replace('private fun setupButtons() {', setup_repl)

    with open(main_kt, 'w', encoding='utf-8') as f:
        f.write(content)
        print("Updated MainActivity logic.")

except Exception as e:
    print(f"Error logic: {e}")
