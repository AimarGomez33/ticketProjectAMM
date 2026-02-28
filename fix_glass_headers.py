#!/usr/bin/env python3
"""
Script para limpiar atributos XML duplicados en headers del activity_main.xml
Causados por el script de glassmorfismo que inyectó paddingStart/End/marginTop 
en headers que ya tenían paddingTop/paddingBottom definidos inline.
"""

import re

LAYOUT_PATH = r"c:\Users\jair_\Desktop\ticketProjectAMM\app\src\main\res\layout\activity_main.xml"

with open(LAYOUT_PATH, "r", encoding="utf-8") as f:
    content = f.read()

original_length = len(content)

# El problema: los headers ya tenían android:paddingTop="8dp" android:paddingBottom="8dp" inline
# y el script añadió android:paddingStart/End/marginTop/Bottom/elevation como nuevas líneas
# Pero también resultó en android:layout_marginTop duplicado (uno inline, uno añadido)

# Patron típico del bug:
# android:layout_marginTop="8dp" android:paddingTop="8dp" android:paddingBottom="8dp" android:background="@drawable/bg_glass_header"
# android:paddingStart="12dp"
# android:paddingEnd="12dp"
# android:layout_marginTop="8dp"    <-- DUPLICADO
# android:layout_marginBottom="4dp"
# android:elevation="4dp">

# Quitar el layout_marginTop duplicado en los headers que ya lo tenían inline
# El patrón es: después de android:elevation="4dp"> que viene de nuestro script,
# hay headers que tienen android:layout_marginTop="8dp" duplicate

# Estrategia: encontrar el patrón del header con el atributo marginTop inline + los añadidos
# y eliminar la versión duplicada del marginTop que se inyectó

# Buscar el patrón: 
# android:layout_marginTop="8dp" ... android:background="@drawable/bg_glass_header"\n
# android:paddingStart="12dp"\n
# android:paddingEnd="12dp"\n
# android:layout_marginTop="8dp"\n   <-- ESTE es el duplicado a eliminar

pattern_dup = (
    r'(android:layout_marginTop="8dp" android:paddingTop="8dp" android:paddingBottom="8dp" '
    r'android:background="@drawable/bg_glass_header")\r\n'
    r'               android:paddingStart="12dp"\r\n'
    r'               android:paddingEnd="12dp"\r\n'
    r'               android:layout_marginTop="8dp"\r\n'
    r'               android:layout_marginBottom="4dp"\r\n'
    r'               android:elevation="4dp">'
)

replacement_dup = (
    r'\1\n'
    r'               android:paddingStart="12dp"\n'
    r'               android:paddingEnd="12dp"\n'
    r'               android:layout_marginBottom="4dp"\n'
    r'               android:elevation="4dp">'
)

count1 = len(re.findall(pattern_dup, content))
content = re.sub(pattern_dup, replacement_dup, content)
print(f"Patrón 1 (marginTop duplicado en header con marginTop inline): {count1} correcciones")

# También hay un patrón sin android:layout_marginTop inline pero con el selector background
# android:paddingTop="8dp" android:paddingBottom="8dp" android:background="@drawable/bg_glass_header"
# android:paddingStart="12dp"
# android:paddingEnd="12dp"
# android:layout_marginTop="8dp"
# android:layout_marginBottom="4dp"
# android:elevation="4dp">
# (sin marginTop inline -> este sí es válido, no tiene duplicado)

# Verificar si hay otro patrón: el header con "?android:attr/selectableItemBackground" ya NO existe,
# fue reemplazado correctamente

# Verificar el header de Pambazos específicamente:
# En el original: android:layout_marginTop="8dp" android:paddingTop="8dp" android:paddingBottom="8dp"
# La sustitución añadió: marginTop, paddingStart, paddingEnd, marginTop(dup), marginBottom, elevation
# NECESITAMOS: remover el android:paddingTop y android:paddingBottom que quedaron inline ya que 
# ahora tenemos paddingStart pero no paddingTop duplicado... 
# En realidad el problema era solo android:layout_marginTop duplicado

# Verificar también el patrón donde el background BrigthSnow tenía solo paddingTop/Bottom sin marginTop
# android:paddingTop="8dp" android:paddingBottom="8dp" android:background="@drawable/bg_glass_header"
# android:paddingStart="12dp"
# android:paddingEnd="12dp"
# android:layout_marginTop="8dp"
# android:layout_marginBottom="4dp"
# android:elevation="4dp">
pattern2 = (
    r'android:paddingTop="8dp" android:paddingBottom="8dp" android:background="@drawable/bg_glass_header"\r\n'
    r'               android:paddingStart="12dp"\r\n'
    r'               android:paddingEnd="12dp"\r\n'
    r'               android:layout_marginTop="8dp"\r\n'
    r'               android:layout_marginBottom="4dp"\r\n'
    r'               android:elevation="4dp">'
)

replacement2 = (
    'android:background="@drawable/bg_glass_header"\n'
    '               android:paddingTop="10dp"\n'
    '               android:paddingBottom="10dp"\n'
    '               android:paddingStart="12dp"\n'
    '               android:paddingEnd="12dp"\n'
    '               android:layout_marginTop="8dp"\n'
    '               android:layout_marginBottom="4dp"\n'
    '               android:elevation="4dp">'
)

count2 = len(re.findall(pattern2, content))
content = re.sub(pattern2, replacement2, content)
print(f"Patrón 2 (headers con paddingTop/Bottom inline sin marginTop): {count2} correcciones")

with open(LAYOUT_PATH, "w", encoding="utf-8") as f:
    f.write(content)

print(f"\n✅ Correcciones aplicadas.")
print(f"   Tamaño original: {original_length:,} bytes")
print(f"   Tamaño nuevo:    {len(content):,} bytes")
