#!/usr/bin/env python3
"""
Script para aplicar glassmorfismo al activity_main.xml del proyecto ticketProjectAMM.
Realiza varios reemplazos en batch para transformar cards, headers y sección raíz.
"""

import re

LAYOUT_PATH = r"c:\Users\jair_\Desktop\ticketProjectAMM\app\src\main\res\layout\activity_main.xml"

with open(LAYOUT_PATH, "r", encoding="utf-8") as f:
    content = f.read()

original_length = len(content)
changes = []

# =========================================================
# 1. BACKGROUND DEL ROOT SCROLL / DRAWERLAYOUT
# =========================================================
# Cambiar el background del DrawerLayout de app_background a gradiente
old = 'android:background="@color/app_background"'
new = 'android:background="@color/BrigthSnow"'
if old in content:
    content = content.replace(old, new)
    changes.append(f"DrawerLayout background already uses app_background (no change needed)")

# =========================================================
# 2. CARDS DE PRODUCTOS CON FONDO BrigthSnow → glass_card_bg
# =========================================================
# Patrón: MaterialCardView con cardBackgroundColor BrigthSnow
old = 'app:cardBackgroundColor="@color/BrigthSnow"'
new = 'app:cardBackgroundColor="@color/glass_card_bg"\n                    app:strokeColor="@color/glass_border"\n                    app:strokeWidth="1dp"'
count = content.count(old)
content = content.replace(old, new)
changes.append(f"Cards BrigthSnow → glass_card_bg: {count} reemplazos")

# =========================================================
# 3. CARDS DE PRODUCTOS CON FONDO CarbonBlack → glass_surface_dark
# =========================================================
old_dark = 'app:cardBackgroundColor="@color/CarbonBlack"'
new_dark = 'app:cardBackgroundColor="@color/glass_surface_dark"\n                    app:strokeColor="@color/glass_border_light"\n                    app:strokeWidth="1dp"'
count_dark = content.count(old_dark)
content = content.replace(old_dark, new_dark)
changes.append(f"Cards CarbonBlack → glass_surface_dark: {count_dark} reemplazos")

# =========================================================
# 4. CARD ELEVATION PARA DARLE MÁS PROFUNDIDAD
# =========================================================
# Actualizar elevación de 2dp a 6dp para dar sensación glass premium
old_elev = 'app:cardElevation="2dp"'
new_elev = 'app:cardElevation="6dp"'
count_elev = content.count(old_elev)
content = content.replace(old_elev, new_elev)
changes.append(f"Card elevation 2dp → 6dp: {count_elev} reemplazos")

# =========================================================
# 5. CARD CORNER RADIUS → 16dp (más redondeado, más glass)
# =========================================================
old_corner = 'app:cardCornerRadius="12dp"'
new_corner = 'app:cardCornerRadius="16dp"'
count_corner = content.count(old_corner)
content = content.replace(old_corner, new_corner)
changes.append(f"Card corner radius 12dp → 16dp: {count_corner} reemplazos")

# =========================================================
# 6. SECTION HEADERS: RelativeLayout background
# =========================================================
# Los headers de sección usan android:background="@color/BrigthSnow" o 
# android:background="?android:attr/selectableItemBackground"
# Los transformamos al drawable glass
old_header_bg = 'android:background="@color/BrigthSnow" >'
new_header_bg = 'android:background="@drawable/bg_glass_header"\n               android:paddingStart="12dp"\n               android:paddingEnd="12dp"\n               android:layout_marginTop="8dp"\n               android:layout_marginBottom="4dp"\n               android:elevation="4dp" >'
count_h1 = content.count(old_header_bg)
content = content.replace(old_header_bg, new_header_bg)
changes.append(f"Headers background BrigthSnow → bg_glass_header: {count_h1} reemplazos")

# Headers con selectableItemBackground
old_header_sel = 'android:background="?android:attr/selectableItemBackground">'
new_header_sel = 'android:background="@drawable/bg_glass_header"\n               android:paddingStart="12dp"\n               android:paddingEnd="12dp"\n               android:layout_marginTop="8dp"\n               android:layout_marginBottom="4dp"\n               android:elevation="4dp">'
count_h2 = content.count(old_header_sel)
content = content.replace(old_header_sel, new_header_sel)
changes.append(f"Headers selectableItemBackground → bg_glass_header: {count_h2} reemplazos")

# =========================================================
# 7. CARD DE PEDIDO ANTERIOR (cardExistingOrder)
# =========================================================
# Este ya tiene BrigthSnow que ya fue cambiado arriba, solo verificar corner radius
old_exist = 'app:cardCornerRadius="8dp"'
new_exist = 'app:cardCornerRadius="16dp"'
count_exist = content.count(old_exist)
content = content.replace(old_exist, new_exist)
changes.append(f"Corner radius 8dp → 16dp: {count_exist} reemplazos")

# =========================================================
# 8. INPUT CAMPO MESA: background BrigthSnow → bg_glass_input
# =========================================================
old_input = 'android:background="@color/BrigthSnow"'
new_input = 'android:background="@drawable/bg_glass_input"'
count_input = content.count(old_input)
content = content.replace(old_input, new_input)
changes.append(f"Input background BrigthSnow → bg_glass_input: {count_input} reemplazos")

# =========================================================
# 9. BOTONES MINUS (Grey/CarbonBlack) → glass_surface_dark
# =========================================================
old_btn_grey = 'app:backgroundTint="@color/Grey"'
new_btn_grey = 'app:backgroundTint="@color/glass_surface_dark"'
count_bg = content.count(old_btn_grey)
content = content.replace(old_btn_grey, new_btn_grey)
changes.append(f"Botones Grey → glass_surface_dark: {count_bg} reemplazos")

# =========================================================
# 10. ROOTLAYOUT: agregar padding más amplio y animación
# =========================================================
old_root = 'android:padding="16dp"\n            android:animateLayoutChanges="true">'
new_root = 'android:padding="12dp"\n            android:animateLayoutChanges="true"\n            android:clipToPadding="false">'
count_root = content.count(old_root)
if count_root > 0:
    content = content.replace(old_root, new_root)
changes.append(f"RootLayout padding ajustado: {count_root} reemplazos")

# =========================================================
# 11. FONDO DE CAMPOS LIGHT_GRAYISH → bg_glass_input
# =========================================================
old_lg = 'android:background="@color/Light_grayish"'
new_lg = 'android:background="@drawable/bg_glass_input"'
count_lg = content.count(old_lg)
content = content.replace(old_lg, new_lg)
changes.append(f"Light_grayish backgrounds → bg_glass_input: {count_lg} reemplazos")

# =========================================================
# 12. BOTÓN Grayish_green → glass_surface_dark (para tono glass)
# =========================================================
old_gsn = 'app:backgroundTint="@color/Grayish_green"'
new_gsn = 'app:backgroundTint="@color/glass_surface_dark"'
count_gsn = content.count(old_gsn)
content = content.replace(old_gsn, new_gsn)
changes.append(f"backgroundTint Grayish_green → glass_surface_dark: {count_gsn} reemplazos")

# =========================================================
# ESCRIBIR RESULTADO
# =========================================================
with open(LAYOUT_PATH, "w", encoding="utf-8") as f:
    f.write(content)

print(f"✅ Glassmorfismo aplicado exitosamente a activity_main.xml")
print(f"   Tamaño original: {original_length:,} bytes")
print(f"   Tamaño nuevo:    {len(content):,} bytes")
print(f"\n📋 Cambios realizados:")
for ch in changes:
    print(f"   • {ch}")
