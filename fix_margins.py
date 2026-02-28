import os

LAYOUT_PATH = r"c:\Users\jair_\Desktop\ticketProjectAMM\app\src\main\res\layout\activity_main.xml"

with open(LAYOUT_PATH, "r", encoding="utf-8") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if "<RelativeLayout android:id=\"@+id/header" in line and "android:layout_marginTop=\"8dp\"" in line:
        # Remover el atributo inline
        new_line = line.replace(' android:layout_marginTop="8dp"', '')
        new_lines.append(new_line)
    else:
        new_lines.append(line)

with open(LAYOUT_PATH, "w", encoding="utf-8") as f:
    f.writelines(new_lines)

print("Duplicates removed.")
