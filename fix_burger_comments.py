
import re

main_kt = 'app/src/main/java/com/example/ticketapp/MainActivity.kt'

try:
    with open(main_kt, 'r', encoding='utf-8') as f:
        content = f.read()

    # Fix 1: Sticky Hamburgers (Normal)
    # Target the Producto() block inside the cantidatesNormales loop.
    # We look for a block that has `esCombo = false` AND is inside `cantidadesNormales` loop context (roughly).
    # Since regex is global, we need unique markers.
    # The normal loop has `val precio = preciosHamburguesas[nombre] ?: 0.0` just before.
    
    # Replacement for Normales
    pattern_normal = (
        r'(val precio = preciosHamburguesas\[nombre\] \?: 0\.0\s+'
        r'lista\.add\(\s+'
        r'Producto\(\s+'
        r'nombre = nombre,\s+'
        r'precio = precio,\s+'
        r'cantidad = cantidad,\s+'
        r'esCombo = false\s+\)\s+\))'
    )
    
    repl_normal = (
        '                val precio = preciosHamburguesas[nombre] ?: 0.0\n'
        '                lista.add(\n'
        '                    Producto(\n'
        '                        nombre = nombre,\n'
        '                        precio = precio,\n'
        '                        cantidad = cantidad,\n'
        '                        esCombo = false,\n'
        '                        comment = productComments[nombre]\n'
        '                    )\n'
        '                )'
    )
    
    # Attempt replace
    content, n1 = re.subn(pattern_normal, repl_normal, content)
    print(f"Fixed Normal Burgers: {n1}")

    # Fix 2: Combos
    # Loop over `cantidadesCombo`
    # val precioBase = preciosHamburguesas[nombre] ?: 0.0
    # val precioCombo = precioBase + extraCombo
    # lista.add(
    #    Producto(
    #        // Utilizar "+ Combo" ...
    #        nombre = "$nombre + Combo",
    #        precio = precioCombo,
    #        cantidad = cantidad,
    #        esCombo = true
    #    )
    # )
    
    # We can match `esCombo = true` inside `Producto(...)`
    # The name line might have comments, so be careful.
    # Let's match:
    # nombre = "\$nombre \+ Combo",\s+precio = precioCombo,\s+cantidad = cantidad,\s+esCombo = true
    
    pattern_combo = (
        r'(nombre = "\$nombre \+ Combo",\s+'
        r'precio = precioCombo,\s+'
        r'cantidad = cantidad,\s+'
        r'esCombo = true\s+\))'
    )
    
    repl_combo = (
        'nombre = "$nombre + Combo",\n'
        '                        precio = precioCombo,\n'
        '                        cantidad = cantidad,\n'
        '                        esCombo = true,\n'
        '                        comment = productComments[nombre]\n'
        '                    )'
    )
    
    content, n2 = re.subn(pattern_combo, repl_combo, content)
    print(f"Fixed Combos: {n2}")

    with open(main_kt, 'w', encoding='utf-8') as f:
        f.write(content)

except Exception as e:
    print(f"Error: {e}")
