path = 'app/src/main/java/com/example/ticketapp/MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

out = []
skip = False
for line in lines:
    if "private fun // setupProductViews replaced by Compose {" in line:
        skip = True
        continue
    if "private fun // setupCollapsibleCategories replaced by Compose {" in line:
        skip = True
        continue
    if skip and line.startswith("    private fun "):
        skip = False
    
    if not skip:
        out.append(line)

with open(path, 'w', encoding='utf-8') as f:
    f.writelines(out)
print("Cleaned up missing blocks.")
