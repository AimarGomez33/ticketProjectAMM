import re

def clean_main_activity():
    path = 'app/src/main/java/com/example/ticketapp/MainActivity.kt'
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Remove productVariations map
    content = re.sub(r'private val productVariations =.*?(?=private val products =)', '', content, flags=re.DOTALL)
    
    # 2. Remove products map
    content = re.sub(r'private val products =.*?(?=override fun onCreate)', '', content, flags=re.DOTALL)
    
    # 3. Remove setupProductViews() call from onCreate
    content = content.replace("setupProductViews()", "// setupProductViews replaced by Compose")
    
    # 4. Remove setupCollapsibleCategories() call from onCreate
    content = content.replace("setupCollapsibleCategories()", "// setupCollapsibleCategories replaced by Compose")

    # 5. Remove the massive setupProductViews() function body
    content = re.sub(r'private fun setupProductViews\(\) \{.*?(?=// plegables por categoría)', '', content, flags=re.DOTALL)
    
    # 6. Remove the setupCollapsibleCategories() and setupCollapsibleView() functions
    content = re.sub(r'private fun setupCollapsibleCategories\(\) \{.*?(?=// -------------------------------------------------------------------------)', '', content, flags=re.DOTALL)

    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
        
    print("MainActivity cleaned successfully")

if __name__ == '__main__':
    clean_main_activity()
