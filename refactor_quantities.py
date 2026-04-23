import re

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if 'private val quantities =' in line:
        # Delete declaration
        continue
    
    # quantities.keys.toList().forEach { key -> quantities[key] = 0 } -> viewModel.clearAllQuantities()
    if 'quantities.keys.toList().forEach { key -> quantities[key] = 0 }' in line:
        new_lines.append(line.replace('quantities.keys.toList().forEach { key -> quantities[key] = 0 }', 'viewModel.clearAllQuantities()'))
        continue

    # val cantidadActual = quantities.getOrPut(nombre) { 0 }
    if 'quantities.getOrPut' in line:
        line = re.sub(r'quantities\.getOrPut\(([^)]+)\)\s*\{\s*0\s*\}', r'viewModel.getQuantity(\1)', line)
        
    if 'quantities.putIfAbsent' in line:
        line = re.sub(r'quantities\.putIfAbsent\(([^,]+),\s*0\)', r'if (viewModel.getQuantity(\1) == 0) viewModel.updateQuantity(\1, 0)', line)
        
    if 'quantities.remove' in line:
        line = re.sub(r'quantities\.remove\(([^)]+)\)', r'viewModel.getQuantity(\1).also { viewModel.updateQuantity(\1, 0) }', line)

    # Assignment quantities[key] = value
    line = re.sub(r'quantities\[([^\]]+)\]\s*=\s*(.+)', r'viewModel.updateQuantity(\1, \2)', line)

    # Access quantities[key]
    line = re.sub(r'quantities\[([^\]]+)\]', r'viewModel.getQuantity(\1)', line)

    # for ((nombre, cantidad) in quantities)
    line = re.sub(r'for\s*\(\(nombre,\s*cantidad\)\s*in\s*quantities\)', r'for ((nombre, cantidad) in viewModel.quantities.value)', line)

    # quantities.keys
    line = re.sub(r'quantities\.keys', r'viewModel.quantities.value.keys', line)
    
    # quantities.clear()
    line = re.sub(r'quantities\.clear\(\)', r'viewModel.clearAllQuantities()', line)
    
    new_lines.append(line)

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'w') as f:
    f.writelines(new_lines)
