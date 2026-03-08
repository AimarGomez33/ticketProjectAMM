"""
Removes the orphaned body of setupProductViews (lines 1300-?) and 
setupCollapsibleCategories that now lives outside the function after stubs were inserted.
This script looks for the exact string patterns to remove.
"""

path = 'app/src/main/java/com/example/ticketapp/MainActivity.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# -------------------
# 1) Remove orphaned setupProductViews body
# The stub ends with:  "    }\n"
# Then orphaned body starts with: "        // Mapeo de productos"
# It must end before the next private fun: "    private fun setupButtons"
# -------------------

import re

# Remove orphaned setupProductViews body block:
# matches from "    // Mapeo de productos" up to (but not including) "    private fun"
content = re.sub(
    r'(    private fun setupProductViews\(\) \{\n        // All product views migrated to Jetpack Compose.*?\n    \}\n)        // Mapeo de productos.*?(\n    private fun)',
    r'\1\2',
    content,
    flags=re.DOTALL
)

# -------------------
# 2) Remove orphaned setupCollapsibleCategories body block:
# starts with "        setupCollapsibleView(" and ends before the next private fun
# -------------------
content = re.sub(
    r'(    private fun setupCollapsibleCategories\(\) \{\n        // All collapsible.*?\n    \}\n)        setupCollapsibleView.*?(\n    private fun)',
    r'\1\2',
    content,
    flags=re.DOTALL
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Done cleaning orphaned bodies.")
