
import re

file_path = 'app/src/main/java/com/example/ticketapp/MainActivity.kt'

try:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Fix generation for combos (c loop)
    # Pattern: sb.appendLine("${c.cantidad} x ${c.nombre}")
    # Replace with appendLine + check comment
    
    # We escape regex chars
    pattern = r'sb\.appendLine\("\$\{c\.cantidad\} x \$\{c\.nombre\}"\)'
    
    repl = (
        r'sb.appendLine("${c.cantidad} x ${c.nombre}")\n'
        r'                                        if (!c.comment.isNullOrEmpty()) {\n'
        r'                                                sb.appendLine("   (Nota: ${c.comment})")\n'
        r'                                        }'
    )
    
    content, count = re.subn(pattern, repl, content)
    print(f"Updated combo ticket generation: {count}")
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

except Exception as e:
    print(f"Error: {e}")
