
import re

file_path = 'app/src/main/java/com/example/ticketapp/MainActivity.kt'

try:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Update ProductData data class definition
    # Existing: data class ProductData(val cantidadTV: TextView, val btnMenos: Button, val btnMas: Button, val precio: Double)
    # We match loosely to handle whitespace
    pd_class_pattern = r'data class ProductData\s*\(\s*val cantidadTV: TextView,\s*val btnMenos: Button,\s*val btnMas: Button,\s*val precio: Double\s*\)'
    pd_class_repl = 'data class ProductData(val cantidadTV: TextView, val btnMenos: Button, val btnMas: Button, val btnComment: ImageButton, val precio: Double)'
    
    content, count_class = re.subn(pd_class_pattern, pd_class_repl, content)
    print(f"Updated ProductData class definition: {count_class}")

    # 2. Update ProductData instantiations
    # Pattern: ProductData(findViewById(R.id.cantidadX), findViewById(R.id.btnMenosX), findViewById(R.id.btnMasX), Price)
    # Note: The code has newlines.
    inst_pattern = (
        r'ProductData\s*\(\s*'
        r'findViewById\(R\.id\.cantidad(\w+)\),\s*'
        r'findViewById\(R\.id\.btnMenos\1\),\s*'
        r'findViewById\(R\.id\.btnMas\1\),\s*'
        r'([\d\.]+)'
    )
    
    # Replacement: Add findViewById(R.id.btnComment\1) before price
    inst_repl = (
        r'ProductData(\n'
        r'                                findViewById(R.id.cantidad\1),\n'
        r'                                findViewById(R.id.btnMenos\1),\n'
        r'                                findViewById(R.id.btnMas\1),\n'
        r'                                findViewById(R.id.btnComment\1),\n'
        r'                                \2'
    )
    
    # We use dotall? No, \s* matches newlines.
    # But strictly, let's use re.DOTALL is not needed if \s matches newlines (it does in Python re if default? No, \s matches [ \t\n\r\f\v]).
    # Yes \s matches newlines.
    
    content, count_inst = re.subn(inst_pattern, inst_repl, content)
    print(f"Updated ProductData instantiations: {count_inst}")

    # 3. Add Import for ImageButton if missing
    if "import android.widget.ImageButton" not in content:
        content = content.replace("import android.widget.Button", "import android.widget.Button\nimport android.widget.ImageButton")

    if count_class > 0 or count_inst > 0:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print("Successfully saved MainActivity.kt")
    else:
        print("No changes made (patterns might not match).")

except Exception as e:
    print(f"Error: {e}")
