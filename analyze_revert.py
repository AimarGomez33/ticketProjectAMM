
import re

file_path = r'c:\Users\jair_\Desktop\ticketProjectAMM\app\src\main\res\layout\activity_main.xml'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Find lines with the responsive column count and nearby IDs
# We look for <GridLayout ... id="..." ... columnCount="@integer/grid_column_count"
# or simply find the index and look backwards for the id.

matches = [m.start() for m in re.finditer(r'@integer/grid_column_count', content)]

print(f"Found {len(matches)} matches.")

for pos in matches:
    # Look backwards for "id="
    start_search = max(0, pos - 200)
    chunk = content[start_search:pos]
    id_match = re.search(r'android:id="@\+id/([^"]+)"', chunk)
    if id_match:
        print(f"Grid: {id_match.group(1)}")
    else:
        print("Grid: <Unknown ID>")
