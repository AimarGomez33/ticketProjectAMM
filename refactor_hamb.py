import re

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if 'private val cantidadesNormales =' in line or 'private val cantidadesCombo =' in line:
        continue
    
    # Assignment
    line = re.sub(r'cantidadesNormales\[([^\]]+)\]\s*=\s*(.+)', r'viewModel.updateHamburguesaNormal(\1, \2)', line)
    line = re.sub(r'cantidadesCombo\[([^\]]+)\]\s*=\s*(.+)', r'viewModel.updateHamburguesaCombo(\1, \2)', line)
    
    # Access
    line = re.sub(r'cantidadesNormales\[([^\]]+)\]', r'viewModel.getHamburguesaNormal(\1)', line)
    line = re.sub(r'cantidadesCombo\[([^\]]+)\]', r'viewModel.getHamburguesaCombo(\1)', line)
    
    line = re.sub(r'cantidadesNormales\.clear\(\)', r'', line) # cleared in viewModel.clearAllQuantities()
    line = re.sub(r'cantidadesCombo\.clear\(\)', r'', line)
    
    new_lines.append(line)

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'w') as f:
    f.writelines(new_lines)
