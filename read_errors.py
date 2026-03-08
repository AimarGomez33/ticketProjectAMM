with open('build_out.txt', 'r', encoding='utf-16le') as f:
    for line in f:
        line = line.strip()
        if 'error:' in line.lower() or 'mainactivity.kt:' in line.lower():
            print(line)
