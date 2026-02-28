import re
import os

LAYOUT_PATH = r"c:\Users\jair_\Desktop\ticketProjectAMM\app\src\main\res\layout\activity_main.xml"

with open(LAYOUT_PATH, "r", encoding="utf-8") as f:
    content = f.read()

# Buscamos cada tag XML para eliminar atributos duplicados.
# Un tag empieza con < y termina con >
def deduplicate_attributes(match):
    tag_content = match.group(0)
    
    # Extraemos el nombre del tag
    parts = tag_content.split(maxsplit=1)
    if len(parts) < 2:
        return tag_content
    
    tag_name = parts[0]
    rest = parts[1]
    
    # Buscar todos los atributos
    # pattern: name="value"
    attr_pattern = r'([a-zA-Z_:]+)\s*=\s*"([^"]*)"'
    
    seen_attrs = set()
    
    # Vamos a reconstruir el tag conservando solo la primera vez que vemos cada atributo
    # Para hacerlo correctamente manteniendo el formato (espacios, saltos de línea),
    # iteraremos sobre todos los matches de atributos.
    
    def replacer(m):
        attr_name = m.group(1)
        if attr_name in seen_attrs:
            return "" # Eliminar duplicado (y dejar algo de basura de espacios, que no importa)
        seen_attrs.add(attr_name)
        return m.group(0)
    
    new_rest = re.sub(attr_pattern, replacer, rest)
    
    return tag_name + " " + new_rest

# Aplicar a todos los tags (no a los comments)
# <[^>]+>
new_content = re.sub(r'<[^!][^>]+>', deduplicate_attributes, content)

with open(LAYOUT_PATH, "w", encoding="utf-8") as f:
    f.write(new_content)

print("Atributos duplicados eliminados.")
