import sys

path = 'app/src/main/java/com/example/ticketapp/MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
skip = False
brace_count = 0

for i, line in enumerate(lines):
    if not skip:
        # Detect start of things to remove
        if 'private val usbReceiver' in line or \
           'private fun setupUsbDevice' in line or \
           'private fun printViaUsb' in line or \
           'private fun detectAndRequestUsbPermission' in line or \
           'private fun releaseUsbDevice' in line:
            skip = True
            brace_count = 0
            if '{' in line:
                brace_count += line.count('{') - line.count('}')
        else:
            new_lines.append(line)
    else:
        # Inside a block to remove
        brace_count += line.count('{') - line.count('}')
        if brace_count <= 0:
            skip = False # Block ended

with open(path, 'w', encoding='utf-8') as f:
    f.writelines(new_lines)

print("USB functions removed.")
