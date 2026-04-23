import sys

with open('app/src/main/java/com/example/ticketapp/MainActivity.kt', 'r') as f:
    lines = f.readlines()

count = 0
for i, line in enumerate(lines):
    count += line.count('{')
    count -= line.count('}')
    if count < 0:
        print(f"Brace mismatch goes negative at line {i+1}: {line.strip()}")
        break

print(f"Final brace count: {count}")

class_start = -1
for i, line in enumerate(lines):
    if line.startswith('class MainActivity'):
        class_start = i
        break

if class_start != -1:
    class_count = 0
    for i in range(class_start, len(lines)):
        class_count += lines[i].count('{')
        class_count -= lines[i].count('}')
        if class_count == 0:
            print(f"Class MainActivity closes at line {i+1}")
            break
