
import re

file_path = r'c:\Users\jair_\Desktop\ticketProjectAMM\app\src\main\res\layout\activity_main.xml'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Define specific replacements based on grid ID context
# We'll use a function to determine the replacement value
def replace_column_count(match):
    # Determine which grid this belongs to by looking backwards
    chunk = content[max(0, match.start() - 200):match.start()]
    if 'gridPlatillos' in chunk:
        return 'android:columnCount="3"'
    else:
        return 'android:columnCount="2"'

# Pattern to find the attribute
pattern = re.compile(r'android:columnCount="@integer/grid_column_count"')

new_content, count = pattern.subn(replace_column_count, content)

print(f"Reverted {count} column counts.")

if count > 0:
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
