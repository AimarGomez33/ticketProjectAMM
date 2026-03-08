import re

def process_xml(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find the start of the first header (Platillos Principales)
    start_marker = '<RelativeLayout android:id="@+id/headerPlatillos"'
    start_idx = content.find(start_marker)

    # Find the end of the last grid (gridPostres or containerNotasExtras)
    # Actually, we can just replace everything from headerPlatillos to the end of the ScrollView's LinearLayout.
    # The ScrollView's layout ends before the closing </ScrollView> tag.
    
    end_marker = '</LinearLayout>\n    </ScrollView>'
    end_idx = content.find(end_marker)

    if start_idx == -1 or end_idx == -1:
        print("Markers not found!")
        return

    # Replace the massive chunk with a ComposeView
    compose_view_xml = '''
        <!-- Jetpack Compose UI for Menu Selection -->
        <androidx.compose.ui.platform.ComposeView
            android:id="@+id/composeMenuView"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp" />
            
    '''

    new_content = content[:start_idx] + compose_view_xml + content[end_idx:]

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
        
    print("activity_main.xml replaced successfully!")

process_xml('app/src/main/res/layout/activity_main.xml')
