
import re
import os

file_path = 'app/src/main/res/layout/activity_main.xml'

try:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Pattern to find the Minus button and capture the product suffix
    # We look for <com.google.android.material.button.MaterialButton android:id="@+id/btnMenosSuffix"
    pattern = r'(<com\.google\.android\.material\.button\.MaterialButton\s+android:id="@\+id/btnMenos)(\w+)(")'
    
    # Replacement: Insert ImageButton before the capture
    # \2 is the Suffix (e.g. Quesadillas)
    replacement = (
        r'<ImageButton android:id="@+id/btnComment\2" '
        r'android:layout_width="32dp" '
        r'android:layout_height="32dp" '
        r'android:src="@drawable/ic_comment" '
        r'android:background="?attr/selectableItemBackgroundBorderless" '
        r'android:layout_gravity="center_vertical" '
        r'android:padding="4dp" '
        r'android:layout_marginEnd="8dp" '
        r'android:contentDescription="Comentario" />'
        r'\n                            \1\2\3'
    )

    new_content, count = re.subn(pattern, replacement, content)

    if count > 0:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Successfully replaced {count} instances.")
    else:
        print("No matches found.")

except Exception as e:
    print(f"Error: {e}")
