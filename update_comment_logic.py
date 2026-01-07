
import re

file_path = 'app/src/main/java/com/example/ticketapp/MainActivity.kt'

try:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Add productComments variable
    if 'private val productComments' not in content:
        content = content.replace(
            'private val cantidadesCombo = mutableMapOf<String, Int>()',
            'private val cantidadesCombo = mutableMapOf<String, Int>()\n        private val productComments = mutableMapOf<String, String>()'
        )

    # 2. Inject listener in setupProductViews loop
    # Pattern: products.forEach { (productName, productData) -> ... quantities[productName] = 0 ...
    # We look for quantities[productName] = 0 and append listener code after it.
    loop_pattern = r'(quantities\[productName\] = 0)'
    loop_repl = (
        r'\1\n\n'
        r'                        productData.btnComment.setOnClickListener {\n'
        r'                                showCommentDialog(productName)\n'
        r'                        }'
    )
    # Only replace the first occurrence inside setupProductViews ideally, but likely unique enough.
    # Actually quantities[productName] = 0 appears once inside the loop.
    content = re.sub(loop_pattern, loop_repl, content, count=1) 

    # 3. Add showCommentDialog function
    # Append it before setupProductViews definition or somewhere safe.
    # We look for "private fun setupProductViews() {"
    if 'fun showCommentDialog' not in content:
        func_code = (
            '\n    private fun showCommentDialog(productName: String) {\n'
            '        val input = EditText(this)\n'
            '        input.hint = "Ej. Sin cebolla, Salsa verde..."\n'
            '        val currentComment = productComments[productName] ?: ""\n'
            '        input.setText(currentComment)\n\n'
            '        AlertDialog.Builder(this)\n'
            '            .setTitle("Comentarios para $productName")\n'
            '            .setView(input)\n'
            '            .setPositiveButton("Guardar") { _, _ ->\n'
            '                val comment = input.text.toString().trim()\n'
            '                if (comment.isNotEmpty()) {\n'
            '                    productComments[productName] = comment\n'
            '                    Toast.makeText(this, "Nota agregada", Toast.LENGTH_SHORT).show()\n'
            '                } else {\n'
            '                    productComments.remove(productName)\n'
            '                }\n'
            '            }\n'
            '            .setNegativeButton("Cancelar", null)\n'
            '            .show()\n'
            '    }\n\n'
        )
        content = content.replace('private fun setupProductViews() {', func_code + '        private fun setupProductViews() {')
    
    # 4. Update limpiarCantidades
    if 'productComments.clear()' not in content:
        content = content.replace(
            'quantities.keys.forEach { quantities[it] = 0 }',
            'quantities.keys.forEach { quantities[it] = 0 }\n                productComments.clear()'
        )

    # 5. Update Producto class definition (add comment)
    # Done via multi_replace? Wait, multi_replace target was:
    # val esCombo: Boolean
    # Replace with: val esCombo: Boolean, val comment: String? = null
    # I should check if it's already done? No, multi_replace failed?
    # Actually I should do it here to be safe.
    if 'val comment: String? = null' not in content:
        content = re.sub(
            r'(val esCombo: Boolean)(\s*\)\s*\{)',
            r'\1,\n                val comment: String? = null\2',
            content
        )

    # 6. Update obtenerProductosDesdeInputs to populate comment
    # Look for: Producto( ... esCombo = false ... )
    # There are 2 instantiations there (normal loop and variation loop).
    # We should replace `esCombo = false` with `esCombo = false, comment = productComments[nombre]` (or nombreVar)
    # Note: Variable names are `nombre` and `nombreVar`.
    # First loop uses `nombre`.
    # Second loop uses `nombreVar`.
    
    # Loop 1: nombre
    content = re.sub(
        r'(nombre = nombre,\s*precio = data\.precio,\s*cantidad = cantidad,\s*esCombo = false)',
        r'\1,\n                                                        comment = productComments[nombre]',
        content
    )
    
    # Loop 2: nombreVar
    content = re.sub(
        r'(nombre = nombreVar,\s*precio = first\.precio,\s*cantidad = items\.size,\s*esCombo = false)',
        r'\1,\n                                        comment = productComments[nombreVar]',
        content
    )
    
    # 7. Update generarTextoTicket to print comments
    # Logic: if (!p.comment.isNullOrEmpty()) sb.appendLine("   (Nota: ${p.comment})")
    # Insert after `sb.appendLine("${p.cantidad} x ${p.nombre}")`
    # There are 2 loops (normales and combos).
    # Use re.sub to inject into both.
    
    ticket_repl = (
        r'sb.appendLine("${p.cantidad} x ${p.nombre}")\n'
        r'                                if (!p.comment.isNullOrEmpty()) {\n'
        r'                                        sb.appendLine("   (Nota: ${p.comment})")\n'
        r'                                }'
    )
    # Need to escape ${} for Python string? No, simple string unless f-string.
    code_repl = (
        r'sb.appendLine("${p.cantidad} x ${p.nombre}")'
        r'\n                                if (!p.comment.isNullOrEmpty()) {'
        r'\n                                        sb.appendLine("   (Nota: ${p.comment})")'
        r'\n                                }'
    )
    # The regex needs escaping for $ and {
    # pattern: `sb\.appendLine\("\$\{p\.cantidad\} x \$\{p\.nombre\}"\)`
    # This might match `c` loop too? `c.cantidad`. No, `p` loop and `c` loop use different vars?
    # In `generarTextoTicket`: `for (p in normales)` and `for (p in combos)`?
    # Let's check view_file at step 171 (Chunk 6 logic).
    # `for (p in normales)` and `for (p in combos)`. Both use `p`.
    # So replacing `p` works for both!
    
    pattern_ticket = r'sb\.appendLine\("\$\{p\.cantidad\} x \$\{p\.nombre\}"\)'
    # Note: In Kotlin string interpolation: "${p.cantidad} x ${p.nombre}"
    
    content, count_ticket = re.subn(pattern_ticket, code_repl, content)
    print(f"Updated ticket generation: {count_ticket}")

    # 8. Update guardarOrden to pass comment
    # Look for `esCombo = it.esCombo` in OrderItemEntity constructor calls.
    # Replace with `esCombo = it.esCombo, comment = it.comment`
    # Occurs twice (add to existing, create new).
    # Pattern: `unitPrice = it.precio,\s*quantity = it.cantidad,\s*esCombo = it.esCombo`
    db_repl = (
        r'unitPrice = it.precio,\n'
        r'                                                        '
        r'quantity = it.cantidad,\n'
        r'                                                        '
        r'esCombo = it.esCombo,\n'
        r'                                                        '
        r'comment = it.comment'
    )
    # Regex needs to match existing layout.
    # pattern: `unitPrice = it\.precio,\s*quantity = it\.cantidad,\s*esCombo = it\.esCombo`
    
    pattern_db = r'unitPrice = it\.precio,\s*quantity = it\.cantidad,\s*esCombo = it\.esCombo'
    content, count_db = re.subn(pattern_db, db_repl, content)
    print(f"Updated DB save logic: {count_db}")

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

except Exception as e:
    print(f"Error: {e}")
